package fr.adrienbrault.idea.symfony2plugin.lang;

import com.intellij.lang.Language;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class LanguageInjection {
    @NotNull
    private final String languageId;
    @Nullable
    private final String prefix;
    @Nullable
    private final String suffix;
    @NotNull
    private final ElementPattern<? extends PsiElement> pattern;

    protected LanguageInjection(@NotNull String languageId, @Nullable String prefix, @Nullable String suffix, @NotNull ElementPattern<? extends PsiElement> pattern) {
        this.languageId = languageId;
        this.prefix = prefix;
        this.suffix = suffix;
        this.pattern = pattern;
    }

    @Nullable
    public Language getLanguage() {
        return Language.findLanguageByID(languageId);
    }

    @Nullable
    public String getPrefix() {
        return prefix;
    }

    @Nullable
    public String getSuffix() {
        return suffix;
    }

    @NotNull
    public ElementPattern<? extends PsiElement> getPattern() {
        return pattern;
    }

    public static class Builder {
        @NotNull
        private final String languageId;
        @NotNull
        private final List<ElementPattern<? extends PsiElement>> patterns;
        @Nullable
        private String prefix;
        @Nullable
        private String suffix;

        public Builder(@NotNull String languageId) {
            this.languageId = languageId;
            this.patterns = new ArrayList<>();
        }

        public Builder withPrefix(@Nullable String prefix) {
            this.prefix = prefix;

            return this;
        }

        public Builder withSuffix(@Nullable String suffix) {
            this.suffix = suffix;

            return this;
        }

        public final Builder matchingPattern(@NotNull ElementPattern<? extends PsiElement> pattern) {
            patterns.add(pattern);
            return this;
        }

        public Builder matchingAttributeArgument(@NotNull String classFQN, @NotNull String argumentName, int argumentIndex) {
            return matchingPattern(LanguageInjectionPatterns.getAttributeArgumentPattern(classFQN, argumentName, argumentIndex));
        }

        public Builder matchingAnnotationProperty(@NotNull String classFQN, @NotNull String propertyName, boolean isDefaultProperty) {
            return matchingPattern(LanguageInjectionPatterns.getAnnotationPropertyPattern(classFQN, propertyName, isDefaultProperty));
        }

        public Builder matchingConstructorCallArgument(@NotNull String classFQN, @NotNull String argumentName, int argumentIndex) {
            return matchingPattern(LanguageInjectionPatterns.getConstructorCallArgumentPattern(classFQN, argumentName, argumentIndex));
        }

        public Builder matchingFunctionCallArgument(@NotNull String functionFQN, @NotNull String argumentName, int argumentIndex) {
            return matchingPattern(LanguageInjectionPatterns.getFunctionCallArgumentPattern(functionFQN, argumentName, argumentIndex));
        }

        public Builder matchingMethodCallArgument(@NotNull String classFQN, @NotNull String methodName, @NotNull String argumentName, int argumentIndex) {
            return matchingPattern(LanguageInjectionPatterns.getMethodCallArgumentPattern(classFQN, methodName, argumentName, argumentIndex));
        }

        public LanguageInjection build() {
            return new LanguageInjection(languageId, prefix, suffix, StandardPatterns.or(patterns.toArray(new ElementPattern[0])));
        }
    }
}
