package fr.adrienbrault.idea.symfonyplugin.util.psi.matcher;

import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.patterns.PhpPatterns;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfonyplugin.util.dict.PhpMethodReferenceCall;
import fr.adrienbrault.idea.symfonyplugin.util.psi.PsiElementAssertUtil;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ArrayValueWithKeyAndMethodMatcher {

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
                if(arrayKey instanceof StringLiteralExpression && ArrayUtils.contains(matcher.getArrayKeys(), ((StringLiteralExpression) arrayKey).getContents())) {
                    PsiElement arrayCreationExpression = arrayHashElement.getParent();
                    if(arrayCreationExpression instanceof ArrayCreationExpression) {
                        Pair<PhpMethodReferenceCall, MethodReference> matchPair = matchesMethodCall(arrayCreationExpression, matcher.getMethodCalls());
                        if(matchPair != null) {
                            return new Result(matchPair.getFirst(), matchPair.getSecond(), (StringLiteralExpression) arrayKey);
                        }
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static Pair<PhpMethodReferenceCall, MethodReference> matchesMethodCall(@NotNull PsiElement psiFirstMethodReferenceChild, @NotNull PhpMethodReferenceCall[] methodCalls) {

        // get parameter, we less then 0 we are not inside method reference
        int parameterIndex = PsiElementUtils.getParameterIndexValue(psiFirstMethodReferenceChild);
        if(parameterIndex < 0) {
            return null;
        }

        PsiElement parameterList = psiFirstMethodReferenceChild.getParent();
        if(!(parameterList instanceof ParameterList)) {
            return null;
        }

        PsiElement methodReference = parameterList.getParent();
        if (!(methodReference instanceof MethodReference)) {
            return null;
        }

        for (PhpMethodReferenceCall methodCall : methodCalls) {
            if(parameterIndex != methodCall.getIndex() ||
                !ArrayUtils.contains(methodCall.getMethods(), ((MethodReference) methodReference).getName()) ||
                !PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, methodCall.getClassName())
                )
            {
                continue;
            }

            return Pair.create(methodCall, (MethodReference) methodReference);
        }

        return null;
    }

    public static class Matcher {

        private final String[] arrayKey;

        @NotNull
        private final PhpMethodReferenceCall[] methodCalls;

        public Matcher(@NotNull String arrayKeys, @NotNull PhpMethodReferenceCall... methodCalls) {
            this(new String[] {arrayKeys}, methodCalls);
        }

        public Matcher(@NotNull String[] arrayKeys, @NotNull PhpMethodReferenceCall... methodCalls) {
            this.arrayKey = arrayKeys;
            this.methodCalls = methodCalls;
        }

        public String[] getArrayKeys() {
            return arrayKey;
        }

        @NotNull
        public PhpMethodReferenceCall[] getMethodCalls() {
            return methodCalls;
        }
    }

    public static class Result {

        @NotNull
        private final PhpMethodReferenceCall methodCall;

        @NotNull
        private final StringLiteralExpression arrayKey;

        @NotNull
        private final MethodReference methodReference;

        public Result(@NotNull PhpMethodReferenceCall methodCall, @NotNull MethodReference methodReference, @NotNull StringLiteralExpression arrayKey) {
            this.methodCall = methodCall;
            this.methodReference = methodReference;
            this.arrayKey = arrayKey;
        }

        @NotNull
        public StringLiteralExpression getArrayKey() {
            return arrayKey;
        }

        @NotNull
        public MethodReference getMethodReference() {
            return methodReference;
        }

        @NotNull
        public PhpMethodReferenceCall getMethodCall() {
            return methodCall;
        }
    }
}
