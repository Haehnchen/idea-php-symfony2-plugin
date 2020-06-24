package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.*;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlLineMarkerProvider implements LineMarkerProvider {

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos) {
        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        for(PsiElement psiElement : psiElements) {
            attachRouteActions(lineMarkerInfos, psiElement);
            attachEntityClass(lineMarkerInfos, psiElement);
            attachRelationClass(lineMarkerInfos, psiElement);

            if(psiElement instanceof YAMLFile) {
                RelatedItemLineMarkerInfo<PsiElement> lineMarker = FileResourceUtil.getFileImplementsLineMarker((PsiFile) psiElement);
                if(lineMarker != null) {
                    lineMarkerInfos.add(lineMarker);
                }
            }
        }
    }

    private void attachEntityClass(@NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement psiElement) {
        if(psiElement.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
            return;
        }

        PsiElement yamlKeyValue = psiElement.getParent();
        if(!(yamlKeyValue instanceof YAMLKeyValue)) {
            return;
        }

        if(yamlKeyValue.getParent() instanceof YAMLMapping && yamlKeyValue.getParent().getParent() instanceof YAMLDocument) {
            PsiFile containingFile;
            try {
                containingFile = yamlKeyValue.getContainingFile();
            } catch (PsiInvalidElementAccessException e) {
                return;
            }

            String fileName = containingFile.getName();
            if(isMetadataFile(fileName)) {
                String keyText = ((YAMLKeyValue) yamlKeyValue).getKeyText();
                if(StringUtils.isNotBlank(keyText)) {
                    Collection<PhpClass> phpClasses = PhpElementsUtil.getClassesInterface(psiElement.getProject(), keyText);
                    if(phpClasses.size() > 0) {
                        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.DOCTRINE_LINE_MARKER).
                            setTargets(phpClasses).
                            setTooltipText("Navigate to class");

                        lineMarkerInfos.add(builder.createLineMarkerInfo(psiElement));
                    }
                }
            }
        }
    }

    /**
     * Find controller definition in yaml structor
     *
     * foo:
     *   defaults: { _controller: "Bundle:Foo:Bar" }
     *   controller: "Bundle:Foo:Bar"
     */
    private void attachRouteActions(@NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement psiElement) {
        if(psiElement.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
            return;
        }

        PsiElement yamlKeyValue = psiElement.getParent();
        if(!(yamlKeyValue instanceof YAMLKeyValue)) {
            return;
        }

        String yamlController = RouteHelper.getYamlController((YAMLKeyValue) yamlKeyValue);
        if(yamlController != null) {
            PsiElement[] methods = RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), yamlController);
            if(methods.length > 0) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
                    setTargets(methods).
                    setTooltipText("Navigate to action");

                lineMarkerInfos.add(builder.createLineMarkerInfo(psiElement));
            }
        }
    }

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    /**
     * Set linemarker for targetEntity in possible yaml entity files
     *
     * foo:
     *   targetEntity: Class
     */
    private void attachRelationClass(@NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement psiElement) {
        if(psiElement.getNode().getElementType() != YAMLTokenTypes.SCALAR_KEY) {
            return;
        }

        PsiElement yamlKeyValue = psiElement.getParent();
        if(!(yamlKeyValue instanceof YAMLKeyValue)) {
            return;
        }

        String keyText = ((YAMLKeyValue) yamlKeyValue).getKeyText();
        if(!(keyText.equalsIgnoreCase("targetEntity") || keyText.equalsIgnoreCase("targetDocument"))) {
            return;
        }

        String valueText = ((YAMLKeyValue) yamlKeyValue).getValueText();
        if(StringUtils.isBlank(valueText)) {
            return;
        }

        Collection<PhpClass> classesInterface = DoctrineMetadataUtil.getClassInsideScope(yamlKeyValue, valueText);
        if(classesInterface.size() == 0) {
            return;
        }

        // get relation key
        PsiElement parent = yamlKeyValue.getParent();
        if(parent != null) {
            PsiElement yamlKeyValueTarget = parent.getParent();
            if(yamlKeyValueTarget instanceof YAMLKeyValue) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.DOCTRINE_LINE_MARKER).
                    setTargets(classesInterface).
                    setTooltipText("Navigate to file");

                PsiElement key = ((YAMLKeyValue) yamlKeyValueTarget).getKey();
                if(key != null) {
                    lineMarkerInfos.add(builder.createLineMarkerInfo(key));
                }
            }
        }
    }

    private boolean isMetadataFile(String fileName) {
        fileName = fileName.toLowerCase();
        return fileName.endsWith("orm.yml") || fileName.endsWith("odm.yml") || fileName.endsWith("mongodb.yml") || fileName.endsWith("couchdb.yml") || fileName.endsWith("document.yml")
            || fileName.endsWith("orm.yaml") || fileName.endsWith("odm.yaml") || fileName.endsWith("mongodb.yaml") || fileName.endsWith("couchdb.yaml") || fileName.endsWith("document.yaml");
    }
}
