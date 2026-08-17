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
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import icons.SymfonyIcons
import javax.swing.Icon

private const val AS_COMMAND_ATTRIBUTE = "\\Symfony\\Component\\Console\\Attribute\\AsCommand"

/**
 * Map of FQN (without leading backslash) to variable name
 */
private val AVAILABLE_COMMAND_PARAMETERS = linkedMapOf(
    "Symfony\\Component\\Console\\Input\\InputInterface" to "input",
    "Symfony\\Component\\Console\\Output\\OutputInterface" to "output",
    "Symfony\\Component\\Console\\Cursor" to "cursor",
    "Symfony\\Component\\Console\\Style\\SymfonyStyle" to "io",
    "Symfony\\Component\\Console\\Application" to "application",
)

/**
 * Formats a FQN for display as "ClassName (namespace)"
 * e.g., "Symfony\\Component\\Console\\Style\\SymfonyStyle" -> "SymfonyStyle (Symfony\\Component\\Console\\Style)"
 */
private fun formatCommandParameterDisplayName(fqn: String): String {
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
 * Filters out parameters whose types are already present in the method signature.
 *
 * @param method The method to check
 * @return List of available FQNs (e.g., "Symfony\\Component\\Console\\Style\\SymfonyStyle")
 */
fun getAvailableCommandParameterFqns(method: Method): List<String> {
    val existingParameterTypes = getExistingCommandParameterTypes(method)

    val availableFqns = mutableListOf<String>()
    for (fqn in AVAILABLE_COMMAND_PARAMETERS.keys) {
        if (!existingParameterTypes.contains("\\$fqn")) {
            availableFqns.add(fqn)
        }
    }

    return availableFqns
}

private fun getExistingCommandParameterTypes(method: Method): Set<String> {
    val types = mutableSetOf<String>()
    for (parameter in method.parameters) {
        types.addAll(parameter.declaredType.types)
    }
    return types
}

/**
 * Intention action to add parameters to the __invoke method of an invokable Symfony Command.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class CommandInvokeParameterIntention : PsiElementBaseIntentionAction(), Iconable {
    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass::class.java)
            ?: return

        val invokeMethod = phpClass.findOwnMethodByName("__invoke")
            ?: return

        val availableFqns = getAvailableCommandParameterFqns(invokeMethod)
        if (availableFqns.isEmpty()) {
            return
        }

        // Build display names: "ClassName (namespace)"
        val displayNames = mutableListOf<String>()
        val displayToFqn = linkedMapOf<String, String>()
        for (fqn in availableFqns) {
            val displayName = formatCommandParameterDisplayName(fqn)
            displayNames.add(displayName)
            displayToFqn[displayName] = fqn
        }

        JBPopupFactory.getInstance().createPopupChooserBuilder(displayNames)
            .setTitle("Symfony: Add Parameter to __invoke")
            .setItemChosenCallback { selectedDisplay ->
                WriteCommandAction.writeCommandAction(project)
                    .withName("Add __invoke Parameter")
                    .run<RuntimeException> {
                        val fqn = displayToFqn[selectedDisplay]
                        val variableName = if (fqn != null) AVAILABLE_COMMAND_PARAMETERS[fqn] else null
                        if (variableName != null && fqn != null) {
                            PhpElementsUtil.addParameterToMethod(invokeMethod, "\\$fqn", variableName)
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

        val phpClass = PsiTreeUtil.getParentOfType(psiElement, PhpClass::class.java)
            ?: return false

        if (phpClass.getAttributes(AS_COMMAND_ATTRIBUTE).isEmpty()) {
            return false
        }

        if (PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Console\\Command\\Command")) {
            return false
        }

        val invokeMethod = phpClass.findOwnMethodByName("__invoke")
            ?: return false

        return getAvailableCommandParameterFqns(invokeMethod).isNotEmpty()
    }

    override fun getFamilyName(): String {
        return "Symfony: Add parameter to __invoke"
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
