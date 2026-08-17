package fr.adrienbrault.idea.symfony2plugin.intentions.php

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpAttributesOwner
import com.jetbrains.php.lang.psi.elements.PhpModifier
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import de.espend.idea.php.annotation.util.AnnotationUtil
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import icons.SymfonyIcons
import javax.swing.Icon

/**
 * Map of FQN (without leading backslash) to variable name
 */
private val AVAILABLE_ROUTE_ACTION_PARAMETERS = linkedMapOf(
    "Symfony\\Component\\HttpFoundation\\Request" to "request",
    "Symfony\\Component\\Security\\Core\\User\\UserInterface" to "user",
)

/**
 * Formats a FQN for display as "ClassName (namespace)"
 */
private fun formatRouteActionParameterDisplayName(fqn: String): String {
    val lastBackslash = fqn.lastIndexOf('\\')
    if (lastBackslash == -1) {
        return fqn
    }
    val className = fqn.substring(lastBackslash + 1)
    val namespace = fqn.substring(0, lastBackslash)
    return "$className ($namespace)"
}

/**
 * Returns the list of FQNs (without leading backslash) that are available to be added to the given method.
 */
fun getAvailableRouteActionParameterFqns(method: Method): List<String> {
    val existingParameterTypes = getExistingRouteActionParameterTypes(method)

    val availableFqns = mutableListOf<String>()
    for (fqn in AVAILABLE_ROUTE_ACTION_PARAMETERS.keys) {
        if (!existingParameterTypes.contains("\\$fqn")) {
            availableFqns.add(fqn)
        }
    }

    return availableFqns
}

private fun getExistingRouteActionParameterTypes(method: Method): Set<String> {
    val types = mutableSetOf<String>()
    for (parameter in method.parameters) {
        types.addAll(parameter.declaredType.types)
    }
    return types
}

/**
 * Checks if the method is a route action.
 * A method is a route action if:
 * - It has a Route attribute/annotation directly on the method, OR
 * - The method is __invoke and the class has a Route attribute/annotation
 */
private fun isMethodARouteAction(method: Method): Boolean {
    // Check if method has Route attribute or annotation
    if (hasRouteAnnotationOrAttribute(method)) {
        return true
    }

    // For __invoke methods, also check if class has Route attribute/annotation
    if ("__invoke" == method.name) {
        val phpClass = method.containingClass
        if (phpClass != null && hasRouteAnnotationOrAttribute(phpClass)) {
            return true
        }
    }

    return false
}

private fun hasRouteAnnotationOrAttribute(method: PhpAttributesOwner): Boolean {
    // Check for Route attributes
    for (route in RouteHelper.ROUTE_ANNOTATIONS) {
        if (method.getAttributes(route).isNotEmpty()) {
            return true
        }
    }

    // Check for Route annotations in PHPDoc
    if (method is PhpNamedElement) {
        val docComment = method.docComment
        if (docComment != null) {
            return AnnotationUtil.getPhpDocCommentAnnotationContainer(docComment)!!.getFirstPhpDocBlock(*RouteHelper.ROUTE_ANNOTATIONS) != null
        }
    }

    return false
}

/**
 * Intention action to add parameters to route action methods (e.g., Request).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class RouteActionParameterIntention : PsiElementBaseIntentionAction(), Iconable {
    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val method = PsiTreeUtil.getParentOfType(psiElement, Method::class.java)
            ?: return

        val availableFqns = getAvailableRouteActionParameterFqns(method)
        if (availableFqns.isEmpty()) {
            return
        }

        // Build display names: "ClassName (namespace)"
        val displayNames = mutableListOf<String>()
        val displayToFqn = linkedMapOf<String, String>()
        for (fqn in availableFqns) {
            val displayName = formatRouteActionParameterDisplayName(fqn)
            displayNames.add(displayName)
            displayToFqn[displayName] = fqn
        }

        JBPopupFactory.getInstance().createPopupChooserBuilder(displayNames)
            .setTitle("Symfony: Add Parameter to Route Action")
            .setItemChosenCallback { selectedDisplay ->
                WriteCommandAction.writeCommandAction(project)
                    .withName("Add Route Action Parameter")
                    .run<RuntimeException> {
                        val fqn = displayToFqn[selectedDisplay]
                        val variableName = if (fqn != null) AVAILABLE_ROUTE_ACTION_PARAMETERS[fqn] else null
                        if (variableName != null && fqn != null) {
                            PhpElementsUtil.addParameterToMethod(method, "\\$fqn", variableName)
                        }
                    }
            }
            .createPopup()
            .showInBestPositionFor(editor!!)
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return false
        }

        val method = PsiTreeUtil.getParentOfType(psiElement, Method::class.java)
            ?: return false

        if (method.access != PhpModifier.Access.PUBLIC) {
            return false
        }

        if (!isMethodARouteAction(method)) {
            return false
        }

        return getAvailableRouteActionParameterFqns(method).isNotEmpty()
    }

    override fun getFamilyName(): String {
        return "Symfony: Add parameter to route action"
    }

    override fun getText(): String {
        return familyName
    }

    override fun getIcon(flags: Int): Icon {
        return SymfonyIcons.Symfony
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }
}
