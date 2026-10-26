package fr.adrienbrault.idea.symfony2plugin.form

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionContributor
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionLookupVisitor
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionTargetVisitor
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionVisitor
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.PhpAttributeIndexer.AS_FORM_TYPE_ATTRIBUTE
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil

/**
 * Completion and navigation for options in Symfony form type attributes.
 *
 * ```php
 * #[AsFormType(options: ['label' => 'Account'])]
 * class AccountData {}
 * ```
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class AsFormTypeAttributeOptionGotoCompletionRegistrar : GotoCompletionRegistrar {
    override fun register(registrar: GotoCompletionRegistrarParameter) {
        registrar.register(
            PlatformPatterns.psiElement().withLanguage(PhpLanguage.INSTANCE).withParent(StringLiteralExpression::class.java),
            AsFormTypeOptionCompletionContributor()
        )
    }

    private class AsFormTypeOptionCompletionContributor : GotoCompletionContributor {
        override fun getProvider(psiElement: PsiElement): GotoCompletionProvider? {
            if (!Symfony2ProjectComponent.isEnabled(psiElement)) return null

            val options = PhpElementsUtil.getCompletableArrayCreationElement(psiElement.parent) ?: return null
            val parameterList = options.parent as? ParameterList ?: return null
            val attribute = parameterList.parent as? PhpAttribute ?: return null
            if (attribute.fqn != AS_FORM_TYPE_ATTRIBUTE
                || PhpPsiAttributesUtil.getAttributeValuePsiElement(attribute, 0, "options") !== options
            ) return null

            val phpClass = attribute.parent.parent as? PhpClass ?: return null
            return AsFormTypeOptionCompletionProvider(psiElement, phpClass)
        }
    }

    private class AsFormTypeOptionCompletionProvider(element: PsiElement, private val phpClass: PhpClass) :
        GotoCompletionProvider(element) {

        override fun getLookupElements(): Collection<LookupElement> {
            val elements = mutableListOf<LookupElement>()
            visitOptions(FormOptionLookupVisitor(elements))
            return elements.distinctBy { it.lookupString }
        }

        override fun getPsiTargets(element: PsiElement): Collection<PsiElement> {
            val literal = element.parent as? StringLiteralExpression ?: return emptyList()
            if (literal.contents.isBlank()) return emptyList()

            val targets = mutableSetOf<PsiElement>()
            visitOptions(FormOptionTargetVisitor(literal.contents, targets))
            return targets
        }

        private fun visitOptions(visitor: FormOptionVisitor) {
            val allowedOptions = FormOptionVisitor { target, option, formClass, kind ->
                if (option != "data_class") visitor.visit(target, option, formClass, kind)
            }

            FormOptionsUtil.visitFormOptions(project, phpClass.fqn, allowedOptions)
        }
    }
}
