package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;

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

        // at least %a%
        // and not this one: %kernel.root_dir%/../web/
        String parameterName = PsiElementUtils.getText(psiElement);
        if(parameterName.length() < 3 || !(parameterName.startsWith("%") && parameterName.endsWith("%"))) {
            return;
        }

        parameterName = parameterName.substring(1, parameterName.length() - 1);

        String parameterValue = ServiceXmlParserFactory.getInstance(psiElement.getProject(), ParameterServiceParser.class).getParameterMap().get(parameterName);
        if (null == parameterValue && !YamlHelper.getLocalParameterMap(psiElement).containsKey(parameterName)) {
            holder.createWarningAnnotation(psiElement, "Missing Parameter");
        }

    }

    private void annotateService(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!YamlElementPatternHelper.getServiceDefinition().accepts(psiElement) || !YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement)) {
            return;
        }

        String serviceName = PsiElementUtils.getText(psiElement).substring(1);

        // yaml strict=false syntax
        if(serviceName.endsWith("=")) {
            serviceName = serviceName.substring(0, serviceName.length() -1);
        }

        String serviceClass = ServiceXmlParserFactory.getInstance(psiElement.getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceName);
        if (null == serviceClass && !YamlHelper.getLocalServiceMap(psiElement).containsKey(serviceName)) {
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