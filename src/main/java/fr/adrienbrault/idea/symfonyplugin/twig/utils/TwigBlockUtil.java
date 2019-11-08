package fr.adrienbrault.idea.symfonyplugin.twig.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfonyplugin.stubs.indexes.TwigBlockIndexExtension;
import fr.adrienbrault.idea.symfonyplugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfonyplugin.twig.loader.FileImplementsLazyLoader;
import fr.adrienbrault.idea.symfonyplugin.twig.loader.FileOverwritesLazyLoader;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlockUtil {
    @NotNull
    public static Collection<TwigBlock> collectParentBlocks(boolean includeSelf, @NotNull PsiFile... file) {
        Collection<TwigBlock> blocks = new ArrayList<>();

        for (PsiFile psiFile : file) {
            blocks.addAll(collectBlocksInFile(includeSelf, psiFile));
        }

        return blocks;
    }

    @NotNull
    private static Collection<TwigBlock> collectBlocksInFile(boolean includeSelf, @NotNull PsiFile file) {
        Collection<TwigBlock> current = new ArrayList<>();

        for (VirtualFile virtualFile : TwigFileUtil.collectParentFiles(includeSelf, file)) {
            PsiFile psiFile = PsiManager.getInstance(file.getProject()).findFile(virtualFile);
            if(psiFile instanceof TwigFile) {
                current.addAll(TwigUtil.getBlocksInFile((TwigFile) psiFile));
            }
        }

        return current;
    }

    /**
     * Withs a file that extends given file scope and search for block names; based in indexed so its fast
     */
    public static boolean hasBlockImplementations(@NotNull PsiElement blockPsiName, @NotNull FileImplementsLazyLoader implementsLazyLoader) {
        String blockName = blockPsiName.getText();
        if(StringUtils.isBlank(blockName)) {
            return false;
        }

        PsiFile psiFile = blockPsiName.getContainingFile();
        if(psiFile == null) {
            return false;
        }

        Collection<VirtualFile> twigChild = implementsLazyLoader.getFiles();
        if(twigChild.size() == 0) {
            return false;
        }

        return hasBlockNamesForFiles(blockPsiName.getProject(), blockName, twigChild);
    }

    /**
     *  {% extends 'foobar.html.twig' %}
     *
     *  {{ block('foo<caret>bar') }}
     *  {% block 'foo<caret>bar' %}
     *  {% block foo<caret>bar %}
     */
    @NotNull
    public static Collection<PsiElement> getBlockImplementationTargets(@NotNull PsiElement blockPsiName) {
        String blockName = blockPsiName.getText();
        if(StringUtils.isBlank(blockName)) {
            return Collections.emptyList();
        }

        PsiFile psiFile = blockPsiName.getContainingFile();
        if(psiFile == null) {
            return Collections.emptyList();
        }

        Collection<VirtualFile> twigChild = TwigUtil.getTemplatesExtendingFile(psiFile.getProject(), psiFile.getVirtualFile());
        if(twigChild.size() == 0) {
            return Collections.emptyList();
        }

        Collection<PsiElement> blockTargets = new ArrayList<>();

        for (VirtualFile virtualFile : twigChild) {
            PsiFile file = PsiManager.getInstance(blockPsiName.getProject()).findFile(virtualFile);
            if(!(file instanceof TwigFile)) {
                continue;
            }

            for (TwigBlock twigBlock : TwigUtil.getBlocksInFile((TwigFile) file)) {
                if (blockName.equals(twigBlock.getName())) {
                    blockTargets.add(twigBlock.getTarget());
                }
            }
        }

        return blockTargets;
    }

    /**
     * Collect every block name by given file name; resolve the "extends" or "embed" scope
     */
    @NotNull
    public static Collection<PsiElement> getBlockOverwriteTargets(@NotNull PsiElement psiElement) {
        String blockName = psiElement.getText();

        if(StringUtils.isBlank(blockName)) {
            return Collections.emptyList();
        }

        Pair<Collection<PsiFile>, Boolean> scopedFile = TwigUtil.findScopedFile(psiElement);

        Collection<PsiElement> psiElements = new HashSet<>();
        for (PsiFile psiFile : scopedFile.getFirst()) {
            psiElements.addAll(getBlockOverwriteTargets(psiFile, blockName, scopedFile.getSecond()));
        }

        return psiElements;
    }

    /**
     * Visits block upwards eg "extends" and "use" statements
     */
    public static boolean hasBlockOverwrites(@NotNull PsiElement psiElement, @NotNull FileOverwritesLazyLoader fileOverwritesLazyLoader) {
        String blockName = psiElement.getText();

        if(StringUtils.isBlank(blockName)) {
            return false;
        }

        Pair<Collection<PsiFile>, Boolean> scopedFile = TwigUtil.findScopedFile(psiElement);

        Collection<VirtualFile> virtualFiles = fileOverwritesLazyLoader.getFiles(
            scopedFile.second,
            scopedFile.first.stream().map(PsiFile::getVirtualFile).collect(Collectors.toSet())
        );

        return hasBlockNamesForFiles(psiElement.getProject(), blockName, virtualFiles);
    }

    /**
     * Collect every block name by given file name; resolve the "extends"
     */
    @NotNull
    public static Collection<PsiElement> getBlockOverwriteTargets(@NotNull PsiFile psiFile, @NotNull String blockName, boolean withSelfBlocks) {
        Collection<PsiElement> psiElements = new ArrayList<>();

        for (TwigBlock block : collectParentBlocks(withSelfBlocks, psiFile)) {
            if(block.getName().equals(blockName)) {
                Collections.addAll(psiElements, block.getTarget());
            }
        }

        return psiElements;
    }

    /**
     * Check is every given file provides a block by name
     */
    private static boolean hasBlockNamesForFiles(@NotNull Project project, @NotNull String blockName, @NotNull Collection<VirtualFile> virtualFiles) {
        return FileBasedIndex.getInstance()
            .getValues(TwigBlockIndexExtension.KEY, "block", GlobalSearchScope.filesScope(project, virtualFiles))
            .stream()
            .anyMatch(block -> block.contains(blockName));
    }
}
