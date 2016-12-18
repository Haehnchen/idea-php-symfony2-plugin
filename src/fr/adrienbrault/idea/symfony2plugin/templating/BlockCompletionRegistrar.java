package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class BlockCompletionRegistrar implements GotoCompletionRegistrar {

    public void register(GotoCompletionRegistrarParameter registrar) {

        // {{ block('foo_block') }}
        registrar.register(TwigHelper.getPrintBlockFunctionPattern("block"), psiElement -> {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) {
                return null;
            }

            return new BlockFunctionReferenceCompletionProvider(psiElement);
        });
    }

    private static class BlockFunctionReferenceCompletionProvider extends GotoCompletionProvider {

        public BlockFunctionReferenceCompletionProvider(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        public Collection<PsiElement> getPsiTargets(PsiElement element) {

            String blockName = PsiElementUtils.trimQuote(element.getText());
            if(StringUtils.isBlank(blockName)) {
                return Collections.emptyList();
            }

            return Arrays.asList(
                TwigTemplateGoToDeclarationHandler.getBlockNameGoTo(element.getContainingFile(), blockName, true)
            );

        }

        @NotNull
        public Collection<LookupElement> getLookupElements() {

            Collection<LookupElement> lookupElements = new ArrayList<>();

            Map<String, VirtualFile> twigFilesByName = TwigHelper.getTwigFilesByName(getElement().getProject());
            List<TwigBlock> blocks = new TwigBlockParser(twigFilesByName).withSelfBlocks(true).walk(getElement().getContainingFile());
            List<String> uniqueList = new ArrayList<>();
            for (TwigBlock block : blocks) {
                if(!uniqueList.contains(block.getName())) {
                    uniqueList.add(block.getName());
                    lookupElements.add(new TwigBlockLookupElement(block));
                }
            }

            return lookupElements;
        }

    }
}
