package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ClassServiceDefinitionTargetLazyValue;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> results) {

        // we need project element; so get it from first item
        if(psiElements.size() == 0) {
            return;
        }

        Project project = psiElements.get(0).getProject();
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for(PsiElement psiElement: psiElements) {

            if(PhpElementsUtil.getMethodReturnPattern().accepts(psiElement)) {
                this.formNameMarker(psiElement, results);
            }

            if(PhpElementsUtil.getClassNamePattern().accepts(psiElement)) {
                this.classNameMarker(psiElement, results);
                this.entityClassMarker(psiElement, results);
                this.repositoryClassMarker(psiElement, results);
                this.validatorClassMarker(psiElement, results);
                this.constraintValidatorClassMarker(psiElement, results);
            }

            if(psiElement instanceof PhpFile) {
                routeAnnotationFileResource((PhpFile) psiElement, results);
            }
        }

    }

    private void classNameMarker(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        PsiElement phpClassContext = psiElement.getContext();
        if(!(phpClassContext instanceof PhpClass)) {
            return;
        }

        ClassServiceDefinitionTargetLazyValue psiElements = ServiceIndexUtil.findServiceDefinitionsLazy((PhpClass) phpClassContext);
        if(psiElements == null) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SERVICE_LINE_MARKER).
            setTargets(psiElements).
            setTooltipText("Navigate to definition");

        result.add(builder.createLineMarkerInfo(psiElement));

    }

    private void entityClassMarker(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        PsiElement phpClassContext = psiElement.getContext();
        if(!(phpClassContext instanceof PhpClass)) {
            return;
        }

        Collection<PsiFile> psiFiles = new ArrayList<>();
        // @TODO: use DoctrineMetadataUtil, for single resolve; we have collecting overhead here
        for(DoctrineModel doctrineModel: EntityHelper.getModelClasses(psiElement.getProject())) {
            PhpClass phpClass = doctrineModel.getPhpClass();
            if(!PhpElementsUtil.isEqualClassName(phpClass, (PhpClass) phpClassContext)) {
                continue;
            }

            PsiFile psiFile = EntityHelper.getModelConfigFile(phpClass);

            // prevent self navigation for line marker
            if(psiFile == null || psiFile instanceof PhpFile) {
                continue;
            }

            psiFiles.add(psiFile);
        }

        if(psiFiles.size() == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.DOCTRINE_LINE_MARKER).
            setTargets(psiFiles).
            setTooltipText("Navigate to model");

        result.add(builder.createLineMarkerInfo(psiElement));
    }

    private void repositoryClassMarker(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        PsiElement phpClassContext = psiElement.getContext();
        if(!(phpClassContext instanceof PhpClass)) {
            return;
        }

        Collection<PsiFile> psiFiles = new ArrayList<>();
        for (VirtualFile virtualFile : DoctrineMetadataUtil.findMetadataForRepositoryClass((PhpClass) phpClassContext)) {
            PsiFile file = PsiManager.getInstance(psiElement.getProject()).findFile(virtualFile);
            if(file == null) {
                continue;
            }

            psiFiles.add(file);
        }

        if(psiFiles.size() == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.DOCTRINE_LINE_MARKER).
            setTargets(psiFiles).
            setTooltipText("Navigate to metadata");

        result.add(builder.createLineMarkerInfo(psiElement));
    }

    private void formNameMarker(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        if(!(psiElement instanceof StringLiteralExpression)) {
            return;
        }

        Method method = PsiTreeUtil.getParentOfType(psiElement, Method.class);
        if(method == null) {
            return;
        }

        if(new Symfony2InterfacesUtil().isCallTo(method, "\\Symfony\\Component\\Form\\FormTypeInterface", "getParent")) {

            // get form string; on blank string we dont need any further action
            String contents = ((StringLiteralExpression) psiElement).getContents();
            if(StringUtils.isBlank(contents)) {
                return;
            }

            PsiElement formPsiTarget = FormUtil.getFormTypeToClass(psiElement.getProject(), contents);
            if(formPsiTarget != null) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.FORM_TYPE_LINE_MARKER).
                    setTargets(formPsiTarget).
                    setTooltipText("Navigate to form type");

                result.add(builder.createLineMarkerInfo(psiElement));
            }

        }

    }

    protected void routeAnnotationFileResource(@NotNull PsiFile psiFile, Collection<? super RelatedItemLineMarkerInfo> results) {
        RelatedItemLineMarkerInfo<PsiElement> lineMarker = FileResourceUtil.getFileImplementsLineMarkerInFolderScope(psiFile);
        if(lineMarker != null) {
            results.add(lineMarker);
        }
    }

    /**
     * Constraints in same namespace and validateBy service name
     */
    private void validatorClassMarker(PsiElement psiElement, Collection<LineMarkerInfo> results) {
        PsiElement phpClassContext = psiElement.getContext();
        if(!(phpClassContext instanceof PhpClass) || !PhpElementsUtil.isInstanceOf((PhpClass) phpClassContext, "\\Symfony\\Component\\Validator\\Constraint")) {
            return;
        }

        Collection<PhpClass> phpClasses = new ArrayList<>();

        // class in same namespace
        String className = ((PhpClass) phpClassContext).getFQN() + "Validator";
        phpClasses.addAll(
            PhpElementsUtil.getClassesInterface(psiElement.getProject(), className)
        );

        // @TODO: validateBy alias

        if(phpClasses.size() == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER).
            setTargets(phpClasses).
            setTooltipText("Navigate to validator");

        results.add(builder.createLineMarkerInfo(psiElement));
    }

    /**
     * "FooValidator" back to "Foo" constraint
     */
    private void constraintValidatorClassMarker(PsiElement psiElement, Collection<LineMarkerInfo> results) {
        PsiElement phpClass = psiElement.getContext();
        if(!(phpClass instanceof PhpClass) || !PhpElementsUtil.isInstanceOf((PhpClass) phpClass, "Symfony\\Component\\Validator\\ConstraintValidatorInterface")) {
            return;
        }

        String fqn = ((PhpClass) phpClass).getFQN();
        if(fqn == null || !fqn.endsWith("Validator")) {
            return;
        }

        Collection<PhpClass> phpClasses = new ArrayList<>(
            PhpElementsUtil.getClassesInterface(psiElement.getProject(), fqn.substring(0, fqn.length() - "Validator".length()))
        );

        if(phpClasses.size() == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER).
            setTargets(phpClasses).
            setTooltipText("Navigate to constraint");

        results.add(builder.createLineMarkerInfo(psiElement));
    }
}

