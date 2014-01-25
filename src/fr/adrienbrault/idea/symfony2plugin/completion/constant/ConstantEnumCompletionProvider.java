package fr.adrienbrault.idea.symfony2plugin.completion.constant;

import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;

public class ConstantEnumCompletionProvider {

    private MethodMatcher.CallToSignature callToSignature;
    private EnumConstantFilter enumConstantFilter;
    private EnumType enumType;

    public ConstantEnumCompletionProvider(MethodMatcher.CallToSignature callToSignature, EnumConstantFilter enumConstantFilter, EnumType enumType) {
        this.callToSignature = callToSignature;
        this.enumConstantFilter = enumConstantFilter;
        this.enumType = enumType;
    }
    public EnumConstantFilter getEnumConstantFilter() {
        return enumConstantFilter;
    }

    public MethodMatcher.CallToSignature getCallToSignature() {
        return callToSignature;
    }

    public EnumType getEnumType() {
        return enumType;
    }


    public enum EnumType {
        PARAMETER, RETURN
    }

}
