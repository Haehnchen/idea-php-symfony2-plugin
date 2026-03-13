package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil

/**
 * Utility for collecting Vite entry points from vite.config.js / vite.config.ts files.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
object ViteUtil {

    private val VITE_CACHE_KEY = Key.create<CachedValue<List<ViteEntry>>>("SYMFONY_VITE_ENTRIES")

    fun getEntries(project: Project): List<ViteEntry> {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            VITE_CACHE_KEY,
            {
                val configFiles = getViteConfigFiles(project)
                val entries = collectEntries(project, configFiles)
                val dependencies: Array<Any> = if (configFiles.isEmpty()) {
                    arrayOf(project)
                } else {
                    configFiles.toTypedArray()
                }
                CachedValueProvider.Result.create(entries, *dependencies)
            },
            false
        )
    }

    private fun collectEntries(project: Project, configFiles: Collection<VirtualFile>): List<ViteEntry> {
        val entries = mutableListOf<ViteEntry>()
        val parser = ViteConfigParser()

        for (file in configFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(file)
            if (psiFile is JSFile) {
                entries.addAll(parser.parseEntries(psiFile))
            }
        }

        return entries
    }

    fun getViteConfigFiles(project: Project): Collection<VirtualFile> {
        val files = mutableListOf<VirtualFile>()

        files.addAll(FilenameIndex.getVirtualFilesByName("vite.config.js", GlobalSearchScope.projectScope(project)))
        files.addAll(FilenameIndex.getVirtualFilesByName("vite.config.ts", GlobalSearchScope.projectScope(project)))

        return files.filter { !isTestFile(project, it) }
    }

    private fun isTestFile(project: Project, virtualFile: VirtualFile): Boolean {
        val path = VfsUtil.getRelativePath(virtualFile, ProjectUtil.getProjectDir(project), '/') ?: return false
        val lower = path.lowercase()
        return lower.contains("/test/") || lower.contains("/tests/")
    }
}
