package fr.adrienbrault.idea.symfony2plugin.twig.icon

import com.intellij.ide.IconProvider
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.LayeredIcon
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.twig.TwigFile
import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import icons.TwigIcons
import javax.swing.Icon
import javax.swing.SwingConstants

private const val FLAG_CONTROLLER = 1
private const val FLAG_EXTENDS = 2
private const val FLAG_INCLUDE = 4

/**
 * Pre-built [LayeredIcon] instances for all 8 badge bitmask combinations.
 * Index 0 = no badges → null. Bits: 0 = controller, 1 = extends, 2 = include.
 *
 * Lazy so icon resources are only loaded on first access (after the platform is ready).
 */
private val TWIG_BADGE_ICONS: Array<Icon?> by lazy {
    Array(8) { key -> if (key == 0) null else buildTwigBadgeIcon(key) }
}

private fun buildTwigBadgeIcon(key: Int): Icon {
    val badges = buildList {
        if (key and FLAG_CONTROLLER != 0) add(Symfony2Icons.TWIG_CONTROLLER_FILE to SwingConstants.NORTH_WEST)
        if (key and FLAG_EXTENDS != 0) add(Symfony2Icons.TWIG_EXTENDS_FILE to SwingConstants.SOUTH_WEST)
        if (key and FLAG_INCLUDE != 0) add(Symfony2Icons.TWIG_IMPLEMENTS_FILE to SwingConstants.SOUTH_EAST)
    }

    val layers = arrayOfNulls<Icon>(badges.size + 1)
    layers[0] = TwigIcons.TwigFileIcon
    badges.forEachIndexed { i, (icon, _) -> layers[i + 1] = icon }

    @Suppress("UNCHECKED_CAST")
    return LayeredIcon.layeredIcon(layers as Array<Icon>).also { layered ->
        badges.forEachIndexed { i, (icon, position) -> layered.setIcon(icon, i + 1, position) }
    }
}

/**
 * Based on Twig content add overlay to the default Twig file icon, indicating the possible template type.
 *
 * Icon positions:
 * - Controller: top left (NORTH_WEST)
 * - Extends: bottom left (SOUTH_WEST)
 * - Include: bottom right (SOUTH_EAST)
 *
 * All 7 possible badge combinations are pre-built as file-level instances (bitmask index 1–7)
 * to avoid repeated [LayeredIcon] instantiation on every file tree repaint.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigIconProvider : IconProvider() {

    override fun getIcon(element: PsiElement, @Iconable.IconFlags flags: Int): Icon? {
        val twigFile = element as? TwigFile ?: return null
        val project = twigFile.project

        if (!Settings.getInstance(project).featureTwigIcon
            || !Symfony2ProjectComponent.isEnabled(project)
            || DumbService.getInstance(project).isDumb
        ) return null

        val virtualFile = twigFile.virtualFile ?: return null
        val templateNames = LinkedHashSet(TwigUtil.getTemplateNamesForFile(project, virtualFile))
        if (templateNames.isEmpty()) return null

        val key = (if (hasController(twigFile, templateNames)) FLAG_CONTROLLER else 0) or
                  (if (hasFileExtendsTag(project, virtualFile)) FLAG_EXTENDS else 0) or
                  (if (isIncludedByOtherTemplates(project, templateNames)) FLAG_INCLUDE else 0)

        return TWIG_BADGE_ICONS[key]
    }

    private fun hasFileExtendsTag(project: Project, virtualFile: VirtualFile): Boolean =
        FileBasedIndex.getInstance().getFileData(TwigExtendsStubIndex.KEY, virtualFile, project).isNotEmpty()

    private fun hasController(twigFile: TwigFile, templateNames: Collection<String>): Boolean =
        TwigUtil.getTwigFileMethodUsageOnIndex(twigFile.project, templateNames).isNotEmpty()
            || TwigUtil.findTwigFileController(twigFile).isNotEmpty()

    private fun isIncludedByOtherTemplates(project: Project, templateNames: Collection<String>): Boolean {
        val scope = GlobalSearchScope.getScopeRestrictedByFileTypes(
            GlobalSearchScope.allScope(project),
            TwigFileType.INSTANCE
        )
        return templateNames.any { name ->
            FileBasedIndex.getInstance().getContainingFiles(TwigIncludeStubIndex.KEY, name, scope).isNotEmpty()
        }
    }
}
