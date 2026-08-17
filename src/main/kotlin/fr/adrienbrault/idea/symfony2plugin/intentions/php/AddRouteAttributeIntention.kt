package fr.adrienbrault.idea.symfony2plugin.intentions.php

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.completion.PhpAttributeScopeValidator
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import icons.SymfonyIcons
import javax.swing.Icon

private const val ROUTE_ATTRIBUTE_CLASS = "\\Symfony\\Component\\Routing\\Attribute\\Route"

/**
 * Intention action to add #[\Symfony\Component\Routing\Attribute\Route] attribute to a public method in a controller
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class AddRouteAttributeIntention : PsiElementBaseIntentionAction(), Iconable {
    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val method = PsiTreeUtil.getParentOfType(psiElement, Method::class.java)
            ?: return

        val phpClass = method.containingClass
            ?: return

        var routeName = AnnotationBackportUtil.getRouteByMethod(method)
        if (routeName == null) {
            routeName = ""
        }

        var routePath = AnnotationBackportUtil.getRoutePathByMethod(method)
        if (routePath == null) {
            routePath = "/"
        }

        val document = PsiDocumentManager.getInstance(project).getDocument(method.containingFile)
            ?: return

        val psiDocManager = PsiDocumentManager.getInstance(project)

        var importedRouteName = PhpElementsUtil.insertUseIfNecessary(phpClass, ROUTE_ATTRIBUTE_CLASS)
        if (importedRouteName == null) {
            importedRouteName = "Route"
        }

        psiDocManager.doPostponedOperationsAndUnblockDocument(document)

        val methodStartOffset = method.textRange.startOffset
        val attributePrefix = "#[${importedRouteName}('"
        val attributeText = "${attributePrefix}${routePath}', name: '${routeName}')]\n"

        document.insertString(methodStartOffset, attributeText)

        psiDocManager.commitDocument(document)
        psiDocManager.doPostponedOperationsAndUnblockDocument(document)

        CodeUtil.reformatAddedAttribute(project, document, methodStartOffset)

        // position caret after the opening quote of the path: #[Route('<caret>/...
        if (editor != null) {
            val caretOffset = methodStartOffset + attributePrefix.length
            editor.caretModel.moveToOffset(caretOffset)
        }
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        val method = PsiTreeUtil.getParentOfType(psiElement, Method::class.java)
            ?: return false

        if (!method.access.isPublic || method.isStatic) {
            return false
        }

        val phpClass = method.containingClass
            ?: return false

        if (hasRouteAttribute(method)) {
            return false
        }

        if (!PhpAttributeScopeValidator.isControllerClass(phpClass)) {
            return false
        }

        return PhpElementsUtil.hasClassOrInterface(project, ROUTE_ATTRIBUTE_CLASS)
    }

    private fun hasRouteAttribute(method: Method): Boolean {
        for (routeAnnotation in RouteHelper.ROUTE_ANNOTATIONS) {
            val attributes = method.getAttributes(routeAnnotation)
            if (attributes.isNotEmpty()) {
                return true
            }
        }
        return false
    }

    override fun getFamilyName(): String {
        return "Symfony: Add Route attribute"
    }

    override fun getText(): String {
        return familyName
    }

    override fun getIcon(flags: Int): Icon {
        return SymfonyIcons.Symfony
    }
}
