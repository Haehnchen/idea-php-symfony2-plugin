package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.mcpserver.mcpFail
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOption
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil

class SymfonyFormTypeOptionsCollector(private val project: Project) {
    fun collect(formType: String): String {
        val normalizedFormType = if (formType.contains("\\")) {
            "\\" + formType.trimStart('\\')
        } else {
            formType
        }

        val options = mutableMapOf<String, FormOption>()

        FormOptionsUtil.visitFormOptions(project, normalizedFormType) { _, option, formClass, optionEnum ->
            if (!options.containsKey(option)) {
                options[option] = FormOption(option, formClass, optionEnum, formClass.phpClass)
            } else {
                options[option]?.addOptionEnum(optionEnum)
            }
        }

        if (options.isEmpty()) {
            mcpFail("Form type '$formType' not found or has no options.")
        }

        val csv = StringBuilder("name,type,source\\n")

        options.forEach { (name, formOption) ->
            val types = formOption.optionEnum.joinToString("|") { it.name }
            val source = formOption.formClass.phpClass.fqn

            csv.append("${McpCsvUtil.escape(name)},${McpCsvUtil.escape(types)},${McpCsvUtil.escape(source)}\\n")
        }

        return csv.toString()
    }
}
