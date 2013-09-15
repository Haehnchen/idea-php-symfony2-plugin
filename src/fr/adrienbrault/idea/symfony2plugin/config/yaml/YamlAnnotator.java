package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLArray;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;

import java.util.ArrayList;

public class YamlAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement.getProject()) || !Settings.getInstance(psiElement.getProject()).yamlAnnotateServiceConfig) {
            return;
        }

        this.annotateClass(psiElement, holder);
        this.annotateParameter(psiElement, holder);
        this.annotateService(psiElement, holder);
        this.annotateConstructorArguments(psiElement, holder);
        this.annotateCallsArguments(psiElement, holder);
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


    private void annotateConstructorArguments(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {


        if(!PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement)
            && !PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement))
        {
            return;
        }

        // @TODO: simplify code checks

        if(!(psiElement.getContext() instanceof YAMLArray)) {
            return;
        }

        YAMLArray yamlArray = (YAMLArray) psiElement.getContext();
        if(!(yamlArray.getContext() instanceof YAMLCompoundValue)) {
            return;
        }

        YAMLCompoundValue yamlCompoundValue = (YAMLCompoundValue) yamlArray.getContext();
        if(!(yamlCompoundValue.getContext() instanceof YAMLKeyValue)) {
            return;
        }

        YAMLKeyValue yamlKeyValue = (YAMLKeyValue) yamlCompoundValue.getContext();
        if(yamlKeyValue == null || !yamlKeyValue.getKeyText().equals("arguments")) {
            return;
        }

        YAMLKeyValue classKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue.getContext(), "class");
        if(classKeyValue == null) {
            return;
        }

        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classKeyValue.getValueText());
        if(serviceClass == null) {
            return;
        }

        Method constructor = serviceClass.getConstructor();
        if(constructor == null) {
            return;
        }

        attachInstanceAnnotation(psiElement, holder, yamlArray, constructor);

    }

    private void annotateCallsArguments(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {


        if(!PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement)
            && !PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement))
        {
            return;
        }

        // @TODO: simplify code checks
        if(!(psiElement.getContext() instanceof YAMLArray)) {
            return;
        }

        YAMLArray yamlCallParameterArray = (YAMLArray) psiElement.getContext();
        if(!(yamlCallParameterArray.getContext() instanceof YAMLArray)) {
            return;
        }

        YAMLArray yamlCallArray = (YAMLArray) yamlCallParameterArray.getContext();
        if(!(yamlCallArray.getContext() instanceof YAMLSequence)) {
            return;
        }

        ArrayList<PsiElement> methodParameter = YamlHelper.getYamlArrayValues(yamlCallArray);
        if(methodParameter.size() < 2) {
            return;
        }

        String methodName = PsiElementUtils.getText(methodParameter.get(0));

        YAMLSequence yamlSequence = (YAMLSequence) yamlCallArray.getContext();
        if(!(yamlSequence.getContext() instanceof YAMLCompoundValue)) {
            return;
        }

        YAMLCompoundValue yamlCompoundValue = (YAMLCompoundValue) yamlSequence.getContext();
        if(!(yamlCompoundValue.getContext() instanceof YAMLKeyValue)) {
            return;
        }

        YAMLCompoundValue serviceDefinition = PsiTreeUtil.getParentOfType(yamlCompoundValue, YAMLCompoundValue.class);
        YAMLKeyValue classKeyValue = YamlHelper.getYamlKeyValue(serviceDefinition, "class");
        if(classKeyValue == null) {
            return;
        }

        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classKeyValue.getValueText());
        if(serviceClass == null) {
            return;
        }

        Method method = PhpElementsUtil.getClassMethod(serviceClass, methodName);
        if(method == null) {
            return;
        }

        attachInstanceAnnotation(psiElement, holder, yamlCallParameterArray, method);

    }

    private void attachInstanceAnnotation(PsiElement psiElement, AnnotationHolder holder, YAMLArray yamlArray, Method constructor) {
        int parameterIndex = YamlHelper.getYamlParameter(yamlArray, psiElement);
        if(parameterIndex == -1) {
            return;
        }

        PhpClass serviceParameterClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), getServiceName(psiElement));
        if(serviceParameterClass == null) {
            return;
        }

        Parameter[] constructorParameter = constructor.getParameters();
        if(parameterIndex >= constructorParameter.length) {
            return;
        }

        PhpClass expectedClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), constructorParameter[parameterIndex].getDeclaredType().toString());
        if(expectedClass == null) {
            return;
        }

        if(!new Symfony2InterfacesUtil().isInstanceOf(serviceParameterClass, expectedClass)) {
            holder.createWeakWarningAnnotation(psiElement, "Expect instance of: " + expectedClass.getPresentableFQN());
        }
    }

    private String getServiceName(PsiElement psiElement) {
        String serviceName = PsiElementUtils.getText(psiElement);
        if(serviceName.startsWith("@")) {
            serviceName = serviceName.substring(1);
        }

        // yaml strict=false syntax
        if(serviceName.endsWith("=")) {
            serviceName = serviceName.substring(0, serviceName.length() -1);
        }

        return serviceName;
    }
}