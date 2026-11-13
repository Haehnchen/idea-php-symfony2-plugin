package fr.adrienbrault.idea.symfony2plugin.translation

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression
import com.jetbrains.php.lang.psi.elements.ArrayHashElement
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.ParameterListOwner
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import fr.adrienbrault.idea.symfony2plugin.translation.inspection.TranslationParameterInspectionUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * $translator->trans('key', ['unknown' => 1]);
 * new TranslatableMessage('key', ['unknown' => 1]);
 */
class PhpTranslationParameterInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is ArrayCreationExpression) {
                    inspectParameters(holder, element)
                }

                super.visitElement(element)
            }
        }
    }

    private fun inspectParameters(holder: ProblemsHolder, parameters: ArrayCreationExpression) {
        val parameterList = parameters.context as? ParameterList ?: return
        val call = parameterList.context as? ParameterListOwner ?: return
        if (!TranslationUtil.isTranslationReference(call) || parameterList.getParameter("parameters", TranslationUtil.getParametersParameter(call)) != parameters) {
            return
        }

        val key = parameterList.getParameter(if (call is MethodReference) "id" else "message", 0)?.let(PhpElementsUtil::getStringValue) ?: return
        val domainElement = parameterList.getParameter("domain", TranslationUtil.getDomainParameter(call))
        val domain = if (domainElement == null) "messages" else PhpElementsUtil.getStringValue(domainElement) ?: return

        val keys = PsiTreeUtil.getChildrenOfTypeAsList(parameters, ArrayHashElement::class.java)
            .mapNotNull { it.key as? StringLiteralExpression }
            .filter { '$' !in it.contents }
            .associateBy { it.contents }

        TranslationParameterInspectionUtil.registerUnknownParameters(holder, key, domain, keys)
    }
}
