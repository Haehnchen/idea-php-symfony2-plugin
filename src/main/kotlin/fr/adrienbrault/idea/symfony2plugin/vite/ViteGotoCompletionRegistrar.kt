package fr.adrienbrault.idea.symfony2plugin.vite

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import org.apache.commons.lang3.StringUtils

/**
 * Provides completion and navigation for Vite entry points in Twig templates:
 * - vite_entry_link_tags()
 * - vite_entry_script_tags()
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ViteGotoCompletionRegistrar : GotoCompletionRegistrar {

    override fun register(registrar: GotoCompletionRegistrarParameter) {
        registrar.register(
            TwigPattern.getPrintBlockOrTagFunctionPattern("vite_entry_link_tags", "vite_entry_script_tags")
        ) { element ->
            if (!Symfony2ProjectComponent.isEnabled(element)) null
            else ViteEntryProvider(element)
        }
    }

    private class ViteEntryProvider(element: PsiElement) : GotoCompletionProvider(element) {

        override fun getLookupElements(): Collection<LookupElement> {
            val seen = mutableSetOf<String>()
            return ViteUtil.getEntries(project).mapNotNull { entry ->
                if (!seen.add(entry.name)) return@mapNotNull null
                LookupElementBuilder.create(entry.name)
                    .withIcon(Symfony2Icons.SYMFONY)
                    .withTypeText(entry.targetPath ?: entry.configFile.name)
            }
        }

        override fun getPsiTargets(element: PsiElement): Collection<PsiElement> {
            val contents = element.text
            if (StringUtils.isBlank(contents)) return emptyList()

            val targets = mutableSetOf<PsiElement>()

            for (entry in ViteUtil.getEntries(project)) {
                if (entry.name == contents) {
                    entry.targetPath?.let { path ->
                        val normalized = path.replace("\\", "/").removePrefix("./")
                        val relativeFile = VfsUtil.findRelativeFile(entry.configFile.parent, *normalized.split("/").toTypedArray())
                        if (relativeFile != null) {
                            PsiManager.getInstance(project).findFile(relativeFile)?.let { targets.add(it) }
                        }
                    }

                    entry.psiElement?.let { targets.add(it) }
                        ?: PsiManager.getInstance(project).findFile(entry.configFile)?.let { targets.add(it) }
                }
            }

            return targets
        }
    }
}
