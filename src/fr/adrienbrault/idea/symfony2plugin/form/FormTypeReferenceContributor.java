package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpTypedElementImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationDomainReference;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationReference;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
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

                        if(keyString.equals("translation_domain")) {
                            return new PsiReference[]{ new TranslationDomainReference((StringLiteralExpression) psiElement) };
                        }

                        // @TODO: how to handle custom bundle fields like help_block
                        if(keyString.equals("label") || keyString.equals("help_block") || keyString.equals("help_inline")) {

                            // translation_domain in current array block
                            String translationDomain = PhpElementsUtil.getArrayHashValue(arrayCreation, "translation_domain");
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
                    if (!interfacesUtil.isCallTo(method, "\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface", "setDefaults")) {
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
                        if(currentIndex == null || currentIndex.getIndex() != 0) {
                            return new PsiReference[0];
                        }

                        StringLiteralExpression key = (StringLiteralExpression) arrayHash.getKey();
                        if(key == null) {
                            return new PsiReference[0];
                        }

                        String keyString = key.getContents();
                        if(keyString.equals("data_class")) {
                            return new PsiReference[]{ new EntityReference((StringLiteralExpression) psiElement, true)};
                        }

                    }

                    return new PsiReference[0];

                }

            }

        );

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
                        Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
                        if (!interfacesUtil.isFormBuilderFormTypeCall(method)) {
                            return new PsiReference[0];
                        }

                        // only use second parameter
                        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
                        if(currentIndex == null || currentIndex.getIndex() != 1) {
                            return new PsiReference[0];
                        }

                        return new PsiReference[]{ new FormTypeReference((StringLiteralExpression) psiElement) };
                    }

                }

        );

        psiReferenceRegistrar.registerReferenceProvider(
            PlatformPatterns.psiElement(StringLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
                    if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                        return new PsiReference[0];
                    }

                    // array('<test>' => '')
                    if(PhpPatterns.psiElement(PhpElementTypes.ARRAY_KEY).accepts(psiElement.getContext())) {
                        PsiElement arrayKey = psiElement.getContext();
                        if(arrayKey != null) {
                            PsiElement arrayHashElement = arrayKey.getContext();
                            if(arrayHashElement instanceof ArrayHashElement) {
                                PsiElement arrayCreationExpression = arrayHashElement.getContext();
                                if(arrayCreationExpression instanceof ArrayCreationExpression) {
                                    return getMatchingOption((ArrayCreationExpression) arrayCreationExpression, (StringLiteralExpression) psiElement);
                                }
                            }
                        }

                    }

                    // on array creation key dont have value, so provide completion here also
                    // array('foo' => 'bar', '<test>')
                    if(PhpPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).accepts(psiElement.getContext())) {
                        PsiElement arrayKey = psiElement.getContext();
                        if(arrayKey != null) {
                            PsiElement arrayCreationExpression = arrayKey.getContext();
                            if(arrayCreationExpression instanceof ArrayCreationExpression) {
                                return getMatchingOption((ArrayCreationExpression) arrayCreationExpression, (StringLiteralExpression) psiElement);
                            }

                        }

                    }

                    return new PsiReference[0];
                }

                private PsiReference[] getMatchingOption(ArrayCreationExpression arrayCreationExpression, StringLiteralExpression psiElement) {

                    if(PsiElementUtils.getParameterIndexValue(arrayCreationExpression) != 2) {
                        return new PsiReference[0];
                    }

                    PsiElement parameterList = arrayCreationExpression.getContext();

                    // unknown formtype so provide form fallback
                    if(!(parameterList instanceof ParameterList)) {
                        // unknown formtype so provide form fallback
                        return new PsiReference[]{ new FormExtensionKeyReference(psiElement, "form") };
                    }


                    // form name can be a string alias
                    String formTypeName = PsiElementUtils.getMethodParameterAt(((ParameterList) arrayCreationExpression.getContext()), 1);

                    // formtype is not a string, so try to find any php class types
                    if(formTypeName == null) {
                        PsiElement psiElement1 = PsiElementUtils.getMethodParameterPsiElementAt(((ParameterList) arrayCreationExpression.getContext()), 1);
                        if(psiElement1 instanceof PhpTypedElementImpl) {
                            formTypeName = ((PhpTypedElementImpl) psiElement1).getType().toString();
                        }
                    }

                    return new PsiReference[]{
                        new FormExtensionKeyReference(psiElement, "form", formTypeName),
                        new FormDefaultOptionsKeyReference(psiElement, formTypeName)
                    };

                }

            }

        );

    }



}
