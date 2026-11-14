package fr.adrienbrault.idea.symfony2plugin.translation.usages

import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageTargetProvider

/**
 * Find Usages for translation keys, PHP calls and form labels.
 *
 * ```php
 * $translator->trans('notification.summary');
 * new TranslatableMessage('notification.summary');
 * $builder->add('summary', TextType::class, ['label' => 'notification.summary']);
 * ```
 */
class TranslationKeyUsageTargetProvider : UsageTargetProvider {
    override fun getTargets(element: PsiElement): Array<UsageTarget>? {
        val declarations = TranslationKeyUsageUtil.getSearchDeclarations(element)
        return declarations.takeIf { it.isNotEmpty() }
            ?.map { PsiElement2UsageTargetAdapter(it.element, true) }
            ?.toTypedArray()
    }

    override fun getTargets(editor: Editor, file: PsiFile): Array<UsageTarget>? {
        val element = file.findElementAt(editor.caretModel.offset) ?: return null
        return getTargets(element)
    }
}
