package fr.adrienbrault.idea.symfony2plugin.navigation.controller

import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class FormControllerRelatedGotoCollector : ControllerActionGotoRelatedCollector {
    override fun collectGotoRelatedItems(parameter: ControllerActionGotoRelatedCollectorParameter) {
        val uniqueTargets = HashSet<PhpClass>()

        for (methodReference in PhpElementsUtil.collectMethodReferencesInsideControlFlow(parameter.method)) {
            val parameter0 = methodReference.getParameter(0) ?: continue

            val matchedSignature = MethodMatcher.getMatchedSignatureWithDepth(parameter0, FormUtil.FORM_FACTORY_SIGNATURES)
            if (matchedSignature != null) {
                val formTypeToClass = FormUtil.getFormTypeClassOnParameter(parameter0)
                if (formTypeToClass != null) {
                    uniqueTargets.add(formTypeToClass)
                }
            }

            val parameter1 = methodReference.getParameter(1)
            if (parameter1 != null) {
                val matchedSignature1 = MethodMatcher.getMatchedSignatureWithDepth(
                    parameter1,
                    FormUtil.PHP_FORM_NAMED_BUILDER_SIGNATURES,
                    1,
                )
                if (matchedSignature1 != null) {
                    val formTypeToClass = FormUtil.getFormTypeClassOnParameter(parameter1)
                    if (formTypeToClass != null) {
                        uniqueTargets.add(formTypeToClass)
                    }
                }
            }
        }

        for (phpClass in uniqueTargets) {
            parameter.add(
                RelatedPopupGotoLineMarker.PopupGotoRelatedItem(
                    phpClass,
                    StringUtils.stripStart(phpClass.fqn, "\\"),
                ).withIcon(Symfony2Icons.FORM_TYPE, Symfony2Icons.FORM_TYPE_LINE_MARKER),
            )
        }
    }
}
