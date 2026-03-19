package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TemplateMoveRenameUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Preserves Symfony logical template names during Twig file move refactoring.
 *
 * <p>The JetBrains Twig plugin's {@code FileReference.bindToElement()} converts logical template
 * names (e.g. {@code 'foo/bar.html.twig'}) to relative file-system paths, which is wrong in
 * Symfony projects. This handler counters that by recording the necessary information before the
 * move and writing the correct strings after all {@code bindToElement()} calls have run:
 *
 * <ul>
 *   <li><b>Files referencing the moved file</b> (via include/extends/embed/…): after the move
 *       their template path is updated to the <em>new</em> Symfony logical name at the new
 *       location (computed from the VFS path, which IntelliJ updates in-place during the move).
 *   <li><b>The moved file's own include/extends tags</b>: their targets did not move, so the
 *       original logical name is simply restored.
 * </ul>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigMoveFileHandler extends MoveFileHandler {

    @Override
    public boolean canProcessElement(PsiFile file) {
        return file instanceof TwigFile;
    }

    @Override
    public void prepareMovedFile(PsiFile file, PsiDirectory moveDestination, Map<PsiElement, PsiElement> oldToNewMap) {
        // nothing — we handle everything via findUsages/retargetUsages
    }

    @Override
    public @Nullable @Unmodifiable List<UsageInfo> findUsages(
            @NotNull PsiFile psiFile,
            @NotNull PsiDirectory newParent,
            boolean searchInComments,
            boolean searchInNonJavaFiles) {

        if (!(psiFile instanceof TwigFile twigFile)) {
            return null;
        }

        VirtualFile movedVFile = psiFile.getVirtualFile();
        if (movedVFile == null) {
            return null;
        }

        Project project = psiFile.getProject();
        List<UsageInfo> result = new ArrayList<>();

        // 1. Files that reference the moved file — update to the new logical name after move.
        collectReferencingFiles(twigFile, movedVFile, project, result);

        // 2. The moved file's own extends/include — restore original (targets didn't move).
        collectOwnReferences(twigFile, result);

        return result.isEmpty() ? null : result;
    }

    /**
     * Applies the correct template name to every collected usage after the physical move:
     * <ul>
     *   <li>Referencing files ({@code movedFileVFile != null}): resolves the <em>new</em> Symfony
     *       logical name from the moved file's VirtualFile (already at the new path by now) and
     *       writes it, overriding the relative path the Twig plugin produced.
     *   <li>Own references ({@code movedFileVFile == null}): restores {@code originalTemplateName}
     *       because those targets did not move.
     * </ul>
     */
    @Override
    public void retargetUsages(
            @NotNull @Unmodifiable List<? extends UsageInfo> usages,
            @NotNull Map<PsiElement, PsiElement> oldToNewMap) {

        // Process in descending offset order so that earlier offsets in the same file
        // are not shifted by replacements at higher positions (commitDocument changes doc length).
        usages.stream()
            .filter(u -> u instanceof TwigTagUsageInfo)
            .map(u -> (TwigTagUsageInfo) u)
            .sorted(Comparator.comparingInt((TwigTagUsageInfo u) -> u.absoluteTemplateNameOffset).reversed())
            .forEach(twigUsage -> {
                Project project = resolveProject(twigUsage, oldToNewMap);

                PsiFile containingFile = PsiManager.getInstance(project).findFile(twigUsage.containingVFile);
                if (containingFile == null) {
                    return;
                }

                String nameToWrite;
                if (twigUsage.movedFileVFile != null) {
                    // Referencing file: write the new logical name at the moved file's new path.
                    // movedFileVFile is a live VirtualFile updated in-place by the VFS move.
                    String newName = TemplateMoveRenameUtil.resolveNewTemplateName(
                        project, twigUsage.movedFileVFile, twigUsage.originalTemplateName
                    );
                    // If the new location is outside all configured namespaces, fall back to the
                    // original name — it is wrong too, but at least not a raw relative path.
                    nameToWrite = newName != null ? newName : twigUsage.originalTemplateName;
                } else {
                    // Own reference: target didn't move — restore the original logical name.
                    nameToWrite = twigUsage.originalTemplateName;
                }

                TemplateMoveRenameUtil.applyToTwigFileByOffset(
                    containingFile,
                    twigUsage.absoluteTemplateNameOffset,
                    nameToWrite
                );
            });
    }

    @Override
    public void updateMovedFile(PsiFile file) {
        // handled via findUsages/retargetUsages so it runs after decodeFileReferences()
    }

    // --- helpers ---

    private static void collectReferencingFiles(
            @NotNull TwigFile psiFile,
            @NotNull VirtualFile movedVFile,
            @NotNull Project project,
            @NotNull List<UsageInfo> result) {

        Collection<String> templateNames = TwigUtil.getTemplateNamesForFile(project, movedVFile);
        if (templateNames.isEmpty()) {
            return;
        }

        FileBasedIndex index = FileBasedIndex.getInstance();
        PsiManager psiManager = PsiManager.getInstance(project);
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

        Set<VirtualFile> processedIncludes = new HashSet<>();
        Set<VirtualFile> processedExtends = new HashSet<>();

        for (String templateName : templateNames) {
            String normalizedName = TwigUtil.normalizeTemplateName(templateName);

            index.processValues(TwigIncludeStubIndex.KEY, normalizedName, null, (file, value) -> {
                if (!processedIncludes.add(file)) return true;
                PsiFile source = psiManager.findFile(file);
                if (!(source instanceof TwigFile twigSource)) return true;
                collectInclude(twigSource, normalizedName, movedVFile, result);
                return true;
            }, scope);

            index.processValues(TwigExtendsStubIndex.KEY, normalizedName, null, (file, value) -> {
                if (!processedExtends.add(file)) return true;
                PsiFile source = psiManager.findFile(file);
                if (!(source instanceof TwigFile twigSource)) return true;
                collectExtends(twigSource, normalizedName, movedVFile, result);
                return true;
            }, scope);
        }
    }

    /** Collects the moved file's own extends/include references (their targets haven't moved). */
    private static void collectOwnReferences(
            @NotNull TwigFile movedFile,
            @NotNull List<UsageInfo> result) {

        TwigUtil.visitTemplateIncludes(movedFile, include -> {
            addUsage(include.getPsiElement(), include.getTemplateName(), null, result);
        });

        TwigUtil.visitTemplateExtends(movedFile, pair -> {
            addUsage(pair.getSecond(), pair.getFirst(), null, result);
        });
    }

    private static void collectInclude(
            @NotNull TwigFile sourceFile,
            @NotNull String normalizedName,
            @NotNull VirtualFile movedVFile,
            @NotNull List<UsageInfo> result) {

        TwigUtil.visitTemplateIncludes(sourceFile, include -> {
            if (!TwigUtil.normalizeTemplateName(include.getTemplateName()).equals(normalizedName)) return;
            addUsage(include.getPsiElement(), include.getTemplateName(), movedVFile, result);
        });
    }

    private static void collectExtends(
            @NotNull TwigFile sourceFile,
            @NotNull String normalizedName,
            @NotNull VirtualFile movedVFile,
            @NotNull List<UsageInfo> result) {

        TwigUtil.visitTemplateExtends(sourceFile, pair -> {
            if (!TwigUtil.normalizeTemplateName(pair.getFirst()).equals(normalizedName)) return;
            addUsage(pair.getSecond(), pair.getFirst(), movedVFile, result);
        });
    }

    /**
     * @param movedFileVFile the VirtualFile of the moved template, or {@code null} for own
     *                       references (targets that did not move)
     */
    private static void addUsage(
            @Nullable PsiElement element,
            @Nullable String rawTemplateName,
            @Nullable VirtualFile movedFileVFile,
            @NotNull List<UsageInfo> result) {

        if (element == null || rawTemplateName == null) return;

        VirtualFile containingVFile = element.getContainingFile().getVirtualFile();
        if (containingVFile == null) return;

        int startIndex = element.getText().indexOf(rawTemplateName);
        if (startIndex < 0) return;

        int absoluteOffset = element.getTextRange().getStartOffset() + startIndex;

        result.add(new TwigTagUsageInfo(
            element,
            startIndex,
            startIndex + rawTemplateName.length(),
            containingVFile,
            absoluteOffset,
            rawTemplateName,
            movedFileVFile
        ));
    }

    @NotNull
    private static Project resolveProject(
            @NotNull TwigTagUsageInfo twigUsage,
            @NotNull Map<PsiElement, PsiElement> oldToNewMap) {

        for (PsiElement el : oldToNewMap.values()) {
            if (el != null) return el.getProject();
        }
        PsiElement el = twigUsage.getElement();
        if (el != null) return el.getProject();
        throw new IllegalStateException("Cannot resolve project from oldToNewMap or UsageInfo");
    }

    // --- inner class ---

    private static final class TwigTagUsageInfo extends UsageInfo {
        @NotNull final VirtualFile containingVFile;
        final int absoluteTemplateNameOffset;
        @NotNull final String originalTemplateName;
        /**
         * The VirtualFile of the moved template (live, updated in-place by VFS during move).
         * {@code null} for own references — targets that did not move.
         */
        @Nullable final VirtualFile movedFileVFile;

        TwigTagUsageInfo(
                @NotNull PsiElement element,
                int startOffset,
                int endOffset,
                @NotNull VirtualFile containingVFile,
                int absoluteTemplateNameOffset,
                @NotNull String originalTemplateName,
                @Nullable VirtualFile movedFileVFile) {
            super(element, startOffset, endOffset, false);
            this.containingVFile = containingVFile;
            this.absoluteTemplateNameOffset = absoluteTemplateNameOffset;
            this.originalTemplateName = originalTemplateName;
            this.movedFileVFile = movedFileVFile;
        }
    }
}
