package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlockSymbolContributor implements ChooseByNameContributorEx {
    private static final Key<CachedValue<String[]>> SYMFONY_TWIG_BLOCK_NAMES = new Key<>("SYMFONY_TWIG_BLOCK_NAMES");

    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        Project project = scope.getProject();
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        String[] blocks = CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_TWIG_BLOCK_NAMES,
            () -> {
                String[] blocksInner = FileBasedIndex.getInstance()
                    .getValues(TwigBlockIndexExtension.KEY, "block", GlobalSearchScope.allScope(project))
                    .stream()
                    .flatMap(Collection::stream)
                    .distinct()
                    .toArray(String[]::new);

                return CachedValueProvider.Result.create(blocksInner, FileIndexCaches.getModificationTrackerForIndexId(project, TwigBlockIndexExtension.KEY));
            },
            false
        );

        for (String block : blocks) {
            processor.process(block);
        }
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        FileBasedIndex.AllKeysQuery<String, Set<String>> query = new FileBasedIndex.AllKeysQuery<>(
            TwigBlockIndexExtension.KEY,
            List.of("block"),
            strings -> strings.contains(name)
        );

        Set<VirtualFile> virtualFiles = new HashSet<>();
        FileBasedIndex.getInstance().processFilesContainingAllKeys(List.of(query), GlobalSearchScope.allScope(project), virtualFile -> {
            virtualFiles.add(virtualFile);
            return true;
        });

        for (PsiFile psiFile : PsiElementUtils.convertVirtualFilesToPsiFiles(project, virtualFiles)) {
            if (psiFile instanceof TwigFile) {
                Set<NavigationItem> block = TwigUtil.getBlocksInFile((TwigFile) psiFile)
                    .stream()
                    .map((Function<TwigBlock, NavigationItem>) twigBlock -> NavigationItemExStateless.create(twigBlock.getTarget(), twigBlock.getName(), Symfony2Icons.TWIG_BLOCK_OVERWRITE, "Block", true))
                    .collect(Collectors.toSet());

                for (NavigationItem navigationItem : block) {
                    processor.process(navigationItem);
                }
            }
        }
    }
}
