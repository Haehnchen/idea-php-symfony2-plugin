package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationCompletionProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationCompletionProviderParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProviderLookupArguments;
import fr.adrienbrault.idea.symfony2plugin.form.gotoCompletion.TranslationGotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * Registers validators-domain completion and navigation for Symfony Constraint messages.
 *
 * <pre>
 * class FooConstraint extends \Symfony\Component\Validator\Constraint
 * {
 *     public $message = '<caret>';
 *     public $missingValueMessage = '<caret>';
 * }
 * </pre>
 *
 * {@code @Assert\NotBlank(message="<caret>")}
 * {@code @Assert\Length(minMessage="<caret>", maxMessage="<caret>")}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConstraintMessageGotoCompletionRegistrar implements GotoCompletionRegistrar {
    private static final String CONSTRAINT_CLASS = "\\Symfony\\Component\\Validator\\Constraint";

    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        /*
         * class FooConstraint extends \Symfony\Component\Validator\Constraint
         * {
         *     public $message = '<caret>';
         *     public $missingValueMessage = '<caret>';
         * }
         */
        registrar.register(
            getConstraintPropertyMessagePattern(),
            psiElement -> {
                PsiElement parent = psiElement.getParent();

                if (parent instanceof StringLiteralExpression && TranslationUtil.isConstraintPropertyField((StringLiteralExpression) parent)) {
                    return new TranslationGotoCompletionProvider(psiElement, "validators");
                }

                return null;
            }
        );

        /*
         * @Assert\Email(message="<caret>")
         * @Assert\Length(minMessage="<caret>", maxMessage="<caret>")
         */
        registrar.register(
            getConstraintAnnotationMessagePattern(),
            psiElement -> {
                PsiElement parent = psiElement.getParent();

                if (parent instanceof StringLiteralExpression && isConstraintMessageStringLiteral((StringLiteralExpression) parent)) {
                    return new TranslationGotoCompletionProvider(psiElement, "validators");
                }

                return null;
            }
        );

        /*
         * #[NotBlank(['message' => '<caret>'])]
         * #[Length(['minMessage' => '<caret>', 'maxMessage' => '<caret>'])]
         */
        registrar.register(
            getConstraintAttributeOptionsMessagePattern(),
            psiElement -> {
                PsiElement parent = psiElement.getParent();

                if (parent instanceof StringLiteralExpression && isConstraintAttributeOptionsMessage((StringLiteralExpression) parent)) {
                    return new TranslationGotoCompletionProvider(psiElement, "validators") {
                        @NotNull
                        @Override
                        public Collection<LookupElement> getLookupElements() {
                            return Collections.emptyList();
                        }

                        @Override
                        public void getLookupElements(@NotNull GotoCompletionProviderLookupArguments arguments) {
                            String prefix = PsiElementUtils.removeIdeaRuleHack(((StringLiteralExpression) parent).getContents());
                            arguments.getResultSet()
                                .withPrefixMatcher(prefix)
                                .addAllElements(TranslationUtil.getTranslationLookupElementsOnDomain(psiElement.getProject(), "validators"));
                        }
                    };
                }

                return null;
            }
        );
    }

    /**
     * Matches string values assigned to Constraint message fields.
     *
     * <pre>
     * class FooConstraint extends \Symfony\Component\Validator\Constraint
     * {
     *     public $message = '<caret>';
     *     public $missingValueMessage = '<caret>';
     * }
     * </pre>
     */
    @NotNull
    public static PsiElementPattern.Capture<PsiElement> getConstraintPropertyMessagePattern() {
        return PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
            PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE,
            PhpTokenTypes.STRING_LITERAL
        )).withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)
            .withParent(PlatformPatterns.psiElement(Field.class)
                .withName(PlatformPatterns.or(
                    PlatformPatterns.string().startsWith("message"),
                    PlatformPatterns.string().endsWith("Message")
                ))
            ));
    }

    @NotNull
    private static PsiElementPattern.Capture<PsiElement> getConstraintAnnotationMessagePattern() {
        return PlatformPatterns.psiElement(PhpDocTokenTypes.DOC_STRING)
            .withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)
                .withParent(PlatformPatterns.psiElement(PhpDocElementTypes.phpDocAttributeList)
                    .withParent(PlatformPatterns.psiElement(PhpDocTag.class))
                )
            );
    }

    /**
     * Matches deprecated Constraint attribute option arrays.
     *
     * <pre>
     * #[NotBlank(['message' => '<caret>'])]
     * #[Length(['minMessage' => '<caret>', 'maxMessage' => '<caret>'])]
     * </pre>
     */
    @NotNull
    private static PsiElementPattern.Capture<PsiElement> getConstraintAttributeOptionsMessagePattern() {
        return PlatformPatterns.psiElement().withElementType(PlatformPatterns.elementType().or(
            PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE,
            PhpTokenTypes.STRING_LITERAL
        )).withParent(PlatformPatterns.psiElement(StringLiteralExpression.class)
            .withParent(PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE)
                .withParent(PlatformPatterns.psiElement(ArrayHashElement.class)
                    .withParent(PlatformPatterns.psiElement(ArrayCreationExpression.class)
                        .withParent(PlatformPatterns.psiElement(ParameterList.class)
                            .withParent(PlatformPatterns.psiElement(PhpAttribute.class))
                        )
                    )
                )
            ));
    }

    private static boolean isConstraintMessageProperty(@NotNull AnnotationPropertyParameter parameter) {
        PhpClass phpClass = parameter.getPhpClass();

        return Symfony2ProjectComponent.isEnabled(parameter.getProject()) && PhpElementsUtil.isInstanceOf(phpClass, CONSTRAINT_CLASS) && isMessagePropertyName(parameter.getPropertyName());
    }

    private static boolean isConstraintMessageStringLiteral(@NotNull StringLiteralExpression stringLiteral) {
        if (!Symfony2ProjectComponent.isEnabled(stringLiteral)) {
            return false;
        }

        PhpDocTag phpDocTag = PsiTreeUtil.getParentOfType(stringLiteral, PhpDocTag.class);
        if (phpDocTag == null) {
            return false;
        }

        PhpClass phpClass = AnnotationUtil.getAnnotationReference(phpDocTag);
        return phpClass != null
            && PhpElementsUtil.isInstanceOf(phpClass, CONSTRAINT_CLASS)
            && isMessagePropertyName(getPropertyName(stringLiteral));
    }

    private static boolean isConstraintAttributeOptionsMessage(@NotNull StringLiteralExpression stringLiteral) {
        if (!Symfony2ProjectComponent.isEnabled(stringLiteral)) {
            return false;
        }

        PsiElement arrayValue = stringLiteral.getParent();
        if (arrayValue == null || !PhpElementTypes.ARRAY_VALUE.equals(arrayValue.getNode().getElementType())) {
            return false;
        }

        PsiElement arrayHashParent = arrayValue.getParent();
        if (!(arrayHashParent instanceof ArrayHashElement arrayHashElement)) {
            return false;
        }

        PsiElement arrayCreationParent = arrayHashElement.getParent();
        if (!(arrayCreationParent instanceof ArrayCreationExpression arrayCreation)) {
            return false;
        }

        PsiElement parameterListParent = arrayCreation.getParent();
        if (!(parameterListParent instanceof ParameterList parameterList)) {
            return false;
        }

        PsiElement[] parameters = parameterList.getParameters();
        if (parameters.length == 0 || !parameters[0].equals(arrayCreation)) {
            return false;
        }

        PsiElement attributeParent = parameterList.getParent();
        if (!(attributeParent instanceof PhpAttribute phpAttribute)) {
            return false;
        }

        String fqn = phpAttribute.getFQN();
        if (fqn == null || !PhpElementsUtil.isInstanceOf(stringLiteral.getProject(), fqn, CONSTRAINT_CLASS)) {
            return false;
        }

        PsiElement key = arrayHashElement.getKey();

        return key != null && isMessagePropertyName(PhpElementsUtil.getStringValue(key));
    }

    private static boolean isMessagePropertyName(@Nullable String propertyName) {
        return propertyName != null && (propertyName.startsWith("message") || propertyName.endsWith("Message"));
    }

    @Nullable
    private static String getPropertyName(@NotNull StringLiteralExpression stringLiteral) {
        PsiElement equalsElement = PsiTreeUtil.prevVisibleLeaf(stringLiteral);
        if (equalsElement == null || !"=".equals(equalsElement.getText())) {
            return null;
        }

        PsiElement propertyName = PsiTreeUtil.prevVisibleLeaf(equalsElement);
        return propertyName == null ? null : propertyName.getText();
    }

    /**
     * Annotation plugin entrypoint for Constraint message property completion.
     * {@code @Assert\NotBlank(message="<caret>")}
     * {@code @Assert\Length(minMessage="<caret>", maxMessage="<caret>")}
     */
    public static class ConstraintMessageAnnotationCompletionProvider implements PhpAnnotationCompletionProvider {
        @Override
        public void getPropertyValueCompletions(AnnotationPropertyParameter parameter, AnnotationCompletionProviderParameter annotationCompletionProviderParameter) {
            if (!isConstraintMessageProperty(parameter)) {
                return;
            }

            annotationCompletionProviderParameter
                .getResult()
                .addAllElements(TranslationUtil.getTranslationLookupElementsOnDomain(parameter.getProject(), "validators"));
        }
    }

}
