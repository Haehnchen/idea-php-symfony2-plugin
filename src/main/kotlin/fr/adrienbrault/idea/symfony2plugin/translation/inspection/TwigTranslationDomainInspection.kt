package fr.adrienbrault.idea.symfony2plugin.translation.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.twig.TwigTokenTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTranslationDomainInspection : LocalInspectionTool() {
    companion object {
        const val MESSAGE = "Symfony: Missing translation domain"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyTranslationDomainPsiElementVisitor(holder)
    }

    /**
     * 'foo'|trans({}, 'foobar')
     * 'foo'|transchoice({}, null, 'foobar')
     */
    private class MyTranslationDomainPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var transDomainPattern: ElementPattern<PsiElement>? = null

        override fun visitElement(psiElement: PsiElement) {
            if (!getTransDomainPattern().accepts(psiElement)) {
                return
            }

            // @TODO: move to pattern, dont allow nested filters: eg "'form.tab.profile'|trans|desc('Interchange')"
            val psiElementTrans = arrayOfNulls<PsiElement>(1)
            PsiElementUtils.getPrevSiblingOnCallback(psiElement) { psiElement1 ->
                if (psiElement1.node.elementType == TwigTokenTypes.FILTER) {
                    false
                } else {
                    if (PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")).accepts(psiElement1)) {
                        psiElementTrans[0] = psiElement1
                    }

                    true
                }
            }

            val translationElement = psiElementTrans[0]
            if (translationElement != null && TwigUtil.getTwigMethodString(translationElement) != null) {
                val text = psiElement.text
                if (StringUtils.isNotBlank(text) && !TranslationUtil.hasDomain(psiElement.project, text)) {
                    holder.registerProblem(
                        psiElement,
                        MESSAGE,
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        TranslationDomainGuessTypoQuickFix(text)
                    )
                }
            }

            super.visitElement(psiElement)
        }

        private fun getTransDomainPattern(): ElementPattern<PsiElement> {
            val pattern = transDomainPattern ?: TwigPattern.getTransDomainPattern()
            transDomainPattern = pattern
            return pattern
        }
    }
}
