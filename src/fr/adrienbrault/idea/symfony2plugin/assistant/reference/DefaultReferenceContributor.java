package fr.adrienbrault.idea.symfony2plugin.assistant.reference;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

public class DefaultReferenceContributor {

    // @TODO: dynamic adding
    public static AssistantReferenceContributor[] DEFAULT_CONTRIBUTORS = new AssistantReferenceContributor[] {
        new ParameterAssistantReferenceProvider(),
        new AssistantReferenceProviderConfigArray(),
        new AssistantReferenceProviderArrayKey()
    };

    private static class ParameterAssistantReferenceProvider implements AssistantReferenceContributor {

        @Override
        public boolean supportData() {
            return false;
        }

        @Override
        public String getAlias() {
            return "parameter";
        }

        @Override
        public boolean isContributedElement(PsiElement psiElement, MethodParameterSetting config) {
            if(psiElement.getContext() instanceof ParameterList) {
                if(PsiElementUtils.getParameterIndexValue(psiElement) == config.getIndexParameter()) {
                    return true;
                }
            }

            return false;
        }
    }

    private static class AssistantReferenceProviderConfigArray implements AssistantReferenceContributor {

        @Override
        public boolean supportData() {
            return true;
        }

        @Override
        public String getAlias() {
            return "array_value";
        }

        public boolean isContributedElement(PsiElement psiElement, MethodParameterSetting config) {

            // value inside array
            // $menu->addChild(array(
            //   'route' => 'foo',
            // ));
            if(PhpPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).accepts(psiElement.getContext())) {
                PsiElement arrayValue = psiElement.getContext();
                if(arrayValue != null) {
                    PsiElement arrayHashElement = arrayValue.getContext();
                    if(arrayHashElement instanceof ArrayHashElement) {
                        PhpPsiElement arrayKey = ((ArrayHashElement) arrayHashElement).getKey();
                        if(arrayKey instanceof StringLiteralExpression && ((StringLiteralExpression) arrayKey).getContents().equals(config.getContributorData())) {
                            PsiElement arrayCreationExpression = arrayHashElement.getContext();
                            if(arrayCreationExpression instanceof ArrayCreationExpression) {
                                if(PsiElementUtils.getParameterIndexValue(arrayCreationExpression) == config.getIndexParameter()) {
                                    return true;
                                }
                            }
                        }

                    }
                }

            }

            return false;
        }

    }

    private static class AssistantReferenceProviderArrayKey implements AssistantReferenceContributor {

        public boolean isContributedElement(PsiElement psiElement, MethodParameterSetting config) {

            // value inside array
            // $menu->addChild(array(
            //   'foo' => '',
            // ));
            if(PhpPatterns.psiElement(PhpElementTypes.ARRAY_KEY).accepts(psiElement.getContext())) {
                PsiElement arrayKey = psiElement.getContext();
                if(arrayKey != null) {
                    PsiElement arrayHashElement = arrayKey.getContext();
                    if(arrayHashElement instanceof ArrayHashElement) {
                        PsiElement arrayCreationExpression = arrayHashElement.getContext();
                        if(arrayCreationExpression instanceof ArrayCreationExpression) {
                            if(PsiElementUtils.getParameterIndexValue(arrayCreationExpression) == config.getIndexParameter()) {
                                return true;
                            }
                        }

                    }
                }

            }

            return false;
        }

        @Override
        public boolean supportData() {
            return false;
        }

        @Override
        public String getAlias() {
            return "array_key";
        }

    }
}
