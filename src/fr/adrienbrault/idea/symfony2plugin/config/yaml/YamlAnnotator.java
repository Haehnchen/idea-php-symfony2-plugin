package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
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

        this.annotateParameter(psiElement, holder);
        this.annotateClass(psiElement, holder);
        this.annotateService(psiElement, holder);

        // only match inside service definitions
        if(!YamlElementPatternHelper.getInsideKeyValue("services").accepts(psiElement)) {
            return;
        }

        this.annotateConstructorArguments(psiElement, holder);
        this.annotateCallsArguments(psiElement, holder);
        this.annotateCallMethod(psiElement, holder);
    }

    private void annotateParameter(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!YamlElementPatternHelper.getServiceParameterDefinition().accepts(psiElement) || !YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement)) {
            return;
        }

        // at least %a%
        // and not this one: %kernel.root_dir%/../web/
        // %kernel.root_dir%/../web/%webpath_modelmasks%
        String parameterName = PsiElementUtils.getText(psiElement);
        if(!YamlHelper.isValidParameterName(parameterName)) {
            return;
        }

        // strip "%"
        parameterName = parameterName.substring(1, parameterName.length() - 1);

        // parameter a always lowercase see #179
        parameterName = parameterName.toLowerCase();
        if (!ContainerCollectionResolver.getParameterNames(psiElement.getProject()).contains(parameterName)) {
            holder.createWarningAnnotation(psiElement, "Missing Parameter");
        }

    }

    private void annotateService(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!YamlElementPatternHelper.getServiceDefinition().accepts(psiElement) || !YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement)) {
            return;
        }

        String serviceName = getServiceName(psiElement);

        // dont mark only "@" and "@?"
        if(serviceName.length() < 2) {
            return;
        }

        // search any current open file
        if(YamlHelper.getLocalServiceMap(psiElement).containsKey(serviceName)) {
            return;
        }

        // search on container should be slowest
        String serviceClass = ServiceXmlParserFactory.getInstance(psiElement.getProject(), XmlServiceParser.class).getServiceMap().getMap().get(serviceName);
        if(serviceClass != null) {
            return;
        }

        // indexer should be the fastest, so think moving first
        VirtualFile[] virtualFiles = ServiceIndexUtil.getFindServiceDefinitionFiles(psiElement.getProject(), serviceName);
        if(virtualFiles.length > 0) {
            return;
        }

        holder.createWarningAnnotation(psiElement, "Missing Service");
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

    private void annotateCallMethod(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {

        if((!PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement)
            && !PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement)))
        {
            return;
        }

        if(!YamlElementPatternHelper.getInsideKeyValue("calls").accepts(psiElement)){
            return;
        }

        if(psiElement.getParent() == null || !(psiElement.getParent().getContext() instanceof YAMLSequence)) {
            return;
        }

        YAMLKeyValue callYamlKeyValue = PsiTreeUtil.getParentOfType(psiElement, YAMLKeyValue.class);
        if(callYamlKeyValue == null) {
            return;
        }

        YAMLKeyValue classKeyValue = YamlHelper.getYamlKeyValue(callYamlKeyValue.getContext(), "class");
        if(classKeyValue == null) {
            return;
        }

        PhpClass serviceParameterClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), getServiceName(classKeyValue.getValue()));
        if(serviceParameterClass == null) {
            return;
        }

        if(PhpElementsUtil.getClassMethod(serviceParameterClass, PsiElementUtils.trimQuote(psiElement.getText())) == null) {
            holder.createWeakWarningAnnotation(psiElement, "Unknown method");
        }

    }

    private String getServiceName(PsiElement psiElement) {
        return YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(psiElement));
    }
}