package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class YamlAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement.getProject()) || !Settings.getInstance(psiElement.getProject()).yamlAnnotateServiceConfig) {
            return;
        }

        this.annotateClass(psiElement, holder);
        this.annotateParameter(psiElement, holder);
        this.annotateService(psiElement, holder);

    }

    private void annotateParameter(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!YamlElementPatternHelper.getServiceParameterDefinition().accepts(psiElement) || !YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement)) {
            return;
        }

        Symfony2ProjectComponent symfony2ProjectComponent = psiElement.getProject().getComponent(Symfony2ProjectComponent.class);
        if (null == symfony2ProjectComponent) {
            return;
        }

        // at least %a%
        String parameterName = PsiElementUtils.getText(psiElement);
        if(parameterName.length() < 3) {
            return;
        }

        String parameterValue = symfony2ProjectComponent.getConfigParameter().get(parameterName.substring(1, parameterName.length() - 1));
        if (null == parameterValue) {
            holder.createWarningAnnotation(psiElement, "Missing Parameter");
        }

    }

    private void annotateService(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!YamlElementPatternHelper.getServiceDefinition().accepts(psiElement) || !YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement)) {
            return;
        }

        Symfony2ProjectComponent symfony2ProjectComponent = psiElement.getProject().getComponent(Symfony2ProjectComponent.class);
        String serviceClass = symfony2ProjectComponent.getServicesMap().getMap().get(PsiElementUtils.getText(psiElement).substring(1));

        if (null == serviceClass) {
            holder.createWarningAnnotation(psiElement, "Missing Service");
        }

    }

    private void annotateClass(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!((YamlElementPatternHelper.getSingleLineScalarKey("class", "factory_class").accepts(element) || YamlElementPatternHelper.getParameterClassPattern().accepts(element)) && YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(element))) {
            return;
        }

        if(element.getText().contains("\\")) {
            String className = PsiElementUtils.getText(element);
            if(className.startsWith("\\")) {
                className = "\\" + className;
            }

            if(PhpElementsUtil.getClassInterfacePsiElements(element.getProject(), className).length == 0) {
                holder.createWarningAnnotation(element, "Missing Class");
            }
        }

    }
}