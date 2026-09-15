package fr.adrienbrault.idea.symfony2plugin.navigation.controller

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigControllerStubIndex
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import icons.TwigIcons
import org.apache.commons.lang3.StringUtils

/**
 * Attach "Foo\FoobarController::FooAction" to its controller
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigControllerUsageControllerRelatedGotoCollector : ControllerActionGotoRelatedCollector {
    override fun collectGotoRelatedItems(parameter: ControllerActionGotoRelatedCollectorParameter) {
        val method = parameter.method
        val containingClass = method.containingClass ?: return

        val controllerAction = StringUtils.stripStart(containingClass.presentableFQN, "\\") + "::" + method.name

        val targets = HashSet<VirtualFile>()
        FileBasedIndex.getInstance().getFilesWithKey(
            TwigControllerStubIndex.KEY,
            hashSetOf(controllerAction),
            { virtualFile ->
                targets.add(virtualFile)
                true
            },
            GlobalSearchScope.getScopeRestrictedByFileTypes(
                GlobalSearchScope.allScope(parameter.project),
                TwigFileType.INSTANCE,
            ),
        )

        for (psiFile in PsiElementUtils.convertVirtualFilesToPsiFiles(parameter.project, targets)) {
            TwigUtil.visitControllerFunctions(psiFile) { pair ->
                if (pair.first.equals(controllerAction, ignoreCase = true)) {
                    parameter.add(
                        RelatedPopupGotoLineMarker.PopupGotoRelatedItem(pair.second)
                            .withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER),
                    )
                }
            }
        }
    }
}
