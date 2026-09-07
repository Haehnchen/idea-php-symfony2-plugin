package fr.adrienbrault.idea.symfony2plugin.form.usages

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usages.PsiElementUsageTarget
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.rules.UsageType
import com.intellij.usages.impl.rules.UsageTypeProviderEx
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent

class FormFieldUsageTypeProvider : UsageTypeProviderEx {
    override fun getUsageType(element: PsiElement): UsageType? = null

    override fun getUsageType(element: PsiElement, targets: Array<out UsageTarget>): UsageType? =
        ReadAction.computeBlocking<UsageType?, RuntimeException> {
            if (!Symfony2ProjectComponent.isEnabled(element.project)) return@computeBlocking null

            val literal = PsiTreeUtil.getParentOfType(element, StringLiteralExpression::class.java, false)
                ?: return@computeBlocking null

            for (target in targets) {
                val member = (target as? PsiElementUsageTarget)?.element as? PhpNamedElement ?: continue
                if (!isSupportedFormTarget(member)) continue

                val literals = CachedValuesManager.getCachedValue(member) {
                    val found = hashSetOf<StringLiteralExpression>()
                    processFormFieldUsages(member) { found.add(it); true }
                    CachedValueProvider.Result.create(found, PsiModificationTracker.MODIFICATION_COUNT)
                }

                if (literal in literals) return@computeBlocking FORM_FIELD
            }

            null
        }
}

private val FORM_FIELD = UsageType { "Symfony form field" }
