package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormFieldNameReference;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationDomainReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationReference;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeReferenceContributor extends PsiReferenceContributor {

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

                    if(!(parameterList.getContext() instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    MethodReference method = (MethodReference) parameterList.getContext();
                    Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
                    if (!interfacesUtil.isFormBuilderFormTypeCall(method)) {
                        return new PsiReference[0];
                    }

                    ArrayHashElement arrayHash = PsiTreeUtil.getParentOfType(psiElement, ArrayHashElement.class);
                    if(arrayHash != null && arrayHash.getKey() instanceof StringLiteralExpression) {

                        ArrayCreationExpression arrayCreation = PsiTreeUtil.getParentOfType(psiElement, ArrayCreationExpression.class);
                        if(arrayCreation == null) {
                            return new PsiReference[0];
                        }

                        // old 3 parameter hold valid array data
                        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayCreation);
                        if(currentIndex == null || currentIndex.getIndex() != 2) {
                            return new PsiReference[0];
                        }

                        StringLiteralExpression key = (StringLiteralExpression) arrayHash.getKey();
                        if(key == null) {
                            return new PsiReference[0];
                        }

                        String keyString = key.getContents();

                        // @TODO: how to handle custom bundle fields like help_block
                        if(keyString.equals("label") || keyString.equals("help_block") || keyString.equals("help_inline")) {

                            // translation_domain in current array block
                            String translationDomain = PhpElementsUtil.getArrayHashValue(arrayCreation, "translation_domain");
                            if(translationDomain == null) {
                                translationDomain = PhpElementsUtil.getArrayKeyValueInsideSignature(psiElement, FormOptionsUtil.FORM_OPTION_METHODS, "setDefaults", "translation_domain");
                            }

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

            }

        );

        /**
         * support form type alias references;
         * we dont use completion here, form type resolving depends on container, which is slow stuff
         */
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    // match add('foo', 'type name')
                    MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 1)
                        .withSignature(Symfony2InterfacesUtil.getFormBuilderInterface())
                        .match();

                    if(methodMatchParameter == null) {
                        methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, 1)
                            .withSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "createNamedBuilder")
                            .withSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "createNamed")
                            .match();
                    }

                    if(methodMatchParameter == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{ new FormTypeReferenceRef((StringLiteralExpression) psiElement) };

                }

            }

        );

        // FormBuilderInterface::add('underscore_method')
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList)) {
                        return new PsiReference[0];
                    }

                    ParameterList parameterList = (ParameterList) psiElement.getContext();

                    if (parameterList == null || !(parameterList.getContext() instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    MethodReference method = (MethodReference) parameterList.getContext();
                    if (method == null || !new Symfony2InterfacesUtil().isFormBuilderFormTypeCall(method)) {
                        return new PsiReference[0];
                    }

                    // only use second parameter
                    ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
                    if (currentIndex == null || currentIndex.getIndex() != 0) {
                        return new PsiReference[0];
                    }

                    PsiElement className = PhpElementsUtil.getArrayKeyValueInsideSignaturePsi(psiElement, FormOptionsUtil.FORM_OPTION_METHODS, "setDefaults", "data_class");
                    if(className == null) {
                        return new PsiReference[0];
                    }

                    PhpClass phpClass = PhpElementsUtil.resolvePhpClassOnPsiElement(className);
                    if (phpClass == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{new FormUnderscoreMethodReference((StringLiteralExpression) psiElement, phpClass)};
                }

            }

        );

        // FormBuilderInterface::add('underscore_method')
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
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

                    if(!(parameterList.getContext() instanceof MethodReference)) {
                        return new PsiReference[0];
                    }

                    MethodReference method = (MethodReference) parameterList.getContext();
                    Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
                    if (!interfacesUtil.isCallTo(method, "\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface", "setDefaults")) {
                        return new PsiReference[0];
                    }

                    // only use second parameter
                    ArrayCreationExpression arrayHash = PsiTreeUtil.getParentOfType(psiElement, ArrayCreationExpression.class);
                    if(arrayHash == null) {
                        return new PsiReference[0];
                    }

                    ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayHash);
                    if(currentIndex == null || currentIndex.getIndex() != 0) {
                        return new PsiReference[0];
                    }

                    if(PhpElementsUtil.getCompletableArrayCreationElement(psiElement) != null) {
                        return new PsiReference[]{
                            new FormExtensionKeyReference((StringLiteralExpression) psiElement),
                            new FormDefaultOptionsKeyReference((StringLiteralExpression) psiElement, "form")
                        };
                    }

                    return new PsiReference[0];
                }

            }

        );

        // $form->get('field')
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {


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

            }

        );

        /**
         * $options
         * public function buildForm(FormBuilderInterface $builder, array $options) {
         *   $options['foo']
         * }
         *
         * public function setDefaultOptions(OptionsResolverInterface $resolver) {
         *   $resolver->setDefaults(array(
         *    'foo' => 'bar',
         * ));
         }

         */
        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {

                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    PsiElement context = psiElement.getContext();
                    if(!(context instanceof ArrayIndex)) {
                        return new PsiReference[0];
                    }

                    PhpPsiElement variable = ((ArrayIndex) context).getPrevPsiSibling();
                    if(!(variable instanceof Variable)) {
                        return new PsiReference[0];
                    }

                    PsiElement parameter = ((Variable) variable).resolve();

                    if(!(parameter instanceof Parameter)) {
                        return new PsiReference[0];
                    }

                    // all options keys are at parameter = 1 by now
                    ParameterBag parameterBag = PsiElementUtils.getCurrentParameterIndex((Parameter) parameter);
                    if(parameterBag == null || parameterBag.getIndex() != 1) {
                        return new PsiReference[0];
                    }

                    Method method = PsiTreeUtil.getParentOfType(parameter, Method.class);
                    if(method == null) {
                        return new PsiReference[0];
                    }

                    Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
                    if(!symfony2InterfacesUtil.isCallTo(method, "\\Symfony\\Component\\Form\\FormTypeInterface", "buildForm") &&
                        !symfony2InterfacesUtil.isCallTo(method, "\\Symfony\\Component\\Form\\FormTypeInterface", "buildView") &&
                        !symfony2InterfacesUtil.isCallTo(method, "\\Symfony\\Component\\Form\\FormTypeInterface", "finishView"))
                    {
                        return new PsiReference[0];
                    }

                    PhpClass phpClass = method.getContainingClass();
                    if(phpClass == null) {
                        return new PsiReference[0];
                    }

                    return new PsiReference[]{
                        new FormExtensionKeyReference((StringLiteralExpression) psiElement),
                        new FormDefaultOptionsKeyReference((StringLiteralExpression) psiElement, phpClass.getPresentableFQN())
                    };

                }

            }

        );


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
