package fr.adrienbrault.idea.symfony2plugin.navigation.controller

import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpMethodVariableResolveUtil
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import icons.TwigIcons

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TemplatesControllerRelatedGotoCollector : ControllerActionGotoRelatedCollector {
    override fun collectGotoRelatedItems(parameter: ControllerActionGotoRelatedCollectorParameter) {
        val uniqueTemplates = HashSet<String>()

        PhpMethodVariableResolveUtil.visitRenderTemplateFunctions(parameter.method) { triple ->
            uniqueTemplates.add(triple.first)
        }

        for (uniqueTemplate in uniqueTemplates) {
            for (templateTarget in TwigUtil.getTemplatePsiElements(parameter.project, uniqueTemplate)) {
                parameter.add(
                    RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templateTarget, uniqueTemplate)
                        .withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER),
                )
            }
        }
    }
}
