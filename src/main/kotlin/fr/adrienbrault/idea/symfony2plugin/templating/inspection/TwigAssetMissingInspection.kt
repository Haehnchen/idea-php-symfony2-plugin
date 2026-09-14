package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.jetbrains.twig.TwigTokenTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import org.apache.commons.lang3.StringUtils

/**
 * asset('<caret>')
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TwigAssetMissingInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var assetPattern: ElementPattern<PsiElement>? = null

        override fun visitElement(element: PsiElement) {
            if (element !is LeafPsiElement || element.node.elementType !== TwigTokenTypes.STRING_TEXT) {
                super.visitElement(element)
                return
            }

            if (getAssetPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                invoke(element, holder)
            }

            super.visitElement(element)
        }

        private fun invoke(element: PsiElement, holder: ProblemsHolder) {
            val asset = element.text

            if (StringUtils.isBlank(asset) || TwigUtil.resolveAssetsFiles(element.project, asset).isNotEmpty()) {
                return
            }

            holder.registerProblem(element, "Missing asset")
        }

        private fun getAssetPattern(): ElementPattern<PsiElement> {
            if (assetPattern != null) {
                return assetPattern!!
            }

            assetPattern = TwigPattern.getAutocompletableAssetPattern()
            return assetPattern!!
        }
    }
}
