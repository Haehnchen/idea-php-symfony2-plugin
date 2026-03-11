package fr.adrienbrault.idea.symfony2plugin.stimulus

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiElementPattern
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.xml.XmlFile
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.twig.TwigLanguage
import com.jetbrains.twig.TwigTokenTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.StimulusControllerStubIndex
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils

/**
 * Completion for Stimulus controllers in HTML and Twig.
 *
 * Supports:
 * - HTML: <div data-controller="|"> </div>
 * - Twig: {{ stimulus_controller('|') }}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class StimulusControllerCompletionRegistrar {

    /**
     * HTML: <div data-controller="|"> </div>
     */
    class HtmlDataControllerCompletionRegistrar : GotoCompletionRegistrar {
        override fun register(registrar: GotoCompletionRegistrarParameter) {
            registrar.register(htmlDataControllerPattern()) { psiElement ->
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) null
                else DataController(psiElement)
            }
        }
    }

    /**
     * Twig: {{ stimulus_controller('|') }}
     */
    class StimulusTwigCompletionRegistrar : GotoCompletionRegistrar {
        override fun register(registrar: GotoCompletionRegistrarParameter) {
            registrar.register(stimulusControllerPattern()) { psiElement ->
                if (!Symfony2ProjectComponent.isEnabled(psiElement)) null
                else StimulusTwigFunction(psiElement)
            }
        }
    }

    /**
     * HTML attribute value completion for data-controller
     */
    class DataController(element: PsiElement) : GotoCompletionProvider(element) {

        override fun getLookupElements(): Collection<LookupElement> {
            if (!Symfony2ProjectComponent.isEnabled(project)) return emptyList()

            return StimulusControllerCompletion.getAllControllers(project).values
                .map { StimulusControllerCompletion.createLookupElement(it, false) }
        }

        override fun getPsiTargets(element: PsiElement): Collection<PsiElement> {
            val controllerName = element.text.trim()
            if (controllerName.isEmpty()) return emptyList()

            return getNavigationTargets(project, controllerName)
        }
    }

    /**
     * Twig function completion for stimulus_controller()
     */
    class StimulusTwigFunction(element: PsiElement) : GotoCompletionProvider(element) {

        override fun getLookupElements(): Collection<LookupElement> {
            if (!Symfony2ProjectComponent.isEnabled(project)) return emptyList()

            return StimulusControllerCompletion.getAllControllers(project).values
                .map { StimulusControllerCompletion.createLookupElement(it, true) }
        }

        override fun getPsiTargets(element: PsiElement): Collection<PsiElement> {
            val controllerName = element.text.trim()
            if (controllerName.isEmpty()) return emptyList()

            return getNavigationTargets(project, resolveControllerName(controllerName))
        }

        private fun resolveControllerName(controllerName: String): String {
            return StimulusControllerCompletion.getAllControllers(project).values
                .firstOrNull { it.twigName == controllerName || it.normalizedName == controllerName }
                ?.normalizedName
                ?: controllerName
        }
    }

    companion object {

        private fun htmlDataControllerPattern(): PsiElementPattern.Capture<PsiElement> =
            PlatformPatterns
                .psiElement()
                .withParent(
                    XmlPatterns.xmlAttributeValue()
                        .withParent(XmlPatterns.xmlAttribute("data-controller"))
                )
                .inFile(XmlPatterns.psiFile(XmlFile::class.java))

        private fun stimulusControllerPattern(): PsiElementPattern.Capture<PsiElement> =
            PlatformPatterns
                .psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE),
                        PlatformPatterns.psiElement(PsiWhiteSpace::class.java),
                        PlatformPatterns.psiElement(TwigTokenTypes.WHITE_SPACE),
                        PlatformPatterns.psiElement(TwigTokenTypes.SINGLE_QUOTE),
                        PlatformPatterns.psiElement(TwigTokenTypes.DOUBLE_QUOTE)
                    ),
                    PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText("stimulus_controller")
                )
                .withLanguage(TwigLanguage.INSTANCE)

        @JvmStatic
        private fun getNavigationTargets(project: Project, normalizedKey: String): Collection<PsiElement> {
            val containingFiles: Collection<VirtualFile> = FileBasedIndex.getInstance().getContainingFiles(
                StimulusControllerStubIndex.KEY,
                normalizedKey,
                GlobalSearchScope.allScope(project)
            )

            return PsiElementUtils.convertVirtualFilesToPsiFiles(project, containingFiles)
        }
    }
}
