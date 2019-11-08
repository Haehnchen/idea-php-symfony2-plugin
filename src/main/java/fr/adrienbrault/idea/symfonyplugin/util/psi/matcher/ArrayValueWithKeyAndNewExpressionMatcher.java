package fr.adrienbrault.idea.symfonyplugin.util.psi.matcher;

import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfonyplugin.util.psi.PsiElementAssertUtil;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ArrayValueWithKeyAndNewExpressionMatcher {

    /**
     * $menu->addChild([
     *   'route' => '<caret>',
     * ]);
     */
    @NotNull
    public static PsiElementPattern.Capture<PsiElement> pattern() {
        return PhpPatterns.psiElement().withParent(
            PlatformPatterns.psiElement(StringLiteralExpression.class).withParent(
                PlatformPatterns.psiElement(PhpElementTypes.ARRAY_VALUE).inside(ParameterList.class)
            )
        );
    }

    /**
     * A array value inside array and method reference
     *
     * $menu->addChild([
     *   'route' => '<caret>',
     * ]);
     */
    @Nullable
    public static Result match(@NotNull PsiElement psiElement, @NotNull Matcher matcher) {
        PsiElement arrayValue = psiElement.getParent();
        if(PsiElementAssertUtil.isNotNullAndIsElementType(arrayValue, PhpElementTypes.ARRAY_VALUE)) {
            PsiElement arrayHashElement = arrayValue.getParent();
            if(arrayHashElement instanceof ArrayHashElement) {
                PhpPsiElement arrayKey = ((ArrayHashElement) arrayHashElement).getKey();
                if(arrayKey != null && ArrayUtils.contains(matcher.getArrayKeys(), PhpElementsUtil.getStringValue(arrayKey))) {
                    PsiElement arrayCreationExpression = arrayHashElement.getParent();
                    if(arrayCreationExpression instanceof ArrayCreationExpression) {
                        Pair<NewExpressionCall, NewExpression> matchPair = matchesMethodCall((ArrayCreationExpression) arrayCreationExpression, matcher.getNewExpressionCalls());
                        if(matchPair != null) {
                            return new Result(matchPair.getFirst(), matchPair.getSecond(), arrayKey);
                        }
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static Pair<NewExpressionCall, NewExpression> matchesMethodCall(@NotNull ArrayCreationExpression arrayCreationExpression, @NotNull NewExpressionCall[] classes) {

        // get parameter, we less then 0 we are not inside method reference
        int parameterIndex = PsiElementUtils.getParameterIndexValue(arrayCreationExpression);
        if(parameterIndex < 0) {
            return null;
        }

        // filter index
        List<NewExpressionCall> filter = ContainerUtil.filter(classes, expressionCall ->
            expressionCall.getIndex() == parameterIndex
        );

        if(filter.size() == 0) {
            return null;
        }

        PsiElement parameterList = arrayCreationExpression.getParent();
        if(!(parameterList instanceof ParameterList)) {
            return null;
        }

        PsiElement newExpression = parameterList.getParent();
        if (!(newExpression instanceof NewExpression)) {
            return null;
        }

        for (NewExpressionCall aClass : filter) {
            if(PhpElementsUtil.getNewExpressionPhpClassWithInstance((NewExpression) newExpression, aClass.getClazz()) == null) {
                continue;
            }

            return Pair.create(aClass, (NewExpression) newExpression);
        }

        return null;
    }

    public static class Matcher {
        @NotNull
        private final String[] arrayKeys;

        @NotNull
        private final NewExpressionCall[] classes;

        public Matcher(@NotNull String[] arrayKeys, @NotNull NewExpressionCall... classes) {
            this.arrayKeys = arrayKeys;
            this.classes = classes;
        }

        public Matcher(@NotNull String arrayKeys, @NotNull NewExpressionCall... classes) {
            this(new String[] {arrayKeys}, classes);
        }

        public Matcher(@NotNull String arrayKeys, @NotNull String clazz) {
            this(new String[] {arrayKeys}, new NewExpressionCall(clazz, 0));
        }

        @NotNull
        public String[] getArrayKeys() {
            return arrayKeys;
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

        private int index = 0;

        public NewExpressionCall(@NotNull String clazz) {
            this(clazz, 0);
        }

        public NewExpressionCall(@NotNull String clazz, int index) {
            this.clazz = clazz;
            this.index = index;
        }

        @NotNull
        public String getClazz() {
            return clazz;
        }

        public int getIndex() {
            return index;
        }
    }
}
