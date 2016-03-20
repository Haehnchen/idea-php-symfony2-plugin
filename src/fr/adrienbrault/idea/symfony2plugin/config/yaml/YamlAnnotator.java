package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.intentions.ui.ServiceSuggestDialog;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLArray;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLSequence;

import java.util.Collection;
import java.util.List;

public class YamlAnnotator implements Annotator {

    private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

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

        this.annotateConstructorSequenceArguments(psiElement, holder);
        this.annotateConstructorArguments(psiElement, holder);
        this.annotateCallsArguments(psiElement, holder);

        this.lazyServiceCollector = null;
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

        // dont mark "@", "@?", "@@" escaping and expressions
        if(serviceName.length() < 2 || serviceName.startsWith("=") || serviceName.startsWith("@")) {
            return;
        }

        if(ContainerCollectionResolver.hasServiceNames(psiElement.getProject(), serviceName)) {
            return;
        }

        holder.createWarningAnnotation(psiElement, "Missing Service");
    }

    private void annotateClass(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!((YamlElementPatternHelper.getSingleLineScalarKey("class", "factory_class").accepts(element) || YamlElementPatternHelper.getParameterClassPattern().accepts(element)) && YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(element))) {
            return;
        }

        String className = PsiElementUtils.getText(element);

        if(YamlHelper.isValidParameterName(className)) {
            String resolvedParameter = ContainerCollectionResolver.resolveParameter(element.getProject(), className);
            if(resolvedParameter != null && PhpElementsUtil.getClassInterfacePsiElements(element.getProject(), resolvedParameter) != null) {
                return ;
            }
        }

        if(PhpElementsUtil.getClassInterface(element.getProject(), className) == null) {
            holder.createWarningAnnotation(element, "Missing Class");
        }

    }

    /**
     * arguments:
     *    - @twig
     *    - @twig
     */
    private void annotateConstructorSequenceArguments(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(isStringValue(psiElement)) {
            PsiElement yamlSequence = psiElement.getContext();
            if(yamlSequence instanceof YAMLSequence) {
                PsiElement yamlCompoundValue = yamlSequence.getContext();
                if(yamlCompoundValue instanceof YAMLCompoundValue) {
                    PsiElement yamlKeyValue = yamlCompoundValue.getContext();
                    if(yamlKeyValue instanceof YAMLKeyValue) {
                        String keyText = ((YAMLKeyValue) yamlKeyValue).getKeyText();
                        if("arguments".equals(keyText)) {
                            List<YAMLSequence> test = PsiElementUtils.getPrevSiblingsOfType(yamlSequence, PlatformPatterns.psiElement(YAMLSequence.class));

                            PsiElement yamlCompoundValueService = yamlKeyValue.getParent();
                            if(yamlCompoundValueService instanceof YAMLCompoundValue) {
                                String className = YamlHelper.getYamlKeyValueAsString((YAMLCompoundValue) yamlCompoundValueService, "class", false);
                                if(className != null) {
                                    PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), className, this.getLazyServiceCollector(psiElement.getProject()));
                                    if(serviceClass != null) {
                                        Method constructor = serviceClass.getConstructor();
                                        if(constructor != null) {
                                            attachInstanceAnnotation(psiElement, holder, test.size(), constructor);
                                        }
                                    }
                                }
                            }


                        }
                    }
                }
            }

        }

    }

    private void annotateConstructorArguments(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if (!isStringValue(psiElement)) {
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

        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classKeyValue.getValueText(), this.getLazyServiceCollector(psiElement.getProject()));
        if(serviceClass == null) {
            return;
        }

        Method constructor = serviceClass.getConstructor();
        if(constructor == null) {
            return;
        }

        attachInstanceAnnotation(psiElement, holder, yamlArray, constructor);

    }

    private boolean isStringValue(@NotNull PsiElement psiElement) {
        return PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING).accepts(psiElement)
        ;
    }

    private void annotateCallsArguments(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!isStringValue(psiElement)){
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

        List<PsiElement> methodParameter = YamlHelper.getYamlArrayValues(yamlCallArray);
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

        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classKeyValue.getValueText(), this.getLazyServiceCollector(psiElement.getProject()));
        if(serviceClass == null) {
            return;
        }

        Method method = serviceClass.findMethodByName(methodName);
        if (method == null) {
            return;
        }

        attachInstanceAnnotation(psiElement, holder, yamlCallParameterArray, method);

    }
    private void attachInstanceAnnotation(PsiElement psiElement, AnnotationHolder holder, int parameterIndex, Method constructor) {

        String serviceName = getServiceName(psiElement);
        if(StringUtils.isBlank(serviceName)) {
            return;
        }

        PhpClass serviceParameterClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), getServiceName(psiElement), this.getLazyServiceCollector(psiElement.getProject()));
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

        if(!PhpElementsUtil.isInstanceOf(serviceParameterClass, expectedClass)) {
            holder.createWeakWarningAnnotation(psiElement, "Expect instance of: " + expectedClass.getPresentableFQN())
                .registerFix(new MySuggestIntentionAction(expectedClass, psiElement));
        }
    }

    private void attachInstanceAnnotation(PsiElement psiElement, AnnotationHolder holder, YAMLArray yamlArray, Method constructor) {

        if(psiElement == null) {
            return;
        }

        int parameterIndex = YamlHelper.getYamlParameter(yamlArray, psiElement);
        if(parameterIndex == -1) {
            return;
        }

        attachInstanceAnnotation(psiElement, holder, parameterIndex, constructor);
    }

    private String getServiceName(PsiElement psiElement) {
        return YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(psiElement));
    }

    private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(Project project) {
        return this.lazyServiceCollector == null ? this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(project) : this.lazyServiceCollector;
    }

    private static class MySuggestIntentionAction extends PsiElementBaseIntentionAction {

        @NotNull
        private final PhpClass expectedClass;

        @NotNull
        private final PsiElement myPsiElement;

        public MySuggestIntentionAction(@NotNull PhpClass expectedClass, @NotNull PsiElement psiElement) {
            this.expectedClass = expectedClass;
            this.myPsiElement = psiElement;
        }

        @Override
        public void invoke(@NotNull final Project project, final Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
            Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionForPhpClass(expectedClass, ContainerCollectionResolver.getServices(project));
            if(suggestions.size() == 0) {
                HintManager.getInstance().showErrorHint(editor, "No suggestion found");
                return;
            }

            ServiceSuggestDialog.create(ContainerUtil.map(suggestions, new Function<ContainerService, String>() {
                @Override
                public String fun(ContainerService containerService) {
                    return containerService.getName();
                }
            }), new MyInsertCallback(editor, myPsiElement));
        }

        @Override
        public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
            return true;
        }

        @Nls
        @NotNull
        @Override
        public String getFamilyName() {
            return "Symfony";
        }

        @NotNull
        @Override
        public String getText() {
            return "Symfony: Suggest Service";
        }
    }

    /**
     * This class replace a service name by plain text modification.
     * This resolve every crazy yaml use case and lexer styles like:
     *
     *  - @, @?
     *  - "@foo", '@foo', @foo
     */
    private static class MyInsertCallback implements ServiceSuggestDialog.Callback {

        @NotNull
        private final Editor editor;

        @NotNull
        private final PsiElement psiElement;

        public MyInsertCallback(@NotNull Editor editor, @NotNull PsiElement psiElement) {
            this.editor = editor;
            this.psiElement = psiElement;
        }

        @Override
        public void insert(@NotNull String selected) {
            String text = this.psiElement.getText();

            int i = getServiceChar(text);
            if(i < 0) {
                HintManager.getInstance().showErrorHint(editor, "No valid char in text range");
                return;
            }

            String afterAtText = text.substring(i);

            // strip ending quotes
            int length = StringUtils.stripEnd(afterAtText, "'\"").length();

            int startOffset = this.psiElement.getTextRange().getStartOffset();
            int afterAt = startOffset + i + 1;

            editor.getDocument().deleteString(afterAt, afterAt + length - 1);
            editor.getDocument().insertString(afterAt, selected);
        }

        private int getServiceChar(@NotNull String text) {
            int i = text.lastIndexOf("@?");
            if(i >= 0) {
                return i + 1;
            }

            return text.lastIndexOf("@");
        }
    }
}