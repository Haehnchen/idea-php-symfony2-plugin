package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.MethodReferenceBag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodMatcher {

    @Nullable
    public static MethodMatcher.MethodMatchParameter getMatchedSignatureWithDepth(PsiElement psiElement, MethodMatcher.CallToSignature[] callToSignatures) {
        return getMatchedSignatureWithDepth(psiElement, callToSignatures, 0);
    }

    @Nullable
    public static MethodMatcher.MethodMatchParameter getMatchedSignatureWithDepth(PsiElement psiElement, MethodMatcher.CallToSignature[] callToSignatures, int defaultParameterIndex) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(psiElement, defaultParameterIndex)
            .withSignature(callToSignatures)
            .match();

        if(methodMatchParameter != null) {
            return methodMatchParameter;
        }

        // try on resolved method
        return new MethodMatcher.StringParameterRecursiveMatcher(psiElement, defaultParameterIndex)
            .withSignature(callToSignatures)
            .match();
    }

    public static class CallToSignature {
        @NotNull
        private final String instance;

        @NotNull
        private final String method;

        public CallToSignature(@NotNull String instance, @NotNull String method) {
            this.instance = instance;
            this.method = method;
        }

        @NotNull
        public String getInstance() {
            return instance;
        }

        @NotNull
        public String getMethod() {
            return method;
        }
    }

    public static class MethodMatchParameter {
        @NotNull
        final private CallToSignature signature;

        @NotNull
        final private ParameterBag parameterBag;

        @NotNull
        final private PsiElement[] parameters;

        @NotNull
        final private MethodReference methodReference;

        public MethodMatchParameter(@NotNull CallToSignature signature, @NotNull ParameterBag parameterBag, @NotNull PsiElement[] parameters, @NotNull MethodReference methodReference) {
            this.signature = signature;
            this.parameterBag = parameterBag;
            this.parameters = parameters;
            this.methodReference = methodReference;
        }

        @NotNull
        public CallToSignature getSignature() {
            return signature;
        }

        @NotNull
        public ParameterBag getParameterBag() {
            return this.parameterBag;
        }

        @NotNull
        public PsiElement[] getParameters() {
            return parameters;
        }

        @NotNull
        public MethodReference getMethodReference() {
            return methodReference;
        }

    }

    public static class StringParameterMatcher extends AbstractMethodParameterMatcher {

        public StringParameterMatcher(PsiElement psiElement, int parameterIndex) {
            super(psiElement, parameterIndex);
        }

        @Nullable
        public MethodMatchParameter match() {

            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            MethodReferenceBag bag = PhpElementsUtil.getMethodParameterReferenceBag(psiElement, this.parameterIndex);
            if(bag == null) {
                return null;
            }

            CallToSignature matchedMethodSignature = this.isCallTo(bag.getMethodReference());
            if(matchedMethodSignature == null) {
                return null;
            }

            return new MethodMatchParameter(matchedMethodSignature, bag.getParameterBag(), bag.getParameterList().getParameters(), bag.getMethodReference());
        }

    }

    public static class StringParameterAnyMatcher extends AbstractMethodParameterMatcher {

        public StringParameterAnyMatcher(PsiElement psiElement) {
            super(psiElement, -1);
        }

        @Nullable
        public MethodMatchParameter match() {

            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }


            MethodReferenceBag bag = PhpElementsUtil.getMethodParameterReferenceBag(psiElement);
            if(bag == null) {
                return null;
            }

            CallToSignature matchedMethodSignature = this.isCallTo(bag.getMethodReference());
            if(matchedMethodSignature == null) {
                return null;
            }

            return new MethodMatchParameter(matchedMethodSignature, bag.getParameterBag(), bag.getParameterList().getParameters(), bag.getMethodReference());
        }

    }

    public static class StringParameterRecursiveMatcher extends AbstractMethodParameterMatcher {

        public StringParameterRecursiveMatcher(PsiElement psiElement, int parameterIndex) {
            super(psiElement, parameterIndex);
        }

        @Nullable
        public MethodMatchParameter match() {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            MethodReferenceBag bag = PhpElementsUtil.getMethodParameterReferenceBag(psiElement);
            if(bag == null) {
                return null;
            }

            // try on current method
            MethodMatcher.MethodMatchParameter methodMatchParameter = new StringParameterMatcher(psiElement, parameterIndex)
                .withSignature(this.signatures)
                .match();

            if(methodMatchParameter != null) {
                return methodMatchParameter;
            }

            // walk down next method
            MethodReference methodReference = bag.getMethodReference();
            for (Method method : PhpElementsUtil.getMultiResolvedMethod(methodReference)) {
                for(PsiElement var: PhpElementsUtil.getMethodParameterReferences(method, bag.getParameterBag().getIndex())) {

                    MethodMatcher.MethodMatchParameter methodMatchParameterRef = new MethodMatcher.StringParameterMatcher(var, parameterIndex)
                        .withSignature(this.signatures)
                        .match();

                    if(methodMatchParameterRef != null) {
                        return methodMatchParameterRef;
                    }
                }
            }

            return null;
        }
    }

    public static class ArrayParameterMatcher extends AbstractMethodParameterMatcher {

        public ArrayParameterMatcher(PsiElement psiElement, int parameterIndex) {
            super(psiElement, parameterIndex);
        }

        @Nullable
        public MethodMatchParameter match() {

            if (!Symfony2ProjectComponent.isEnabled(this.psiElement)) {
                return null;
            }

            ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(this.psiElement);
            if (arrayCreationExpression == null) {
                return null;
            }

            PsiElement parameterList = arrayCreationExpression.getContext();
            if (!(parameterList instanceof ParameterList)) {
                return null;
            }

            PsiElement methodParameters[] = ((ParameterList) parameterList).getParameters();
            if (methodParameters.length < this.parameterIndex) {
                return null;
            }

            if (!(parameterList.getContext() instanceof MethodReference)) {
                return null;
            }
            MethodReference methodReference = (MethodReference) parameterList.getContext();

            ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayCreationExpression);
            if (currentIndex == null || currentIndex.getIndex() != this.parameterIndex) {
                return null;
            }

            CallToSignature matchedMethodSignature = this.isCallTo(methodReference);
            if(matchedMethodSignature == null) {
                return null;
            }

            return new MethodMatchParameter(matchedMethodSignature, currentIndex, methodParameters, methodReference);
        }

    }

    public interface MethodParameterMatcherInterface {
        @Nullable
        MethodMatchParameter match();
    }

    public abstract static class AbstractMethodParameterMatcher implements MethodParameterMatcherInterface {

        final protected List<CallToSignature> signatures;
        final protected int parameterIndex;
        final protected PsiElement psiElement;

        public AbstractMethodParameterMatcher(PsiElement psiElement, int parameterIndex) {
            this.signatures = new ArrayList<>();
            this.parameterIndex = parameterIndex;
            this.psiElement = psiElement;
        }

        public AbstractMethodParameterMatcher withSignature(String instance, String method) {
            this.signatures.add(new CallToSignature(instance, method));
            return this;
        }

        public AbstractMethodParameterMatcher withSignature(Collection<CallToSignature> signatures) {
            this.signatures.addAll(signatures);
            return this;
        }

        public AbstractMethodParameterMatcher withSignature(CallToSignature[] callToSignatures) {
            this.signatures.addAll(Arrays.asList(callToSignatures));
            return this;
        }

        @Nullable
        protected CallToSignature isCallTo(MethodReference methodReference) {
            for(CallToSignature signature: this.signatures) {
                if(PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, signature)) {
                    return signature;
                }
            }

            return null;
        }
    }
}
