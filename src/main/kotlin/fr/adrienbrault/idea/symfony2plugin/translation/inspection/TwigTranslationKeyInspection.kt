package fr.adrienbrault.idea.symfony2plugin.translation.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyIntentionAndQuickFixAction
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils.isInterpolatedString
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTranslationKeyInspection : LocalInspectionTool() {
    companion object {
        const val MESSAGE = "Symfony: Missing translation key"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyTranslationKeyPsiElementVisitor(holder)
    }

    private class MyTranslationKeyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var translationKeyPattern: ElementPattern<PsiElement>? = null

        override fun visitElement(psiElement: PsiElement) {
            if (!getTranslationKeyPattern().accepts(psiElement)) {
                super.visitElement(psiElement)
                return
            }

            val text = psiElement.text
            if (StringUtils.isBlank(text) || isInterpolatedString(text)) {
                super.visitElement(psiElement)
                return
            }

            // get domain on file scope or method parameter
            val domainName = TwigUtil.getPsiElementTranslationDomain(psiElement)

            if (TranslationUtil.hasTranslationKey(psiElement.project, text, domainName)) {
                super.visitElement(psiElement)
                return
            }

            holder.registerProblem(
                psiElement,
                MESSAGE,
                TranslationKeyIntentionAndQuickFixAction(text, domainName),
                TranslationKeyGuessTypoQuickFix(text, domainName)
            )

            super.visitElement(psiElement)
        }

        private fun getTranslationKeyPattern(): ElementPattern<PsiElement> {
            val pattern = translationKeyPattern ?: TwigPattern.getTranslationKeyPattern("trans", "transchoice")
            translationKeyPattern = pattern
            return pattern
        }
    }
}
