package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

/**
 * Line marker for JavaScript/TypeScript files that are Vite entry points.
 *
 * When a JS/TS file is referenced in a vite.config.js/ts rollupOptions.input,
 * a gutter icon appears at the top of the file that navigates to the JSProperty
 * in the Vite config defining that entry.
 *
 * Uses [ViteEntryStubIndex] for efficient reverse lookup (file path → entry name)
 * and [ViteUtil.getEntries] (cached) for PSI navigation targets.
 *
 * Registered for both JavaScript and TypeScript in plugin.xml.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ViteEntryStubIndex
 */
class ViteJavaScriptLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(
        elements: List<PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        if (elements.isEmpty() || !Symfony2ProjectComponent.isEnabled(elements[0])) {
            return
        }

        for (element in elements) {
            // Only fire once per file — match on the JSFile node itself
            if (element !is JSFile) continue

            val project = element.project
            val virtualFile = element.virtualFile ?: continue

            val targets = mutableListOf<PsiElement>()

            for (configVirtualFile in ViteUtil.getViteConfigFiles(project)) {
                val configDir = configVirtualFile.parent ?: continue

                // Compute path of the current file relative to the config's directory
                val relPath = VfsUtil.getRelativePath(virtualFile, configDir, '/') ?: continue
                val normalized = relPath.removePrefix("./").removePrefix("/")

                // Fast index check: any entry in this config file points to this path?
                val entryNames = FileBasedIndex.getInstance()
                    .getValues(ViteEntryStubIndex.KEY, normalized, com.intellij.psi.search.GlobalSearchScope.fileScope(project, configVirtualFile))

                if (entryNames.isEmpty()) continue

                // Resolve PSI targets from the cached entry list
                ViteUtil.getEntries(project)
                    .filter { it.configFile == configVirtualFile && it.name in entryNames }
                    .mapNotNullTo(targets) { it.psiElement }
            }

            if (targets.isEmpty()) continue

            // LineMarkerInfo requires a leaf element — walk down to the first leaf token
            val anchor = PsiTreeUtil.getDeepestFirst(element)

            result.add(
                NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
                    .setTargets(targets)
                    .setTooltipText("Vite entry point")
                    .setPopupTitle("Vite Entry")
                    .createLineMarkerInfo(anchor)
            )
        }
    }
}
