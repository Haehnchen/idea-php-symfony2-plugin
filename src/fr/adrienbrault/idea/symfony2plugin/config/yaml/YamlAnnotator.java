package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
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
import org.jetbrains.yaml.psi.YAMLScalar;
import org.jetbrains.yaml.psi.YAMLSequenceItem;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlAnnotator implements Annotator {

    private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

    @Override
    public void annotate(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement.getProject())) {
            return;
        }

        this.annotateParameter(psiElement, holder);
        this.annotateClass(psiElement, holder);

        // only match inside service definitions
        if(!YamlElementPatternHelper.getInsideKeyValue("services").accepts(psiElement)) {
            return;
        }

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
     * foo:
     *  class: Foo
     *  arguments: [@<caret>]
     *  arguments:
     *      - @<caret>
     */
    private void annotateConstructorArguments(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        ServiceTypeHint methodTypeHint = ServiceContainerUtil.getYamlConstructorTypeHint(psiElement, getLazyServiceCollector(psiElement.getProject()));
        if(methodTypeHint == null) {
            return;
        }

        attachInstanceAnnotation(psiElement, holder, methodTypeHint.getIndex(), methodTypeHint.getMethod());
    }

    public static boolean isStringValue(@NotNull PsiElement psiElement) {
        // @TODO use new YAMLScalar element
        return PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING).accepts(psiElement)
        ;
    }

    /**
     * class: FooClass
     * tags:
     *  - [ setFoo, [@args_bar] ]
     */
    private void annotateCallsArguments(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!isStringValue(psiElement)){
            return;
        }

        PsiElement yamlScalar = psiElement.getContext();
        if(!(yamlScalar instanceof YAMLScalar)) {
            return;
        }

        YamlHelper.visitServiceCallArgument((YAMLScalar) yamlScalar, visitor -> {
            PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), visitor.getClassName(), getLazyServiceCollector(psiElement.getProject()));
            if(serviceClass == null) {
                return;
            }

            Method method = serviceClass.findMethodByName(visitor.getMethod());
            if (method == null) {
                return;
            }

            attachInstanceAnnotation(psiElement, holder, visitor.getParameterIndex(), method);
        });

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

            ServiceSuggestDialog.create(
                editor,
                ContainerUtil.map(suggestions, ContainerService::getName),
                new MyInsertCallback(editor, myPsiElement)
            );
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