package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * {{ block('foo_block') }}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class BlockGotoCompletionRegistrar implements GotoCompletionRegistrar {
    public void register(@NotNull GotoCompletionRegistrarParameter registrar) {
        // {{ block('foo_block') }}
        registrar.register(TwigPattern.getPrintBlockFunctionPattern("block"), psiElement -> {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new BlockFunctionReferenceCompletionProvider(psiElement);
        });
    }

    private static class BlockFunctionReferenceCompletionProvider extends GotoCompletionProvider {
        private BlockFunctionReferenceCompletionProvider(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        public Collection<PsiElement> getPsiTargets(PsiElement element) {
            String blockName = PsiElementUtils.trimQuote(element.getText());
            if(StringUtils.isBlank(blockName)) {
                return Collections.emptyList();
            }

            Collection<PsiElement> psiElements = new HashSet<>();

            psiElements.addAll(
                TwigTemplateGoToDeclarationHandler.getBlockNameGoTo(element.getContainingFile(), blockName, true)
            );

            psiElements.addAll(
                TwigUtil.getBlocksByImplementations(element)
            );

            // filter self navigation
            return psiElements.stream()
                .filter(psiElement -> psiElement != element)
                .collect(Collectors.toSet());
        }

        @NotNull
        public Collection<LookupElement> getLookupElements() {
            return TwigUtil.getBlockLookupElements(
                getProject(),
                new TwigBlockParser(true).walk(getElement().getContainingFile())
            );
        }
    }
}
