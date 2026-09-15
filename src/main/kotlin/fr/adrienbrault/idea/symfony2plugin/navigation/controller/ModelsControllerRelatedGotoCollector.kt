package fr.adrienbrault.idea.symfony2plugin.navigation.controller

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.config.SymfonyPhpReferenceContributor
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker
import fr.adrienbrault.idea.symfony2plugin.doctrine.EntityHelper
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ModelsControllerRelatedGotoCollector : ControllerActionGotoRelatedCollector {
    override fun collectGotoRelatedItems(parameter: ControllerActionGotoRelatedCollectorParameter) {
        val uniqueTargets = ArrayList<PsiElement>()

        for (psiElement in parameter.parameterLists) {
            val matchedSignature = MethodMatcher.getMatchedSignatureWithDepth(
                psiElement,
                SymfonyPhpReferenceContributor.REPOSITORY_SIGNATURES,
            )
            if (matchedSignature != null) {
                val resolveString = PhpElementsUtil.getStringValue(psiElement)
                if (resolveString != null) {
                    for (templateTarget in EntityHelper.getModelPsiTargets(parameter.project, resolveString)) {
                        if (!uniqueTargets.contains(templateTarget)) {
                            uniqueTargets.add(templateTarget)
                            // we can provide targets to model config and direct class targets
                            if (templateTarget is PsiFile) {
                                parameter.add(
                                    RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, resolveString)
                                        .withIcon(templateTarget.getIcon(0), Symfony2Icons.DOCTRINE_LINE_MARKER),
                                )
                            } else {
                                // @TODO: we can resolve for model types and provide icons, but not for now
                                parameter.add(
                                    RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, resolveString)
                                        .withIcon(Symfony2Icons.DOCTRINE, Symfony2Icons.DOCTRINE_LINE_MARKER),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
