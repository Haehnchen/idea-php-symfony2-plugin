package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.*;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil;
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

    private static class BlockFunctionReferenceCompletionProvider extends GotoCompletionProvider implements GotoCompletionProviderInterfaceEx {
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
                TwigBlockUtil.getBlockOverwriteTargets(element.getContainingFile(), blockName, true)
            );

            psiElements.addAll(
                TwigBlockUtil.getBlockImplementationTargets(element)
            );

            // filter self navigation
            return psiElements.stream()
                .filter(psiElement -> psiElement != element)
                .collect(Collectors.toSet());
        }

        @Override
        public void getLookupElements(@NotNull GotoCompletionProviderLookupArguments arguments) {
            arguments.addAllElements(TwigUtil.getBlockLookupElements(
                getProject(),
                TwigFileUtil.collectParentFiles(true, arguments.getParameters().getOriginalFile())
            ));
        }
    }
}
