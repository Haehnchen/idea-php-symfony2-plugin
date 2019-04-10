package fr.adrienbrault.idea.symfony2plugin.lang;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ParameterLanguageInjector implements MultiHostInjector {

    private static final MethodMatcher.CallToSignature[] CSS_SELECTOR_SIGNATURES = {
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "filter"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "children"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\CssSelector\\CssSelectorConverter", "toXPath"),
    };

    private static final MethodMatcher.CallToSignature[] XPATH_SIGNATURES = {
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "filterXPath"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "evaluate"),
    };

    private static final MethodMatcher.CallToSignature[] JSON_SIGNATURES = {
            //new MethodMatcher.CallToSignature("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "__construct"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "fromJsonString"),
            new MethodMatcher.CallToSignature("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "setJson"),
    };

    private static final MethodMatcher.CallToSignature[] DQL_SIGNATURES = {
            new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\EntityManager", "createQuery"),
            new MethodMatcher.CallToSignature("\\Doctrine\\ORM\\Query", "setDQL"),
    };
    public static final String DQL_VARIABLE_NAME = "dql";

    private final MethodLanguageInjection[] LANGUAGE_INJECTIONS = {
            new MethodLanguageInjection(LANGUAGE_ID_CSS, "@media all { ", " }", CSS_SELECTOR_SIGNATURES),
            new MethodLanguageInjection(LANGUAGE_ID_XPATH, null, null, XPATH_SIGNATURES),
            new MethodLanguageInjection(LANGUAGE_ID_JSON, null, null, JSON_SIGNATURES),
            new MethodLanguageInjection(LANGUAGE_ID_DQL, null, null, DQL_SIGNATURES),
    };

    private static final String LANGUAGE_ID_JSON = "JSON";
    private static final String LANGUAGE_ID_XPATH = "XPath";
    private static final String LANGUAGE_ID_CSS = "CSS";
    private static final String LANGUAGE_ID_DQL = "DQL";

    public ParameterLanguageInjector() {
    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(StringLiteralExpressionImpl.class);
    }

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement element) {
        if (!(element instanceof StringLiteralExpression) || !((PsiLanguageInjectionHost) element).isValidHost()) {
            return;
        }
        if (!Symfony2ProjectComponent.isEnabled(element.getProject())) {
            return;
        }

        final StringLiteralExpressionImpl expr = (StringLiteralExpressionImpl) element;

        PsiElement parent = expr.getParent();

        final boolean isParameter = parent instanceof ParameterList && expr.getPrevPsiSibling() == null; // 1st parameter
        final boolean isAssignment = parent instanceof AssignmentExpression;

        if (!isParameter && !isAssignment) {
            return;
        }

        if (isParameter)  {
            parent = parent.getParent();
        }

        for (MethodLanguageInjection languageInjection : LANGUAGE_INJECTIONS) {
            Language language = languageInjection.getLanguage();
            if (language == null) {
                continue;
            }
            // $crawler->filter('...')
            // $em->createQuery('...')
            // JsonResponse::fromJsonString('...')
            if (parent instanceof MethodReference) {
                if (PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) parent, languageInjection.getSignatures())) {
                    injectLanguage(registrar, expr, language, languageInjection);
                    return;
                }
            }
            // $dql = "...";
            else if (parent instanceof AssignmentExpression) {
                if (LANGUAGE_ID_DQL.equals(language.getID())) {
                    PhpPsiElement variable = ((AssignmentExpression) parent).getVariable();
                    if (variable instanceof Variable) {
                        if (DQL_VARIABLE_NAME.equals(variable.getName())) {
                            injectLanguage(registrar, expr, language, languageInjection);
                            return;
                        }
                    }
                }
            }
        }

    }

    private void injectLanguage(@NotNull MultiHostRegistrar registrar, @NotNull StringLiteralExpressionImpl element, Language language, MethodLanguageInjection languageInjection) {
        final int length = ((StringLiteralExpression) element).getContents().length();
        final TextRange range = TextRange.create(1, length + 1);

        registrar.startInjecting(language)
                .addPlace(languageInjection.getPrefix(), languageInjection.getSuffix(), element, range)
                .doneInjecting();
    }

    private class MethodLanguageInjection {
        private final Language language;
        private final String prefix;
        private final String suffix;
        private final MethodMatcher.CallToSignature[] signatures;

        MethodLanguageInjection(@NotNull String languageId, String prefix, String suffix, MethodMatcher.CallToSignature[] signatures) {

            this.language = Language.findLanguageByID(languageId);
            this.prefix = prefix;
            this.suffix = suffix;
            this.signatures = signatures;
        }

        public Language getLanguage() {
            return language;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public MethodMatcher.CallToSignature[] getSignatures() {
            return signatures;
        }
    }
}
