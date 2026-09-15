package fr.adrienbrault.idea.symfony2plugin.navigation

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import com.jetbrains.twig.TwigFile
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils

private val SYMFONY_TWIG_BLOCK_NAMES = Key.create<CachedValue<Array<String>>>("SYMFONY_TWIG_BLOCK_NAMES")

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigBlockSymbolContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        val project = scope.project ?: return
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return
        }

        val blocks = CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_TWIG_BLOCK_NAMES,
            {
                val blockNames = FileBasedIndex.getInstance()
                    .getValues(TwigBlockIndexExtension.KEY, "block", GlobalSearchScope.allScope(project))
                    .flatten()
                    .distinct()
                    .toTypedArray()

                CachedValueProvider.Result.create(
                    blockNames,
                    FileIndexCaches.getModificationTrackerForIndexId(project, TwigBlockIndexExtension.KEY),
                )
            },
            false,
        )

        blocks.forEach(processor::process)
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters,
    ) {
        val project = parameters.project
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return
        }

        val query = FileBasedIndex.AllKeysQuery<String, Set<String>>(
            TwigBlockIndexExtension.KEY,
            listOf("block"),
        ) { name in it }

        val virtualFiles = mutableSetOf<VirtualFile>()
        FileBasedIndex.getInstance().processFilesContainingAllKeys(
            listOf(query),
            GlobalSearchScope.allScope(project),
        ) { virtualFiles.add(it) }

        PsiElementUtils.convertVirtualFilesToPsiFiles(project, virtualFiles)
            .filterIsInstance<TwigFile>()
            .forEach { file ->
                TwigUtil.getBlocksInFile(file)
                    .map {
                        NavigationItemExStateless.create(
                            it.target,
                            it.name,
                            Symfony2Icons.TWIG_BLOCK_OVERWRITE,
                            "Block",
                            true,
                        )
                    }
                    .toSet()
                    .forEach(processor::process)
            }
    }
}
