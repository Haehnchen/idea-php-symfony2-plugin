package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormFieldNameReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationReference;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeReferenceContributor extends PsiReferenceContributor {
    public static final MethodMatcher.CallToSignature[] BUILDER_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormTypeInterface", "buildForm"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormTypeInterface", "buildView"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormTypeInterface", "finishView"),
    };

    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar psiReferenceRegistrar) {

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(
                    PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).inside(ParameterList.class)
                ),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    ParameterList parameterList = PsiTreeUtil.getParentOfType(psiElement, ParameterList.class);
                    if (parameterList == null) {
                        return new PsiReference[0];
                    }

                    PsiElement methodReference = parameterList.getContext();
                    if(!(methodReference instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    if (!PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, FormUtil.PHP_FORM_BUILDER_SIGNATURES)) {
                        return new PsiReference[0];
                    }

                    ArrayHashElement arrayHash = PsiTreeUtil.getParentOfType(psiElement, ArrayHashElement.class);
                    if(arrayHash != null && arrayHash.getKey() instanceof StringLiteralExpression key) {
                        ArrayCreationExpression arrayCreation = PsiTreeUtil.getParentOfType(psiElement, ArrayCreationExpression.class);

                        if(arrayCreation == null) {
                            return new PsiReference[0];
                        }

                        // old 3 parameter hold valid array data
                        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayCreation);
                        if(currentIndex == null || currentIndex.getIndex() != 2) {
                            return new PsiReference[0];
                        }

                        if(key == null) {
                            return new PsiReference[0];
                        }

                        String keyString = key.getContents();

                        // @TODO: how to handle custom bundle fields like help_block
                        if(keyString.equals("label") || keyString.equals("help_block") || keyString.equals("help_inline") || keyString.equals("placeholder") || keyString.equals("help")) {
                            // translation_domain in current array block

                            String translationDomain = FormOptionsUtil.getTranslationFromScope(arrayCreation);
                            if(translationDomain == null) {
                                translationDomain = "messages";
                            }

                            return new PsiReference[]{ new TranslationReference((StringLiteralExpression) psiElement, translationDomain) };
                        }

                        if(keyString.equals("class")) {
                            return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement, true)};
                        }
                    }

                    return new PsiReference[0];
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        /*
         * support form type alias references;
         * we dont use completion here, form type resolving depends on container, which is slow stuff
         */
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    // match add('foo', 'type name')
                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 1)
                        .withSignature(FormUtil.PHP_FORM_BUILDER_SIGNATURES)
                        .match();

                    if(methodMatchParameter == null) {
                        methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 1)
                            .withSignature(FormUtil.PHP_FORM_NAMED_BUILDER_SIGNATURES)
                            .match();
                    }

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new FormTypeReferenceRef((StringLiteralExpression) psiElement) };
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // FormBuilderInterface::add('underscore_method')
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList parameterList)) {
                        return new PsiReference[0];
                    }

                    PsiElement methodReference = parameterList.getContext();
                    if (!(methodReference instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    if (!PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, FormUtil.PHP_FORM_BUILDER_SIGNATURES)) {
                        return new PsiReference[0];
                    }

                    // only use second parameter
                    ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
                    if (currentIndex == null || currentIndex.getIndex() != 0) {
                        return new PsiReference[0];
                    }

                    PhpClass phpClass = getFormPhpClassFromContext(psiElement);
                    if(phpClass == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{new FormUnderscoreMethodReference((StringLiteralExpression) psiElement, phpClass)};
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );

        // $form->get('field')
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 0)
                        .withSignature("\\Symfony\\Component\\Form\\FormInterface", "get")
                        .withSignature("\\Symfony\\Component\\Form\\FormInterface", "has")
                        .match();

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    Method method = FormUtil.resolveFormGetterCallMethod(methodMatchParameter.getMethodReference());
                    if(method == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[] {
                        new FormFieldNameReference((StringLiteralExpression) psiElement, method)
                    };
                }

                @Override
                public boolean acceptsTarget(@NotNull PsiElement target) {
                    return Symfony2ProjectComponent.isEnabled(target);
                }
            }
        );
    }

    @Nullable
    public static PhpClass getFormPhpClassFromContext(@NotNull PsiElement psiElement) {
        Collection<String> classes = new ArrayList<>();

        // $resolver->setDefaults(['data_class' => User::class]);
        PsiElement className = PhpElementsUtil.getArrayKeyValueInsideSignaturePsi(psiElement, FormOptionsUtil.FORM_OPTION_METHODS, "setDefaults", "data_class");
        if(className != null) {
            String stringValue = PhpElementsUtil.getStringValue(className);
            if(stringValue != null) {
                classes.add(stringValue);
            }
        }

        // $resolver->setDefault('data_class', User::class);
        classes.addAll(FormOptionsUtil.getMethodReferenceStringParameter(psiElement, FormOptionsUtil.FORM_OPTION_METHODS, "setDefault", "data_class"));

        // find first class
        Project project = psiElement.getProject();

        return classes.stream()
            .map(clazz -> PhpElementsUtil.getClassInterface(project, clazz))
            .filter(Objects::nonNull).findFirst()
            .orElse(null);
    }

    private static class FormTypeReferenceRef extends FormTypeReference {
        public FormTypeReferenceRef(@NotNull StringLiteralExpression element) {
            super(element);
        }

        @NotNull
        @Override
        public Object[] getVariants() {
            return new Object[0];
        }
    }
}
