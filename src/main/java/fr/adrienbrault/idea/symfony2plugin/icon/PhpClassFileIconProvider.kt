package fr.adrienbrault.idea.symfony2plugin.icon

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.ElementBase
import com.intellij.ui.IconManager
import com.intellij.ui.LayeredIcon
import com.jetbrains.php.lang.PhpFileType
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.PhpPsiUtil
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpModifier
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.doctrine.DoctrineUtil
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import javax.swing.Icon
import javax.swing.SwingConstants

/**
 * Decorates Symfony-related PHP class file icons while keeping PHP's own class icon as the base.
 */
class PhpClassFileIconProvider : FileIconProvider {

    override fun getIcon(virtualFile: VirtualFile, flags: Int, project: Project?): Icon? {
        if (!isSupportedRequest(virtualFile, project)) {
            return null
        }

        val currentProject = project ?: return null
        val psiFile = PsiManager.getInstance(currentProject).findFile(virtualFile) as? PhpFile ?: return null
        val phpClass = extractClass(psiFile) ?: return null

        val badgeIcon = getBadgeIcon(psiFile, phpClass) ?: return null
        val icon = createDecoratedFileIcon(phpClass.icon, badgeIcon)
        return IconManager.getInstance().createLayeredIcon(psiFile, icon, ElementBase.transformFlags(psiFile, flags))
    }

    private fun isSupportedRequest(virtualFile: VirtualFile, project: Project?): Boolean =
        project != null &&
            Symfony2ProjectComponent.isEnabled(project) &&
            Settings.getInstance(project).featurePhpClassIcon &&
            !DumbService.getInstance(project).isDumb &&
            FileTypeRegistry.getInstance().isFileOfType(virtualFile, PhpFileType.INSTANCE)

    private fun extractClass(file: PhpFile): PhpClass? {
        val classes = PhpPsiUtil.findAllClasses(file)
        if (classes.size != 1) {
            return null
        }

        val phpClass = classes.first()
        // Keep PHP core file icon behavior: only decorate files whose name matches the class name.
        return if (StringUtil.containsIgnoreCase(file.name, phpClass.name)) phpClass else null
    }

    private fun getBadgeIcon(file: PhpFile, phpClass: PhpClass): Icon? =
        when {
            isSymfonyCommand(phpClass) -> Symfony2Icons.SYMFONY_COMMAND_FILE
            isSymfonyFormType(phpClass) -> Symfony2Icons.FORM_TYPE_FILE
            isSymfonyController(phpClass) -> Symfony2Icons.CONTROLLER_FILE
            isDoctrineEntity(file, phpClass) -> Symfony2Icons.DOCTRINE_ENTITY_FILE
            isDoctrineRepository(phpClass) -> Symfony2Icons.DOCTRINE_REPOSITORY_FILE
            else -> null
        }

    private fun isSymfonyCommand(phpClass: PhpClass): Boolean =
        phpClass.getAttributes("\\Symfony\\Component\\Console\\Attribute\\AsCommand").isNotEmpty()

    private fun isSymfonyFormType(phpClass: PhpClass): Boolean =
        PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Form\\AbstractType") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Form\\FormTypeInterface")

    private fun isSymfonyController(phpClass: PhpClass): Boolean =
        PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller") ||
            hasRouteAttribute(phpClass) ||
            phpClass.ownMethods.any { hasRouteAttribute(it) }

    private fun hasRouteAttribute(phpClass: PhpClass): Boolean =
        RouteHelper.ROUTE_ANNOTATIONS.any { phpClass.getAttributes(it).isNotEmpty() }

    private fun hasRouteAttribute(method: Method): Boolean =
        method.access == PhpModifier.Access.PUBLIC &&
            !method.isStatic &&
            RouteHelper.ROUTE_ANNOTATIONS.any { method.getAttributes(it).isNotEmpty() }

    private fun isDoctrineEntity(file: PhpFile, phpClass: PhpClass): Boolean {
        val className = phpClass.fqn.trimStart('\\')
        return DoctrineUtil.getClassRepositoryPair(file).any { it.className() == className }
    }

    private fun isDoctrineRepository(phpClass: PhpClass): Boolean =
        DoctrineMetadataUtil.findMetadataForRepositoryClass(phpClass).isNotEmpty() ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\ORM\\EntityRepository") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Bundle\\DoctrineBundle\\Repository\\ServiceEntityRepository") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Common\\Persistence\\ObjectRepository") ||
            PhpElementsUtil.isInstanceOf(phpClass, "\\Doctrine\\Persistence\\ObjectRepository")
}

internal fun createDecoratedFileIcon(baseIcon: Icon, badgeIcon: Icon): Icon =
    LayeredIcon.layeredIcon(arrayOf(baseIcon, badgeIcon)).also {
        it.setIcon(badgeIcon, 1, SwingConstants.SOUTH_EAST)
    }
