package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MethodMatcher {

    public static class CallToSignature {

        private final String instance;
        private final String method;

        public CallToSignature(String instance, String method) {
            this.instance = instance;
            this.method = method;
        }

        public String getInstance() {
            return instance;
        }

        public String getMethod() {
            return method;
        }
    }

    public static class MethodMatchParameter {

        final private CallToSignature signature;
        final private ParameterBag parameterBag;
        final private PsiElement[] parameters;
        final private MethodReference methodReference;

        public MethodMatchParameter(CallToSignature signature, ParameterBag parameterBag, PsiElement[] parameters, MethodReference methodReference) {
            this.signature = signature;
            this.parameterBag = parameterBag;
            this.parameters = parameters;
            this.methodReference = methodReference;
        }

        @Nullable
        public CallToSignature getSignature() {
            return signature;
        }

        public ParameterBag getParameterBag() {
            return this.parameterBag;
        }

        public PsiElement[] getParameters() {
            return parameters;
        }

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

            if (!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement.getContext() instanceof ParameterList)) {
                return null;
            }

            ParameterList parameterList = (ParameterList) psiElement.getContext();
            if (parameterList == null || !(parameterList.getContext() instanceof MethodReference)) {
                return null;
            }

            CallToSignature matchedMethodSignature = this.isCallTo((MethodReference) parameterList.getContext());
            if(matchedMethodSignature == null) {
                return null;
            }

            ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(this.psiElement);
            if(currentIndex == null || currentIndex.getIndex() != this.parameterIndex) {
                return null;
            }

            return new MethodMatchParameter(null, currentIndex, parameterList.getParameters(), (MethodReference) parameterList.getContext());
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
        public MethodMatchParameter match();
    }

    public abstract static class AbstractMethodParameterMatcher implements MethodParameterMatcherInterface {

        final protected List<CallToSignature> signatures;
        final protected int parameterIndex;
        final protected PsiElement psiElement;

        public AbstractMethodParameterMatcher(PsiElement psiElement, int parameterIndex) {
            this.signatures = new ArrayList<CallToSignature>();
            this.parameterIndex = parameterIndex;
            this.psiElement = psiElement;
        }

        public AbstractMethodParameterMatcher withSignature(String instance, String method) {
            this.signatures.add(new CallToSignature(instance, method));
            return this;
        }

        @Nullable
        protected CallToSignature isCallTo(MethodReference methodReference) {
            Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();

            for(CallToSignature signature: this.signatures) {
                if(interfacesUtil.isCallTo(methodReference, signature.getInstance(), signature.getMethod())) {
                    return signature;
                }
            }

            return null;
        }
    }
}
