@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyFormTypeOptionsCollector
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Symfony form type options.
 * Provides access to options for a specific form type.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class FormTypeOptionsMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists all options for a Symfony form type as CSV.

        Returns CSV format with columns: name,type,source
        - name: Option name
        - type: Option type (DEFAULT, REQUIRED, DEFINED)
        - source: Source class that defines the option

        Example output:
        name,type,source
        label,DEFAULT,Symfony\Component\Form\Extension\Core\Type\FormType
        required,DEFAULT,Symfony\Component\Form\Extension\Core\Type\FormType
        data,DEFAULT,Symfony\Component\Form\Extension\Core\Type\FormType
    """)
    suspend fun list_symfony_form_options(
        @McpDescription("Form type name or FQN (e.g., 'text', 'Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType')")
        formType: String
    ): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_symfony_form_options")

        if (formType.isBlank()) {
            mcpFail("formType parameter is required.")
        }

        return readAction {
            SymfonyFormTypeOptionsCollector(project).collect(formType)
        }
    }
}
