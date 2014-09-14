package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class FormGotoCompletionRegistrar implements GotoCompletionRegistrar {

    public void register(GotoCompletionRegistrarParameter registrar) {

        // FormBuilderInterface:add("", "type")
        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), new GotoCompletionContributor() {
            @Nullable
            @Override
            public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {

                PsiElement parent = psiElement.getParent();
                if(parent == null) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(parent, 1)
                    .withSignature(Symfony2InterfacesUtil.getFormBuilderInterface())
                    .match();

                if(methodMatchParameter == null) {
                    return null;
                }

                return new FormBuilderAddGotoCompletionProvider(parent);
            }
        });

        /**
         * $options lookup
         * public function createNamedBuilder($name, $type = 'form', $data = null, array $options = array())
         */
        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), new GotoCompletionContributor() {
            @Nullable
            @Override
            public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {

                PsiElement parent = psiElement.getParent();
                if(!(parent instanceof StringLiteralExpression)) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(parent, 3)
                    .withSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "createNamedBuilder")
                    .withSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "createNamed")
                    .match();

                if(methodMatchParameter == null) {
                    return null;
                }

                return getFormProvider((StringLiteralExpression) parent, methodMatchParameter.getParameters()[1]);

            }

        });


        /**
         * $this->createForm(new FormType(), $entity, array('<foo_key>' => ''));
         * $this->createForm('foo', $entity, array('<foo_key>'));
         */
        registrar.register(PlatformPatterns.psiElement().withParent(StringLiteralExpression.class).withLanguage(PhpLanguage.INSTANCE), new GotoCompletionContributor() {
            @Nullable
            @Override
            public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {

                PsiElement parent = psiElement.getParent();
                if(!(parent instanceof StringLiteralExpression)) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.ArrayParameterMatcher(parent, 2)
                    .withSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "createForm")
                    .withSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "create")
                    .withSignature("\\Symfony\\Component\\Form\\FormFactory", "createBuilder")
                    .match();

                if(methodMatchParameter == null) {
                    return null;
                }

                return getFormProvider((StringLiteralExpression) parent, methodMatchParameter.getParameters()[0]);

            }

        });

    }

    private static class FormOptionsGotoCompletionProvider extends GotoCompletionProvider {

        private final String formType;
        private final Collection<FormOption> options;

        public FormOptionsGotoCompletionProvider(@NotNull PsiElement element, @NotNull String formType, FormOption... options) {
            super(element);
            this.formType = formType;
            this.options = Arrays.asList(options);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {

            Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

            if(options.contains(FormOption.EXTENSION)) {
                lookupElements.addAll(FormOptionsUtil.getFormExtensionKeysLookupElements(getElement().getProject(), this.formType));
            }

            if(options.contains(FormOption.DEFAULT_OPTIONS)) {
                lookupElements.addAll(FormOptionsUtil.getDefaultOptionLookupElements(getElement().getProject(), this.formType));
            }

            return lookupElements;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {

            PsiElement element = psiElement.getParent();
            if(!(element instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            Collection<PsiElement> targets = new ArrayList<PsiElement>();

            if(options.contains(FormOption.EXTENSION)) {
                targets.addAll(FormOptionsUtil.getFormExtensionsKeysTargets((StringLiteralExpression) element, this.formType));
            }

            if(options.contains(FormOption.DEFAULT_OPTIONS)) {
                targets.addAll(FormOptionsUtil.getDefaultOptionTargets((StringLiteralExpression) element, this.formType));
            }

            return targets;

        }
    }

    private static class FormBuilderAddGotoCompletionProvider extends GotoCompletionProvider {

        public FormBuilderAddGotoCompletionProvider(PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return FormUtil.getFormTypeLookupElements(getElement().getProject());
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {

            PsiElement element = psiElement.getParent();
            if(!(element instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            PhpClass formTypeToClass = FormUtil.getFormTypeToClass(getElement().getProject(), ((StringLiteralExpression) element).getContents());
            if(formTypeToClass == null) {
                return Collections.emptyList();
            }

            return Arrays.asList(new PsiElement[] { formTypeToClass });
        }
    }

    private static enum FormOption {
        EXTENSION, DEFAULT_OPTIONS
    }

    private FormOptionsGotoCompletionProvider getFormProvider(StringLiteralExpression psiElement, PsiElement formType) {

        PhpClass phpClass = FormUtil.getFormTypeClassOnParameter(formType);
        if (phpClass == null) {
            return new FormOptionsGotoCompletionProvider(psiElement, "form", FormOption.EXTENSION);
        }

        String presentableFQN = phpClass.getPresentableFQN();
        if(presentableFQN == null) {
            presentableFQN = "form";
        }

        return new FormOptionsGotoCompletionProvider(psiElement, presentableFQN, FormOption.EXTENSION, FormOption.DEFAULT_OPTIONS);
    }
}
