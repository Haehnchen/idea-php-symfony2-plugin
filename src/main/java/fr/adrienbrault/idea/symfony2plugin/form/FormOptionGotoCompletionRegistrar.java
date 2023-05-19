package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.utils.GotoCompletionUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionLookupVisitor;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionTargetVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormOptionGotoCompletionRegistrar implements GotoCompletionRegistrar {
    /**
     * Symfony 2 / 3 switch
     */
    private static final String[] DEFAULT_FORM = {
        "form",
        "Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType"
    };

    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        registrar.register(
            PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE),
            new FormOptionBuilderCompletionContributor()
        );

        /*
         * eg "$resolver->setDefault('<caret>')"
         */
        registrar.register(
            PlatformPatterns.psiElement().withParent(PhpElementsUtil.getMethodWithFirstStringOrNamedArgumentPattern()),
            new OptionDefaultCompletionContributor()
        );

        /*
         * eg "$resolver->setDefaults('<caret>')"
         */
        registrar.register(
            PhpElementsUtil.getParameterListArrayValuePattern(),
            new OptionDefaultsCompletionContributor()
        );

        /*
         * $options
         * public function buildForm(FormBuilderInterface $builder, array $options) {
         *   $options['foo']
         * }
         *
         * public function setDefaultOptions(OptionsResolverInterface $resolver) {
         *   $resolver->setDefaults([
         *    'foo' => 'bar',
         * ]);
         * }
         */
        registrar.register(
            PlatformPatterns.psiElement().withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)),
            new FormArrayAccessOptionGotoCompletionContributor()
        );
    }

    private static class FormOptionBuilderCompletionContributor implements GotoCompletionContributor {
        @Nullable
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(psiElement.getParent());
            if(arrayCreationExpression != null) {
                PsiElement parameterList = arrayCreationExpression.getParent();
                if (parameterList instanceof ParameterList) {
                    PsiElement context = parameterList.getContext();
                    if(context instanceof MethodReference) {
                        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayCreationExpression);
                        if(currentIndex != null && currentIndex.getIndex() == 2) {
                            if (PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) context, FormUtil.PHP_FORM_BUILDER_SIGNATURES)) {
                                return getMatchingOption((ParameterList) parameterList, psiElement);
                            }
                        }
                    }
                }
            }

            return null;
        }

        private GotoCompletionProvider getMatchingOption(ParameterList parameterList, @NotNull PsiElement psiElement) {
            // form name can be a string alias; also resolve on constants, properties, ...
            PsiElement psiElementAt = PsiElementUtils.getMethodParameterPsiElementAt(parameterList, 1);

            Set<String> formTypeNames = new HashSet<>();
            if(psiElementAt != null) {
                PhpClass phpClass = FormUtil.getFormTypeClassOnParameter(psiElementAt);
                if(phpClass != null) {
                    formTypeNames.add(phpClass.getFQN());
                }
            }

            // fallback to form
            if(formTypeNames.size() == 0) {
                formTypeNames.add("form"); // old Symfony systems
                formTypeNames.add("Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType");
            }

            return new FormReferenceCompletionProvider(psiElement, formTypeNames);
        }
    }

    private static class FormReferenceCompletionProvider extends GotoCompletionProvider {
        @NotNull
        private final Collection<String> formTypes;

        FormReferenceCompletionProvider(@NotNull PsiElement element, @NotNull Collection<String> formTypes) {
            super(element);
            this.formTypes = formTypes;
        }

        @NotNull
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {

            PsiElement element = psiElement.getParent();
            if(!(element instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            final String value = ((StringLiteralExpression) element).getContents();
            if(StringUtils.isBlank(value)) {
                return Collections.emptyList();
            }

            final Collection<PsiElement> psiElements = new ArrayList<>();
            for (String formType : formTypes) {
                FormOptionsUtil.visitFormOptions(getProject(), formType, new FormOptionTargetVisitor(value, psiElements));
            }

            return psiElements;
        }

        @NotNull
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> lookupElements = new ArrayList<>();

            for (String formType : formTypes) {
                FormOptionsUtil.visitFormOptions(getProject(), formType, new FormOptionLookupVisitor(lookupElements));
            }

            return lookupElements;
        }
    }

    /*
     * eg "$resolver->setDefault('<caret>')"
     */
    private static class FormOptionGotoCompletionProvider extends GotoCompletionProvider {
        private final Collection<String> formTypes;

        FormOptionGotoCompletionProvider(PsiElement psiElement, @NotNull Collection<String> formTypes) {
            super(psiElement);
            this.formTypes = formTypes;
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> elements = new ArrayList<>();

            for (String formType : this.formTypes) {
                FormOptionsUtil.visitFormOptions(
                    getProject(),
                    formType,
                    new FormOptionLookupVisitor(elements)
                );
            }

            return elements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String contents = GotoCompletionUtil.getTextValueForElement(element);
            if(contents == null) {
                return Collections.emptyList();
            }

            Collection<PsiElement> elements = new ArrayList<>();
            for (String formType : this.formTypes) {
                FormOptionsUtil.visitFormOptions(
                    getProject(),
                    formType,
                    new FormOptionTargetVisitor(contents, elements)
                );
            }

            return elements;
        }
    }

    private static class OptionDefaultCompletionContributor implements GotoCompletionContributor {
        @Nullable
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            MethodMatcher.StringParameterRecursiveMatcher matcher = new MethodMatcher.StringParameterRecursiveMatcher(context, 0);

            String[] methods = new String[] {
                "setDefault", "hasDefault", "isRequired", "isMissing",
                "setAllowedValues", "addAllowedValues", "setAllowedTypes", "addAllowedTypes"
            };

            // @TODO: drop too many classes, add PhpMatcher
            for (String method : methods) {
                matcher.withSignature("Symfony\\Component\\OptionsResolver\\OptionsResolver", method);

                // BC: Symfony < 3
                matcher.withSignature("Symfony\\Component\\OptionsResolver\\OptionsResolverInterface", method);
            }

            if (matcher.match() == null) {
                return null;
            }

            Collection<String> formTypes = new HashSet<>(Set.of(DEFAULT_FORM));
            formTypes.addAll(FormUtil.getFormTypeParentFromOptionResolverScope(psiElement));

            String formTypeClassFromScope = FormUtil.getFormTypeClassFromScope(psiElement);
            if (formTypeClassFromScope != null) {
                formTypes.add(formTypeClassFromScope);
            }

            return new FormOptionGotoCompletionProvider(psiElement, Collections.unmodifiableCollection(formTypes));
        }
    }

    /*
     * eg "$resolver->setDefaults('<caret>' => '')"
     */
    private static class OptionDefaultsCompletionContributor implements GotoCompletionContributor {
        @Nullable
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            ParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, ParameterList.class);
            if (parameterList == null) {
                return null;
            }

            if (!(parameterList.getContext() instanceof MethodReference method)) {
                return null;
            }

            // Symfony 2 and 3 BC fix
            if (!(PhpElementsUtil.isMethodReferenceInstanceOf(method, "\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface", "setDefaults") ||
                PhpElementsUtil.isMethodReferenceInstanceOf(method, "\\Symfony\\Component\\OptionsResolver\\OptionsResolver", "setDefaults"))
            ) {
                return null;
            }

            // only use second parameter
            ArrayCreationExpression arrayHash = PsiTreeUtil.getParentOfType(psiElement, ArrayCreationExpression.class);
            if (arrayHash == null) {
                return null;
            }

            ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayHash);
            if (currentIndex == null || currentIndex.getIndex() != 0) {
                return null;
            }

            if (PhpElementsUtil.getCompletableArrayCreationElement(context) != null) {
                Set<String> formTypes = new HashSet<>();

                String formTypeClassFromScope = FormUtil.getFormTypeClassFromScope(psiElement);
                if (formTypeClassFromScope != null) {
                    formTypes.add(formTypeClassFromScope);
                }

                formTypes.addAll(FormUtil.getFormTypeParentFromOptionResolverScope(psiElement));

                return new OptionDefaultsCompletionGotoCompletionProvider(psiElement, formTypes);
            }

            return null;
        }
    }

    /*
     * eg "$resolver->setDefaults('<caret>' => '')"
     */
    private static class OptionDefaultsCompletionGotoCompletionProvider extends GotoCompletionProvider {
        private final Collection<String> formTypes;

        OptionDefaultsCompletionGotoCompletionProvider(PsiElement psiElement, @NotNull Collection<String> formTypes) {
            super(psiElement);
            this.formTypes = new HashSet<>();

            this.formTypes.add("form");
            this.formTypes.add("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType");
            this.formTypes.addAll(formTypes);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> elements = new ArrayList<>(
                FormOptionsUtil.getFormExtensionKeysLookupElements(getElement().getProject(), formTypes.toArray(new String[0]))
            );

            for (String formType : this.formTypes) {
                elements.addAll(FormOptionsUtil.getDefaultOptionLookupElements(getElement().getProject(), formType));
            }

            return elements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            PsiElement parent = element.getParent();
            if (!(parent instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            Collection<PsiElement> targets = new HashSet<>(
                FormOptionsUtil.getFormExtensionsKeysTargets((StringLiteralExpression) parent, formTypes.toArray(new String[0]))
            );

            for (String formType : this.formTypes) {
                targets.addAll(FormOptionsUtil.getDefaultOptionTargets((StringLiteralExpression) parent, formType));
            }

            return targets;
        }
    }

    private static class FormArrayAccessOptionGotoCompletionContributor implements GotoCompletionContributor {
        @Override
        public @Nullable GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            PsiElement arrayIndex = context.getContext();
            if(!(arrayIndex instanceof ArrayIndex)) {
                return null;
            }

            PhpPsiElement variable = ((ArrayIndex) arrayIndex).getPrevPsiSibling();
            if(!(variable instanceof Variable)) {
                return null;
            }

            PsiElement parameter = ((Variable) variable).resolve();

            if(!(parameter instanceof Parameter)) {
                return null;
            }

            // all options keys are at parameter = 1 by now
            ParameterBag parameterBag = PsiElementUtils.getCurrentParameterIndex((Parameter) parameter);
            if(parameterBag == null || parameterBag.getIndex() != 1) {
                return null;
            }

            Method method = PsiTreeUtil.getParentOfType(parameter, Method.class);
            if(method == null) {
                return null;
            }

            if(!PhpElementsUtil.isMethodInstanceOf(method, FormTypeReferenceContributor.BUILDER_SIGNATURES)) {
                return null;
            }

            PhpClass phpClass = method.getContainingClass();
            if(phpClass == null) {
                return null;
            }

            Set<String> formTypes = new HashSet<>();

            String formTypeClassFromScope = FormUtil.getFormTypeClassFromScope(psiElement);
            if (formTypeClassFromScope != null) {
                formTypes.add(formTypeClassFromScope);
            }

            formTypes.addAll(FormUtil.getFormTypeParentFromFormTypeImplementation(arrayIndex));

            return new OptionDefaultsCompletionGotoCompletionProvider(psiElement, formTypes);
        }
    }
}
