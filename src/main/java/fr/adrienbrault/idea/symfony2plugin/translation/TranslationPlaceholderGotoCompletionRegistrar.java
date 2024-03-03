package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.NewExpression;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationPlaceholderGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        // {{ 'symfony.great'|trans({'fo<caret>f'}, 'symfony')) }}
        registrar.register(
            TwigPattern.getFunctionWithFirstParameterAsKeyLiteralPattern("trans"),
            new MyTwigTransFilterCompletionContributor("trans")
        );

        // {{ 'symfony.great'|transchoice(12, {'fo<caret>f'}, 'symfony')) }}
        registrar.register(
            TwigPattern.getFunctionWithSecondParameterAsKeyLiteralPattern("transchoice"),
            new MyTwigTransFilterCompletionContributor("transchoice")
        );

        // $x->trans('symfony.great', ['%fo<caret>obar%', null], 'symfony')
        registrar.register(
            PlatformPatterns.psiElement().withParent(StringLiteralExpression.class),
            new MyPhpTranslationCompletionContributor("trans", 1, 2)
        );

        // $x->transChoice('symfony.great', 10, ['%fo<caret>obar%', null], 'symfony')
        registrar.register(
            PlatformPatterns.psiElement().withParent(StringLiteralExpression.class),
            new MyPhpTranslationCompletionContributor("transChoice", 2, 3)
        );

        // new \Symfony\Component\Translation\TranslatableMessage('symfony.great', ['test' => '%fo<caret>obar%'], 'symfony');
        registrar.register(
            PlatformPatterns.psiElement().withParent(StringLiteralExpression.class),
            new MyPhpTranslatableMessageCompletionContributor()
        );
    }

    private static class MyTranslationPlaceholderGotoCompletionProvider extends GotoCompletionProvider {
        @NotNull
        private final String key;

        @NotNull
        private final String domain;

        MyTranslationPlaceholderGotoCompletionProvider(@NotNull PsiElement psiElement, @NotNull String key, @NotNull String domain) {
            super(psiElement);
            this.key = key;
            this.domain = domain;
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            return TranslationUtil.getPlaceholderFromTranslation(getProject(), key, domain).stream()
                .map(s -> LookupElementBuilder.create(s).withIcon(Symfony2Icons.TRANSLATION))
                .collect(Collectors.toList());
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            return Arrays.asList(TranslationUtil.getTranslationPsiElements(getProject(), key, domain));
        }
    }

    private static class MyPhpTranslationCompletionContributor implements GotoCompletionContributor {
        @NotNull
        private final String method;
        private final int placeHolderParameter;
        private final int domainParameter;

        private MyPhpTranslationCompletionContributor(@NotNull String method, int placeHolderParameter, int domainParameter) {
            this.method = method;
            this.placeHolderParameter = placeHolderParameter;
            this.domainParameter = domainParameter;
        }

        @Nullable
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            MethodMatcher.MethodMatchParameter match = new MethodMatcher.ArrayParameterMatcher(context, placeHolderParameter)
                .withSignature("Symfony\\Component\\Translation\\TranslatorInterface", method)
                .withSignature("Symfony\\Contracts\\Translation\\TranslatorInterface", method)
                .match();

            if (match == null) {
                return null;
            }

            PsiElement[] parameters = match.getMethodReference().getParameters();
            String key = PhpElementsUtil.getStringValue(parameters[0]);
            if(key == null) {
                return null;
            }

            String domain = "messages";
            if(parameters.length > domainParameter) {
                domain = PhpElementsUtil.getStringValue(parameters[domainParameter]);
                if(domain == null) {
                    return null;
                }
            }

            return new MyTranslationPlaceholderGotoCompletionProvider(psiElement, key, domain);
        }
    }

    /**
     * new \Symfony\Component\Translation\TranslatableMessage('symfony.great', ['test' => '%fo<caret>obar%'], 'symfony');
     * new \Symfony\Component\Translation\TranslatableMessage('symfony.great', ['%fo<caret>obar%', null], 'symfony');
     */
    private static class MyPhpTranslatableMessageCompletionContributor implements GotoCompletionContributor {
        @Nullable
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            ArrayCreationExpression arrayCreationExpression = PhpElementsUtil.getCompletableArrayCreationElement(context);
            if (arrayCreationExpression == null) {
                return null;
            }

            PsiElement parameterList = arrayCreationExpression.getContext();
            if (!(parameterList instanceof ParameterList)) {
                return null;
            }

            PsiElement[] parameters = ((ParameterList) parameterList).getParameters();
            int placeHolderParameter = 1;
            if (parameters.length < placeHolderParameter) {
                return null;
            }

            PsiElement newEx = parameterList.getContext();
            if (!(newEx instanceof NewExpression)) {
                return null;
            }

            ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(arrayCreationExpression);
            if (currentIndex == null || currentIndex.getIndex() != placeHolderParameter) {
                return null;
            }

            if (!PhpElementsUtil.isNewExpressionPhpClassWithInstance((NewExpression) newEx, TranslationUtil.PHP_TRANSLATION_TRANSLATABLE_MESSAGE)) {
                return null;
            }

            String key = PhpElementsUtil.getStringValue(parameters[0]);
            if (key == null) {
                return null;
            }

            String domain = "messages";
            int domainParameter = 2;
            if (parameters.length > domainParameter) {
                domain = PhpElementsUtil.getStringValue(parameters[domainParameter]);
                if(domain == null) {
                    return null;
                }
            }

            return new MyTranslationPlaceholderGotoCompletionProvider(psiElement, key, domain);
        }
    }

    /**
     * {{ 'symfony.great'|trans({'fo<caret>f'}, 'symfony')) }}
     */
    private record MyTwigTransFilterCompletionContributor(@NotNull String filter) implements GotoCompletionContributor {
        @Nullable
        @Override
        public GotoCompletionProvider getProvider(@NotNull PsiElement psiElement) {
            PsiElement parent = psiElement.getParent();
            if (parent.getNode().getElementType() != TwigElementTypes.LITERAL) {
                return null;
            }

            PsiElement functionCall = parent.getParent();
            if (functionCall.getNode().getElementType() != TwigElementTypes.FUNCTION_CALL) {
                return null;
            }

            // find translation key: 'symfony.great'
            PsiElement function = PsiElementUtils.getPrevSiblingOfType(functionCall, TwigPattern.getTranslationKeyPattern(this.filter));
            if (function == null) {
                return null;
            }

            String key = function.getText();
            if (StringUtils.isBlank(key)) {
                return null;
            }

            String domain = TwigUtil.getPsiElementTranslationDomain(function);
            if (StringUtils.isBlank(domain)) {
                return null;
            }

            return new MyTranslationPlaceholderGotoCompletionProvider(psiElement, key, domain);
        }
    }
}