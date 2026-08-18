package fr.adrienbrault.idea.symfony2plugin.translation

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.ParameterListOwner
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TranslationKeyGuessTypoQuickFix
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TwigTranslationKeyInspection
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class PhpTranslationKeyInspection : LocalInspectionTool() {
    companion object {
        const val MESSAGE = TwigTranslationKeyInspection.MESSAGE
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is StringLiteralExpression) {
                    inspectTranslationKey(holder, element)
                }
                super.visitElement(element)
            }
        }
    }

    private fun inspectTranslationKey(holder: ProblemsHolder, psiElement: StringLiteralExpression) {
        val methodReferenceOrNewExpression: ParameterListOwner = TranslationUtil.getTranslationFunctionContext(psiElement)
            ?: return

        if (!PsiElementUtils.isCurrentParameter(psiElement, "id", 0)) {
            return
        }

        if (!TranslationUtil.isTranslationReference(methodReferenceOrNewExpression)) {
            return
        }

        val parameterList = psiElement.context as ParameterList
        val domainElement = parameterList.getParameter("domain", TranslationUtil.getDomainParameter(methodReferenceOrNewExpression))
        if (domainElement == null) {
            // no domain found; fallback to default domain
            annotateTranslationKey(psiElement, "messages", holder)
        } else {
            // resolve string in parameter
            val domain = PhpElementsUtil.getStringValue(domainElement)
            if (domain != null) {
                annotateTranslationKey(psiElement, domain, holder)
            }
        }
    }

    private fun annotateTranslationKey(psiElement: StringLiteralExpression, domainName: String, holder: ProblemsHolder) {
        val keyName = psiElement.contents

        // should not annotate "foo$bar"
        // @TODO: regular expression to only notice translation keys and not possible text values
        if (StringUtils.isBlank(keyName) || "$" in keyName) {
            return
        }

        // dont annotate non goto available keys
        if (TranslationUtil.hasTranslationKey(psiElement.project, keyName, domainName)) {
            return
        }

        holder.registerProblem(
            psiElement,
            MESSAGE,
            TranslationKeyIntentionAndQuickFixAction(keyName, domainName),
            TranslationKeyGuessTypoQuickFix(keyName, domainName)
        )
    }
}
