package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.expressionLanguage.ExpressionLanguage;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLPsiElement;
import org.jetbrains.yaml.psi.YAMLQuotedText;

import java.util.Collections;
import java.util.List;

public class YamlLanguageInjector implements MultiHostInjector {

    private static final String EXPRESSION_LANGUAGE_PREFIX = "@=";

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
        if (!Symfony2ProjectComponent.isEnabled(context.getProject())) {
            return;
        }

        if (!(context instanceof YAMLQuotedText)) {
            return;
        }

        var file = context.getContainingFile();
        var element = (YAMLQuotedText) context;
        var value = element.getTextValue();

        if (YamlHelper.isServicesFile(file) && isExpressionLanguageString(value) && isExpressionLanguageStringAllowed(element)) {
            registrar
                .startInjecting(ExpressionLanguage.INSTANCE)
                .addPlace(null, null, element, getExpressionLanguageTextRange(value))
                .doneInjecting();
        } else if (YamlHelper.isRoutingFile(file) && isInsideRouteConditionKey(element)) {
            registrar
                .startInjecting(ExpressionLanguage.INSTANCE)
                .addPlace(null, null, element, ElementManipulators.getValueTextRange(element))
                .doneInjecting();
        }
    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(YAMLQuotedText.class);
    }

    private boolean isExpressionLanguageString(@NotNull String str) {
        return str.startsWith(EXPRESSION_LANGUAGE_PREFIX) && str.length() > EXPRESSION_LANGUAGE_PREFIX.length();
    }

    private boolean isExpressionLanguageStringAllowed(@NotNull YAMLPsiElement element) {
        return PlatformPatterns.and(
            YamlElementPatternHelper.getInsideKeyValue("services"),
            YamlElementPatternHelper.getInsideKeyValue("arguments", "properties", "calls", "configurator")
        ).accepts(element);
    }

    @NotNull
    private TextRange getExpressionLanguageTextRange(@NotNull String str) {
        return new TextRange(EXPRESSION_LANGUAGE_PREFIX.length() + 1, str.length() + 1);
    }

    private boolean isInsideRouteConditionKey(@NotNull YAMLPsiElement element) {
        return YamlElementPatternHelper.getInsideKeyValue("condition").accepts(element);
    }
}
