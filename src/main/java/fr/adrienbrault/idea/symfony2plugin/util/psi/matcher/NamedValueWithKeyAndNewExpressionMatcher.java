package fr.adrienbrault.idea.symfony2plugin.util.psi.matcher;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NamedValueWithKeyAndNewExpressionMatcher {

    /**
     * new Foobar(
     *   route: '<caret>',
     * );
     */
    @NotNull
    public static PsiElementPattern.Capture<PsiElement> pattern() {
        return PhpPatterns.psiElement().withParent(
            PlatformPatterns.psiElement(StringLiteralExpression.class).afterLeafSkipping(PlatformPatterns.or(
                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                PlatformPatterns.psiElement(PhpTokenTypes.WHITE_SPACE),
                PlatformPatterns.psiElement(PhpTokenTypes.opCOLON)
            ), PlatformPatterns.psiElement(PhpTokenTypes.IDENTIFIER)).withParent(PlatformPatterns.psiElement(ParameterList.class))
        );
    }

    /**
     * new Foobar(
     *   route: '<caret>',
     * );
     */
    @Nullable
    public static Result match(@NotNull PsiElement psiElement, @NotNull Matcher matcher) {
        PsiElement parameterList = psiElement.getParent();
        if (parameterList instanceof ParameterList) {
            PsiElement newExpression = parameterList.getParent();
            if (newExpression instanceof NewExpression) {
                for (NewExpressionCall newExpressionCall : matcher.getNewExpressionCalls()) {
                    PsiElement colon = PsiTreeUtil.prevCodeLeaf(psiElement);
                    if (colon == null || colon.getNode().getElementType() != PhpTokenTypes.opCOLON) {
                        continue;
                    }

                    PsiElement identifier = PsiTreeUtil.prevCodeLeaf(colon);
                    if (identifier == null || identifier.getNode().getElementType() != PhpTokenTypes.IDENTIFIER || !newExpressionCall.getNamedArgument().equals(identifier.getText())) {
                        continue;
                    }

                    if (!PhpElementsUtil.isNewExpressionPhpClassWithInstance((NewExpression) newExpression, newExpressionCall.getClazz())) {
                        continue;
                    }

                    return new Result(newExpressionCall, (NewExpression) newExpression, identifier);
                }
            }
        }

        return null;
    }

    public static class Matcher {
        @NotNull
        private final NewExpressionCall[] classes;

        public Matcher(@NotNull String namedArgument, @NotNull String clazz) {
            this.classes = new NewExpressionCall[] { new NewExpressionCall(clazz, namedArgument)};
        }

        public Matcher(@NotNull NewExpressionCall... classes) {
            this.classes = classes;
        }

        @NotNull
        public NewExpressionCall[] getNewExpressionCalls() {
            return classes;
        }
    }

    public static class Result {
        @NotNull
        private final NewExpression newExpression;

        @NotNull
        private final NewExpressionCall expressionCall;

        @NotNull
        private final PsiElement arrayKey;

        public Result(@NotNull NewExpressionCall expressionCall, @NotNull NewExpression newExpression, @NotNull PsiElement arrayKey) {
            this.expressionCall = expressionCall;
            this.newExpression = newExpression;
            this.arrayKey = arrayKey;
        }

        @NotNull
        public NewExpression getNewExpression() {
            return newExpression;
        }

        @NotNull
        public NewExpressionCall getExpressionCall() {
            return expressionCall;
        }

        @NotNull
        public PsiElement getArrayKey() {
            return arrayKey;
        }
    }

    public static class NewExpressionCall {
        @NotNull
        private final String clazz;

        @NotNull
        private final String namedArgument;

        public NewExpressionCall(@NotNull String clazz, @NotNull String namedArgument) {
            this.clazz = clazz;
            this.namedArgument = namedArgument;
        }

        @NotNull
        public String getClazz() {
            return clazz;
        }

        @NotNull
        public String getNamedArgument() {
            return namedArgument;
        }
    }
}
