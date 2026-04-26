package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.twig.TwigFile
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import javax.swing.Icon


/**
 * Line marker for JavaScript/TypeScript files that are Vite entry points.
 *
 * When a JS/TS file is referenced in a vite.config.js/ts rollupOptions.input,
 * a gutter icon appears at the top of the file. The navigation popup shows:
 * - The JSProperty in the Vite config defining that entry
 * - All Twig templates that call vite_entry_link_tags / vite_entry_script_tags with that entry name
 *
 * Uses [ViteEntryStubIndex] for reverse lookup (file path → entry name) and
 * [ViteTwigUsageStubIndex] for finding Twig usages, both resolved lazily.
 *
 * Registered for JavaScript in plugin.xml (covers TypeScript as a dialect).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see ViteEntryStubIndex
 * @see ViteTwigUsageStubIndex
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

        var viteConfigFiles: Collection<VirtualFile>? = null

        for (element in elements) {
            // Only fire once per file — match on the JSFile node itself
            if (element !is JSFile) continue

            val virtualFile = element.virtualFile ?: continue
            val project = element.project
            val configs = viteConfigFiles ?: ViteUtil.getViteConfigFiles(project).also {
                if (it.isEmpty()) {
                    return
                }
                viteConfigFiles = it
            }

            val entryNames = mutableSetOf<String>()

            for (configVirtualFile in configs) {
                val configDir = configVirtualFile.parent ?: continue
                val relPath = VfsUtil.getRelativePath(virtualFile, configDir, '/') ?: continue
                val normalized = relPath.removePrefix("./").removePrefix("/")

                FileBasedIndex.getInstance()
                    .getValues(VITE_ENTRY_STUB_INDEX_KEY, normalized, GlobalSearchScope.fileScope(project, configVirtualFile))
                    .forEach { entryNames.add(it) }
            }

            if (entryNames.isEmpty()) continue

            val anchor = PsiTreeUtil.getDeepestFirst(element)

            result.add(
                NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
                    .setTargets(NotNullLazyValue.lazy { resolveTargets(element, entryNames) })
                    .setTooltipText("Vite entry point")
                    .setPopupTitle("Vite Entry")
                    .setTargetRenderer { ViteEntryTargetRenderer() }
                    .createLineMarkerInfo(anchor)
            )
        }
    }

    private fun resolveTargets(element: JSFile, entryNames: Set<String>): Collection<PsiElement> {
        val project = element.project
        val targets = mutableSetOf<PsiElement>()

        // Config JSProperty targets
        ViteUtil.getEntries(project)
            .filter { it.name in entryNames }
            .mapNotNullTo(targets) { it.psiElement }

        // Twig template targets via ViteTwigUsageStubIndex
        val psiManager = PsiManager.getInstance(project)
        for (entryName in entryNames) {
            FileBasedIndex.getInstance()
                .getContainingFiles(VITE_TWIG_USAGE_STUB_INDEX_KEY, entryName, GlobalSearchScope.projectScope(project))
                .forEach { twigVirtualFile ->
                    val psiFile = psiManager.findFile(twigVirtualFile)
                    if (psiFile is TwigFile) targets.add(psiFile)
                }
        }

        return targets
    }

    private class ViteEntryTargetRenderer : PsiTargetPresentationRenderer<PsiElement>() {
        override fun getElementText(element: PsiElement): String = when (element) {
            is JSProperty -> element.name ?: "?"
            is TwigFile -> element.name
            else -> element.containingFile?.name ?: "?"
        }

        override fun getContainerText(element: PsiElement): String? {
            val file = element.containingFile?.virtualFile ?: return null
            return VfsUtil.getRelativePath(file, ProjectUtil.getProjectDir(element), '/') ?: file.name
        }

        override fun getIcon(element: PsiElement): Icon? = element.getIcon(0)
    }
}
