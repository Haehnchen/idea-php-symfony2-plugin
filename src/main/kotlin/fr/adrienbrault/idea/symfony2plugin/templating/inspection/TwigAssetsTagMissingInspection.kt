package fr.adrienbrault.idea.symfony2plugin.templating.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.twig.assets.TwigNamedAssetsServiceParser
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory

/**
 * {% javascripts
 *   "@MainBundle/Resources/public/colorbox/colorbox.css"
 * %}
 *
 * {% stylesheets
 *   "@MainBundle/Resources/public/colorbox/colorbox.css"
 * %}
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class TwigAssetsTagMissingInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var stylesheetsPattern: ElementPattern<*>? = null
        private var javascriptsPattern: ElementPattern<*>? = null

        override fun visitElement(element: PsiElement) {
            if (getStylesheetsPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                val templateName = element.text
                if (!isKnownAssetFileOrFolder(element, templateName, *TwigUtil.CSS_FILES_EXTENSIONS)) {
                    holder.registerProblem(element, "Missing asset")
                }
            } else if (getJavascriptsPattern().accepts(element) && TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)) {
                val templateName = element.text
                if (!isKnownAssetFileOrFolder(element, templateName, *TwigUtil.JS_FILES_EXTENSIONS)) {
                    holder.registerProblem(element, "Missing asset")
                }
            }

            super.visitElement(element)
        }

        private fun getStylesheetsPattern(): ElementPattern<*> {
            return stylesheetsPattern ?: TwigPattern.getAutocompletableAssetTag("stylesheets").also { stylesheetsPattern = it }
        }

        private fun getJavascriptsPattern(): ElementPattern<*> {
            return javascriptsPattern ?: TwigPattern.getAutocompletableAssetTag("javascripts").also { javascriptsPattern = it }
        }
    }
}

private fun isKnownAssetFileOrFolder(element: PsiElement, templateName: String, vararg fileTypes: String): Boolean {
    // custom assets
    if (templateName.startsWith("@") && templateName.length > 1) {
        val twigPathServiceParser = ServiceXmlParserFactory.getInstance(element.project, TwigNamedAssetsServiceParser::class.java)
        val strings = twigPathServiceParser.namedAssets.keys
        if (strings.contains(templateName.substring(1))) {
            return true
        }
    }

    return TwigUtil.resolveAssetsFiles(element.project, templateName, *fileTypes).isNotEmpty()
}
