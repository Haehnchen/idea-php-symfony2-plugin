@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOption
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
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

        Parameters:
        - formType: Form type name or FQN (e.g., "text", "Symfony\Component\Form\Extension\Core\Type\TextType")

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
            throw IllegalStateException("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_symfony_form_options")

        if (formType.isBlank()) {
            throw IllegalArgumentException("formType parameter is required.")
        }

        // Normalize FQN: only add leading "\" if it's a FQN (contains backslash)
        val normalizedFormType = if (formType.contains("\\")) {
            "\\" + formType.trimStart('\\')
        } else {
            formType
        }

        return readAction {
            val options = mutableMapOf<String, FormOption>()

            FormOptionsUtil.visitFormOptions(project, normalizedFormType) { _, option, formClass, optionEnum ->
                if (!options.containsKey(option)) {
                    options[option] = FormOption(option, formClass, optionEnum, formClass.phpClass)
                } else {
                    options[option]?.addOptionEnum(optionEnum)
                }
            }

            if (options.isEmpty()) {
                throw IllegalArgumentException("Form type '$formType' not found or has no options.")
            }

            val csv = StringBuilder("name,type,source\n")

            options.forEach { (name, formOption) ->
                val types = formOption.optionEnum.joinToString("|") { it.name }
                val source = formOption.formClass.phpClass.fqn

                csv.append("${escapeCsv(name)},${escapeCsv(types)},${escapeCsv(source)}\n")
            }

            csv.toString()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
