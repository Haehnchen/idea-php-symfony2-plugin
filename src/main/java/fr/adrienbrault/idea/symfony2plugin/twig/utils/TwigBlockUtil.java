package fr.adrienbrault.idea.symfony2plugin.twig.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigBlockStatement;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockEmbedIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.loader.FileImplementsLazyLoader;
import fr.adrienbrault.idea.symfony2plugin.twig.loader.FileOverwritesLazyLoader;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlockUtil {
    /**
     * Returns the subset of {@code virtualFiles} that contain {@code blockName} according to the Twig block index.
     * Use this to pre-filter before loading PSI, avoiding traversal of files that do not define the block.
     */
    @NotNull
    public static Collection<VirtualFile> filterFilesWithBlock(
        @NotNull Project project,
        @NotNull String blockName,
        @NotNull Collection<VirtualFile> virtualFiles
    ) {
        if (virtualFiles.isEmpty()) {
            return Collections.emptyList();
        }

        Set<VirtualFile> result = new HashSet<>();
        FileBasedIndex.getInstance().processValues(
            TwigBlockIndexExtension.KEY,
            "block",
            null,
            (file, blockNames) -> {
                if (blockNames.contains(blockName)) {
                    result.add(file);
                }
                return true;
            },
            GlobalSearchScope.filesScope(project, virtualFiles)
        );

        return result;
    }

    /**
     * Resolves PSI targets of {@code blockName} across all templates of the given Twig component.
     */
    @NotNull
    public static Collection<PsiElement> getComponentBlockTargets(@NotNull Project project, @NotNull String componentName, @NotNull String blockName) {
        Collection<VirtualFile> templateVirtualFiles = new ArrayList<>();
        for (PsiFile f : UxUtil.getComponentTemplates(project, componentName)) {
            VirtualFile vf = f.getVirtualFile();
            if (vf != null && f instanceof TwigFile) templateVirtualFiles.add(vf);
        }

        return resolveBlockTargets(project, blockName, templateVirtualFiles);
    }

    /**
     * Pre-filters {@code candidateFiles} via index, then traverses PSI only for files that define {@code blockName}.
     */
    @NotNull
    private static Collection<PsiElement> resolveBlockTargets(@NotNull Project project, @NotNull String blockName, @NotNull Collection<VirtualFile> candidateFiles) {
        Collection<PsiElement> targets = new ArrayList<>();
        for (VirtualFile vf : filterFilesWithBlock(project, blockName, candidateFiles)) {
            if (!(PsiManager.getInstance(project).findFile(vf) instanceof TwigFile twigFile)) continue;
            for (TwigBlock block : TwigUtil.getBlocksInFile(twigFile)) {
                if (blockName.equals(block.getName())) targets.add(block.getTarget());
            }
        }
        return targets;
    }

    /**
     * Withs a file that extends given file scope and search for block names; based in indexed so its fast
     */
    public static boolean hasBlockImplementations(@NotNull PsiElement blockPsiName, @NotNull FileImplementsLazyLoader implementsLazyLoader) {
        String blockName = blockPsiName.getText();
        if (StringUtils.isBlank(blockName)) {
            return false;
        }

        PsiFile psiFile = blockPsiName.getContainingFile();
        if (psiFile == null) {
            return false;
        }

        Collection<VirtualFile> twigChild = implementsLazyLoader.getFiles();

        Set<VirtualFile> virtualFiles = new HashSet<>(twigChild);
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile != null) {
            virtualFiles.add(virtualFile);
        }

        Project project = psiFile.getProject();

        for (VirtualFile file : virtualFiles) {
            if (hasBlockImplementationsForEmbed(project, file, blockName)) {
                return true;
            }
        }

        if (twigChild.isEmpty()) {
            return false;
        }

        return hasBlockNamesForFiles(project, blockName, twigChild);
    }

    /**
     *  {% extends 'foobar.html.twig' %}
     *  {% embed 'foobar.html.twig' %}
     *
     *  {{ block('foo<caret>bar') }}
     *  {% block 'foo<caret>bar' %}
     *  {% block foo<caret>bar %}
     */
    @NotNull
    public static Collection<PsiElement> getBlockImplementationTargets(@NotNull PsiElement blockPsiName) {
        String blockName = blockPsiName.getText();
        if (StringUtils.isBlank(blockName)) {
            return Collections.emptyList();
        }

        PsiFile psiFile = blockPsiName.getContainingFile();
        if (psiFile == null) {
            return Collections.emptyList();
        }

        Project project = psiFile.getProject();
        VirtualFile currentVirtualFile = psiFile.getVirtualFile();
        Collection<VirtualFile> parentTwigExtendingFiles = TwigUtil.getTemplatesExtendingFile(project, currentVirtualFile);

        HashSet<VirtualFile> embedFiles = new HashSet<>(parentTwigExtendingFiles);
        embedFiles.add(currentVirtualFile);

        Collection<PsiElement> blockTargets = new HashSet<>();

        // embed targets
        for (VirtualFile vFile : embedFiles) {
            Set<String> templateNames = TwigUtil.getTemplateNamesForFile(blockPsiName.getProject(), vFile).stream()
                .map(TwigUtil::normalizeTemplateName)
                .collect(Collectors.toSet());

            for (String templateName : templateNames) {
                for (VirtualFile containingFile : FileBasedIndex.getInstance().getContainingFiles(TwigBlockEmbedIndex.KEY, templateName, GlobalSearchScope.allScope(project))) {
                    if(!(PsiManager.getInstance(project).findFile(containingFile) instanceof TwigFile twigFile)) {
                        continue;
                    }

                    TwigUtil.visitEmbedBlocks(twigFile, embedBlock -> {
                        if (embedBlock.blockName().equals(blockName) && templateName.equals(embedBlock.templateName())) {
                            blockTargets.add(getBlockNamePsiElementTarget(embedBlock.target()));
                        }
                    });
                }
            }
        }

        blockTargets.addAll(resolveBlockTargets(project, blockName, parentTwigExtendingFiles));

        return blockTargets;
    }

    /**
     * Extracted named block element for ui presentable
     */
    private static PsiElement getBlockNamePsiElementTarget(@NotNull TwigBlockStatement twigBlockStatement) {
        PsiElement blockTag = twigBlockStatement.getFirstChild();

        if (blockTag != null) {
            PsiElement childrenOfType = PsiElementUtils.getChildrenOfType(blockTag, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER));
            if (childrenOfType != null) {
                return childrenOfType;
            }
        }

        return twigBlockStatement;
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

        if (StringUtils.isBlank(blockName)) {
            return false;
        }

        Pair<Collection<PsiFile>, Boolean> scopedFile = TwigUtil.findScopedFile(psiElement);

        Collection<VirtualFile> virtualFiles = fileOverwritesLazyLoader.getFiles(
            scopedFile.second,
            scopedFile.first.stream().map(PsiFile::getVirtualFile).collect(Collectors.toSet())
        );

        return hasBlockNamesForFiles(psiElement.getProject(), blockName, virtualFiles);
    }

    private static boolean hasBlockImplementationsForEmbed(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull String blockName) {
        for (String templateName : TwigUtil.getTemplateNamesForFile(project, virtualFile)) {
            Set<String> blockNames = FileBasedIndex.getInstance().getValues(TwigBlockEmbedIndex.KEY, templateName, GlobalSearchScope.allScope(project))
                .stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

            if (blockNames.contains(blockName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Collect every block name by given file name; resolve the "extends"
     */
    @NotNull
    public static Collection<PsiElement> getBlockOverwriteTargets(@NotNull PsiFile psiFile, @NotNull String blockName, boolean withSelfBlocks) {
        Collection<VirtualFile> parentVirtualFiles = TwigFileUtil.collectParentFiles(withSelfBlocks, psiFile);
        if (parentVirtualFiles.isEmpty()) {
            return Collections.emptyList();
        }

        Project project = psiFile.getProject();

        return resolveBlockTargets(project, blockName, parentVirtualFiles);
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
