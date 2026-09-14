package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import org.apache.commons.lang3.StringUtils

/**
 * PHP version of "Twig" "asset" function
 *
 * "Package*::getUrl"
 * "Package*::getVersion"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class PhpAssetMissingInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return AssetPackageElementVisitor(holder)
    }

    private class AssetPackageElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is StringLiteralExpression) {
                val methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element)
                if (methodReference != null) {
                    val methodName = methodReference.name
                    if (methodName != null && (methodName == "getUrl" || methodName == "getVersion") &&
                        (PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Symfony\\Component\\Asset\\Packages", "getUrl") ||
                            PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Symfony\\Component\\Asset\\Packages", "getVersion") ||
                            PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Symfony\\Component\\Asset\\PackageInterface", "getUrl") ||
                            PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, "\\Symfony\\Component\\Asset\\PackageInterface", "getVersion"))
                    ) {
                        invoke(element, holder)
                    }
                }
            }
            super.visitElement(element)
        }

        private fun invoke(element: StringLiteralExpression, holder: ProblemsHolder) {
            val asset = element.contents
            if (StringUtils.isBlank(asset) || TwigUtil.resolveAssetsFiles(element.project, asset).isNotEmpty()) {
                return
            }

            holder.registerProblem(element, "Symfony: Missing asset")
        }
    }
}
