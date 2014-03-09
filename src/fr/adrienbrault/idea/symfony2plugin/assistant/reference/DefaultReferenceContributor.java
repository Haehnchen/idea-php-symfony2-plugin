package fr.adrienbrault.idea.symfony2plugin.assistant.reference;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.form.FormTypeReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;

public class DefaultReferenceContributor {

    // @TODO: dynamic adding
    public static AssistantReferenceContributor[] DEFAULT_CONTRIBUTORS = new AssistantReferenceContributor[] {
        new ParameterAssistantReferenceProvider(),
        new AssistantReferenceProviderConfigArray(),
        new AssistantReferenceProviderArrayKey()
    };

    public enum DEFAULT_CONTRIBUTORS_ENUM {
        PARAMETER {
            public String toString() {
                return "parameter";
            }
        },
        ARRAY_VALUE {
            public String toString() {
                return "array_value";
            }
        },
        ARRAY_KEY {
            public String toString() {
                return "array_key";
            }
        }
    }

    private static class ParameterAssistantReferenceProvider implements AssistantReferenceContributor {

        @Override
        public boolean supportData() {
            return false;
        }

        @Override
        public String getAlias() {
            return DEFAULT_CONTRIBUTORS_ENUM.PARAMETER.toString();
        }

        @Override
        public boolean isContributedElement(PsiElement psiElement, MethodParameterSetting config) {
            return psiElement.getContext() instanceof ParameterList;
        }
    }

    public static class AssistantReferenceProviderConfigArray implements AssistantReferenceContributor {

        @Override
        public boolean supportData() {
            return true;
        }

        @Override
        public String getAlias() {
            return DEFAULT_CONTRIBUTORS_ENUM.ARRAY_VALUE.toString();
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
                                if(arrayCreationExpression.getParent() instanceof ParameterList) {
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
                            if(arrayCreationExpression.getParent() instanceof ParameterList) {
                                return true;
                            }
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
                        if(arrayCreationExpression.getParent() instanceof ParameterList) {
                            return true;
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
            return DEFAULT_CONTRIBUTORS_ENUM.ARRAY_KEY.toString();
        }

    }
}
