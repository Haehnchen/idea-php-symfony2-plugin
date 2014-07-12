package fr.adrienbrault.idea.symfony2plugin.config;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

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

        boolean phpHighlightServices = Settings.getInstance(project).phpHighlightServices;

        for(PsiElement psiElement: psiElements) {

            if(PhpElementsUtil.getMethodReturnPattern().accepts(psiElement)) {
                this.formNameMarker(psiElement, results);
            }

            if(PhpElementsUtil.getClassNamePattern().accepts(psiElement)) {
                this.classNameMarker(psiElement, results);
            }

            if(phpHighlightServices) {
                collectNavigationMarkers(psiElement, results);
            }

        }

    }

    protected void collectNavigationMarkers(@NotNull PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        if (!(psiElement instanceof StringLiteralExpression) || !(psiElement.getContext() instanceof ParameterList)) {
            return;
        }

        ParameterList parameterList = (ParameterList) psiElement.getContext();
        if (parameterList == null || !(parameterList.getContext() instanceof MethodReference)) {
            return;
        }

        MethodReference method = (MethodReference) parameterList.getContext();
        if(!new Symfony2InterfacesUtil().isContainerGetCall(method)) {
            return;
        }

        String serviceId = ((StringLiteralExpression) psiElement).getContents();

        String serviceClass = ServiceXmlParserFactory.getInstance(psiElement.getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceId.toLowerCase());
        if (null == serviceClass) {
            return;
        }

        PsiElement[] resolveResults = PhpElementsUtil.getClassInterfacePsiElements(psiElement.getProject(), serviceClass);
        if(resolveResults.length == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SERVICE_LINE_MARKER).
            setTargets(resolveResults).
            setTooltipText("Navigate to service");

        result.add(builder.createLineMarkerInfo(psiElement));

    }

    private void classNameMarker(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        PsiElement phpClassContext = psiElement.getContext();
        if(!(phpClassContext instanceof PhpClass)) {
            return;
        }

        PsiElement[] psiElements = ServiceIndexUtil.findServiceDefinitions((PhpClass) phpClassContext);
        if(psiElements.length == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SERVICE_LINE_MARKER).
            setTargets(psiElements).
            setTooltipText("Navigate to definition");

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

}

