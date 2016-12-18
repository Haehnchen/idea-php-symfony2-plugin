package fr.adrienbrault.idea.symfony2plugin.completion;


import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.completion.constant.ConstantEnumCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.completion.constant.EnumConstantFilter;
import fr.adrienbrault.idea.symfony2plugin.completion.lookup.PhpConstantFieldPhpLookupElement;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

public class ConstantEnumCompletionContributor extends CompletionContributor {


    private static String[] HTTP_METHODS = new String[] {"POST", "GET", "HEAD", "DELETE", "PATCH", "PUT", "post", "get", "head", "delete", "patch", "put"};

    public static ConstantEnumCompletionProvider[] CONSTANTS_ENUMS = new ConstantEnumCompletionProvider[] {
        new ConstantEnumCompletionProvider(
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\HttpFoundation\\Response", "setStatusCode"),
            new EnumConstantFilter("\\Symfony\\Component\\HttpFoundation\\Response", "HTTP_"),
            ConstantEnumCompletionProvider.EnumType.PARAMETER
        ),
        new ConstantEnumCompletionProvider(
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\HttpFoundation\\Response", "getStatusCode"),
            new EnumConstantFilter("\\Symfony\\Component\\HttpFoundation\\Response", "HTTP_"),
            ConstantEnumCompletionProvider.EnumType.RETURN
        ),
        new ConstantEnumCompletionProvider(
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\HttpFoundation\\Request", "getMethod"),
            new EnumConstantFilter(HTTP_METHODS),
            ConstantEnumCompletionProvider.EnumType.RETURN
        ),
    };

    public ConstantEnumCompletionContributor() {

        // fields

        // $this->method(TEST:HTTP);
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement psiElement = completionParameters.getOriginalPosition();
                if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getParent() instanceof MethodReference)) {
                    return;
                }

                Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
                for(ConstantEnumCompletionProvider enumProvider: CONSTANTS_ENUMS) {

                    if(enumProvider.getEnumType() == ConstantEnumCompletionProvider.EnumType.PARAMETER) {
                        attachLookup(completionResultSet, (MethodReference) psiElement.getParent(), symfony2InterfacesUtil, enumProvider);
                    }

                }

            }

        });

        // $test = TEST::HTTP
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                BinaryExpression binaryExpression = PsiTreeUtil.getPrevSiblingOfType(psiElement, BinaryExpression.class);
                if(binaryExpression == null) {
                    return;
                }

                // OK: $response->getStatusCode() == Response::HTTP_BAD_GATEWAY
                // @TODO: error we are complete outside of context: $response->getStatusCode() == Response::HTTP_BAD_GATEWAY || $response->getStatusCode() ==
                PsiElement leftOperand = binaryExpression.getLeftOperand();
                if(!(leftOperand instanceof MethodReference)) {
                    return;
                }

                Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
                for(ConstantEnumCompletionProvider enumProvider: CONSTANTS_ENUMS) {
                    if(enumProvider.getEnumType() == ConstantEnumCompletionProvider.EnumType.RETURN) {
                        attachLookup(completionResultSet, (MethodReference) leftOperand, symfony2InterfacesUtil, enumProvider);
                    }
                }

            }

        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getOriginalPosition();
                if(psiElement == null|| !Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                BinaryExpression binaryExpression = PsiTreeUtil.getPrevSiblingOfType(psiElement, BinaryExpression.class);
                if(binaryExpression == null) {
                    return;
                }

                // OK: $response->getStatusCode() == Response::HTTP_BAD_GATEWAY
                // @TODO: error we are complete outside of context: $response->getStatusCode() == Response::HTTP_BAD_GATEWAY || $response->getStatusCode() ==
                PsiElement leftOperand = binaryExpression.getLeftOperand();
                if(!(leftOperand instanceof MethodReference)) {
                    return;
                }

                Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
                for(ConstantEnumCompletionProvider enumProvider: CONSTANTS_ENUMS) {
                    if(enumProvider.getEnumType() == ConstantEnumCompletionProvider.EnumType.RETURN) {
                        attachLookup(completionResultSet, (MethodReference) leftOperand, symfony2InterfacesUtil, enumProvider);
                    }
                }

            }

        });


        // strings

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getPosition().getOriginalElement();
                if(!(psiElement.getParent() instanceof StringLiteralExpression) || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                for(ConstantEnumCompletionProvider enumProvider: CONSTANTS_ENUMS) {
                    if(enumProvider.getEnumType() == ConstantEnumCompletionProvider.EnumType.PARAMETER && enumProvider.getEnumConstantFilter().getStringValues() != null) {
                        if(MethodMatcher.getMatchedSignatureWithDepth(psiElement.getParent(), new MethodMatcher.CallToSignature[] {enumProvider.getCallToSignature()}) != null) {
                            for(String stringValue: enumProvider.getEnumConstantFilter().getStringValues()) {
                                completionResultSet.addElement(LookupElementBuilder.create(stringValue));
                            }
                        }
                    }

                }
            }

        });

        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                PsiElement psiElement = completionParameters.getPosition().getOriginalElement();
                if(!(psiElement.getParent() instanceof StringLiteralExpression) || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                if(!(psiElement.getParent().getParent() instanceof BinaryExpression)) {
                    return;
                }

                BinaryExpression binaryExpression = (BinaryExpression) psiElement.getParent().getParent();


                // OK: $response->getStatusCode() == Response::HTTP_BAD_GATEWAY
                // @TODO: error we are complete outside of context: $response->getStatusCode() == Response::HTTP_BAD_GATEWAY || $response->getStatusCode() ==
                PsiElement leftOperand = binaryExpression.getLeftOperand();
                if(!(leftOperand instanceof MethodReference)) {
                    return;
                }

                Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
                for(ConstantEnumCompletionProvider enumProvider: CONSTANTS_ENUMS) {
                    if(enumProvider.getEnumType() == ConstantEnumCompletionProvider.EnumType.RETURN && enumProvider.getEnumConstantFilter().getStringValues() != null && symfony2InterfacesUtil.isCallTo((MethodReference) leftOperand, enumProvider.getCallToSignature().getInstance(), enumProvider.getCallToSignature().getMethod())) {
                        for(String stringValue: enumProvider.getEnumConstantFilter().getStringValues()) {
                            completionResultSet.addElement(LookupElementBuilder.create(stringValue));
                        }
                    }
                }

            }

        });

    }

    private void attachLookup(CompletionResultSet completionResultSet, MethodReference psiElement, Symfony2InterfacesUtil symfony2InterfacesUtil, ConstantEnumCompletionProvider enumProvider) {

        // we allow string values
        if(enumProvider.getEnumConstantFilter().getInstance() == null || enumProvider.getEnumConstantFilter().getField() == null) {
            return;
        }

        if(!symfony2InterfacesUtil.isCallTo(psiElement, enumProvider.getCallToSignature().getInstance(), enumProvider.getCallToSignature().getMethod())) {
            return;
        }

        PhpClass phpClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), enumProvider.getEnumConstantFilter().getInstance());
        if(phpClass == null) {
            return;
        }

        for(Field field: phpClass.getFields()) {
            if(field.isConstant() && field.getName().startsWith(enumProvider.getEnumConstantFilter().getField())) {
                completionResultSet.addElement(new PhpConstantFieldPhpLookupElement(field));
            }
        }

    }

}
