package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationPlaceholderGotoCompletionRegistrar implements GotoCompletionRegistrar {
    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        // {{ 'symfony.great'|trans({'fo<caret>f'}, 'symfony')) }}
        registrar.register(
            TwigHelper.getFunctionWithFirstParameterAsKeyLiteralPattern("trans"), psiElement -> {
                PsiElement parent = psiElement.getParent();
                if(parent.getNode().getElementType() != TwigElementTypes.LITERAL) {
                    return null;
                }

                PsiElement function = PsiElementUtils.getPrevSiblingOfType(parent, TwigHelper.getTranslationPattern("trans"));
                if(function == null) {
                    return null;
                }

                String key = function.getText();
                if(StringUtils.isBlank(key)) {
                    return null;
                }

                String domain = TwigUtil.getPsiElementTranslationDomain(function);
                if(StringUtils.isBlank(domain)) {
                    return null;
                }

                return new MyTranslationPlaceholderGotoCompletionProvider(psiElement, key, domain);
            }
        );

        // $x->trans('symfony.great', ['%fo<caret>obar%', null], 'symfony')
        registrar.register(
            PlatformPatterns.psiElement().withParent(StringLiteralExpression.class), psiElement -> {
                PsiElement context = psiElement.getContext();
                if (!(context instanceof StringLiteralExpression)) {
                    return null;
                }

                MethodMatcher.MethodMatchParameter match = new MethodMatcher.ArrayParameterMatcher(context, 1)
                    .withSignature("Symfony\\Component\\Translation\\TranslatorInterface", "trans")
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
                if(parameters.length > 2) {
                    domain = PhpElementsUtil.getStringValue(parameters[2]);
                    if(domain == null) {
                        return null;
                    }
                }

                return new MyTranslationPlaceholderGotoCompletionProvider(psiElement, key, domain);
            }
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
            Set<String> placeholder = new HashSet<>();

            for (PsiElement element : TranslationUtil.getTranslationPsiElements(getProject(), key, domain)) {
                if (!(element instanceof YAMLScalar)) {
                    continue;
                }

                String textValue = ((YAMLScalar) element).getTextValue();
                if(StringUtils.isBlank(textValue)) {
                    continue;
                }

                placeholder.addAll(
                    TranslationUtil.getPlaceholderFromTranslation(textValue)
                );
            }

            return placeholder.stream()
                .map(s -> LookupElementBuilder.create(s).withIcon(Symfony2Icons.TRANSLATION))
                .collect(Collectors.toList());
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            return Arrays.asList(TranslationUtil.getTranslationPsiElements(getProject(), key, domain));
        }
    }
}