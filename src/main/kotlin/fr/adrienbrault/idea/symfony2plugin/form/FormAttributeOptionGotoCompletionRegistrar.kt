package fr.adrienbrault.idea.symfony2plugin.form

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionLookupVisitor
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionTargetVisitor
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.PhpAttributeIndexer.FORM_FIELD_ATTRIBUTE
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil

/**
 * Completion and navigation for options in Symfony form field attributes.
 *
 * ```php
 * #[FormField(EmailType::class, options: ['required' => true])]
 * ```
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class FormAttributeOptionGotoCompletionRegistrar : GotoCompletionRegistrar {
    override fun register(registrar: GotoCompletionRegistrarParameter) {
        registrar.register(
            PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE).withParent(StringLiteralExpression::class.java),
            FormFieldOptionCompletionContributor()
        )
    }

    private class FormFieldOptionCompletionContributor : GotoCompletionContributor {
        override fun getProvider(psiElement: PsiElement): GotoCompletionProvider? {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) return null

            val options = PhpElementsUtil.getCompletableArrayCreationElement(psiElement.parent) ?: return null
            val parameterList = options.parent as? ParameterList ?: return null
            val attribute = parameterList.parent as? PhpAttribute ?: return null
            if (attribute.fqn != FORM_FIELD_ATTRIBUTE
                || PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 1, "options") !== options
            ) return null

            val type = PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 0, "type")
            val formType = type?.let(FormUtil::getFormTypeFqnOnParameter) ?: DEFAULT_FORM_TYPE
            return FormAttributeOptionCompletionProvider(psiElement, formType)
        }
    }

    private class FormAttributeOptionCompletionProvider(element: PsiElement, private val formType: String) :
        GotoCompletionProvider(element) {

        override fun getLookupElements(): Collection<LookupElement> {
            val elements = mutableListOf<LookupElement>()
            FormOptionsUtil.visitFormOptions(project, formType, FormOptionLookupVisitor(elements))
            return elements
        }

        override fun getPsiTargets(element: PsiElement): Collection<PsiElement> {
            val literal = element.parent as? StringLiteralExpression ?: return emptyList()
            if (literal.contents.isBlank()) return emptyList()

            val targets = mutableListOf<PsiElement>()
            FormOptionsUtil.visitFormOptions(project, formType, FormOptionTargetVisitor(literal.contents, targets))
            return targets
        }
    }

    companion object {
        private const val DEFAULT_FORM_TYPE = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\FormType"
    }
}
