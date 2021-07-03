package fr.adrienbrault.idea.symfony2plugin.lang;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StringLiteralLanguageInjector implements MultiHostInjector {

    public static final String LANGUAGE_ID_CSS = "CSS";
    public static final String LANGUAGE_ID_XPATH = "XPath";
    public static final String LANGUAGE_ID_JSON = "JSON";
    public static final String LANGUAGE_ID_DQL = "DQL";
    public static final String LANGUAGE_ID_EXPRESSION_LANGUAGE = "Symfony Expression Language";

    private final LanguageInjection[] LANGUAGE_INJECTIONS = {
        new LanguageInjection.Builder(LANGUAGE_ID_CSS)
            .withPrefix("@media all { ")
            .withSuffix(" }")
            .matchingMethodCallArgument("\\Symfony\\Component\\DomCrawler\\Crawler", "filter", "selector", 0)
            .matchingMethodCallArgument("\\Symfony\\Component\\DomCrawler\\Crawler", "children", "selector", 0)
            .matchingMethodCallArgument("\\Symfony\\Component\\CssSelector\\CssSelectorConverter", "toXPath", "cssExpr", 0)
            .build(),
        new LanguageInjection.Builder(LANGUAGE_ID_XPATH)
            .matchingMethodCallArgument("\\Symfony\\Component\\DomCrawler\\Crawler", "filterXPath", "xpath", 0)
            .matchingMethodCallArgument("\\Symfony\\Component\\DomCrawler\\Crawler", "evaluate", "xpath", 0)
            .build(),
        new LanguageInjection.Builder(LANGUAGE_ID_JSON)
            .matchingMethodCallArgument("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "fromJsonString", "data", 0)
            .matchingMethodCallArgument("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "setJson", "json", 0)
            .build(),
        new LanguageInjection.Builder(LANGUAGE_ID_DQL)
            .matchingMethodCallArgument("\\Doctrine\\ORM\\EntityManager", "createQuery", "dql", 0)
            .matchingMethodCallArgument("\\Doctrine\\ORM\\Query", "setDQL", "dqlQuery", 0)
            .matchingVariableAssigment("dql")
            .build(),
        new LanguageInjection.Builder(LANGUAGE_ID_EXPRESSION_LANGUAGE)
            .matchingConstructorCallArgument("\\Symfony\\Component\\ExpressionLanguage\\Expression", "expression", 0)
            .matchingFunctionCallArgument("\\Symfony\\Component\\DependencyInjection\\Loader\\Configurator\\expr", "expression", 0)
            .matchingMethodCallArgument("\\Symfony\\Component\\ExpressionLanguage\\ExpressionLanguage", "evaluate", "expression", 0)
            .matchingMethodCallArgument("\\Symfony\\Component\\ExpressionLanguage\\ExpressionLanguage", "compile", "expression", 0)
            .matchingMethodCallArgument("\\Symfony\\Component\\ExpressionLanguage\\ExpressionLanguage", "parse", "expression", 0)
            .matchingMethodCallArgument("\\Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\\RouteTrait", "condition", "condition", 0)
            .matchingAttributeArgument("\\Symfony\\Component\\Validator\\Constraints\\Expression", "expression", 0)
            .matchingAttributeArgument("\\Symfony\\Component\\Routing\\Annotation\\Route", "condition", 9)
            .matchingAttributeArgument("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache", "lastModified", 7)
            .matchingAttributeArgument("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache", "etag", 8)
            .matchingAttributeArgument("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security", "data", 0)
            .matchingAttributeArgument("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Entity", "expr", 1)
            .matchingAnnotationProperty("\\Symfony\\Component\\Validator\\Constraints\\Expression", "expression", true)
            .matchingAnnotationProperty("\\Symfony\\Component\\Routing\\Annotation\\Route", "condition", false)
            .matchingAnnotationProperty("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route", "condition", false)
            .matchingAnnotationProperty("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Security", "expression", true)
            .matchingAnnotationProperty("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache", "lastModified", false)
            .matchingAnnotationProperty("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Cache", "Etag", false)
            .matchingAnnotationProperty("\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Entity", "expr", false)
            .build()
    };

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement element) {
        if (!(element instanceof StringLiteralExpression) || !((PsiLanguageInjectionHost) element).isValidHost()) {
            return;
        }

        if (!Symfony2ProjectComponent.isEnabled(element.getProject())) {
            return;
        }

        for (var injection : LANGUAGE_INJECTIONS) {
            if (injection.getPattern().accepts(element)) {
                var literal = (StringLiteralExpression) element;
                var language = injection.getLanguage();

                if (language != null) {
                    registrar.startInjecting(language);
                    registrar.addPlace(injection.getPrefix(), injection.getSuffix(), literal, literal.getValueRange());
                    registrar.doneInjecting();
                }
            }
        }
    }

    @Override
    public @NotNull
    List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(StringLiteralExpression.class);
    }
}
