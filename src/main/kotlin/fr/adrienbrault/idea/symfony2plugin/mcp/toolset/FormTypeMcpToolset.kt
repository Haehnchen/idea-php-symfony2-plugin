@file:Suppress("FunctionName", "unused")

package fr.adrienbrault.idea.symfony2plugin.mcp.toolset

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import kotlinx.coroutines.currentCoroutineContext

/**
 * MCP toolset for Symfony form types.
 * Provides access to form types configured in the Symfony project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class FormTypeMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Lists all Symfony form types in the project as CSV.

        Returns CSV format with columns: name,className,filePath
        - name: Form type name/alias
        - className: FQN of the form type class
        - filePath: Relative path from project root

        Example output:
        name,className,filePath
        user,App\Form\UserType,src/Form/UserType.php
    """)
    suspend fun list_symfony_forms(): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "list_symfony_forms")

        return readAction {
            val collector = FormUtil.FormTypeCollector(project).collect()
            val formTypes = collector.formTypesMap
            val projectDir = ProjectUtil.getProjectDir(project)

            val csv = StringBuilder("name,className,filePath\n")

            formTypes.forEach { (name, formTypeClass) ->
                val phpClass = formTypeClass.getPhpClass(project)

                val filePath = phpClass?.containingFile?.virtualFile?.let { virtualFile ->
                    projectDir?.let { dir ->
                        VfsUtil.getRelativePath(virtualFile, dir, '/')
                            ?: FileUtil.getRelativePath(dir.path, virtualFile.path, '/')
                    }
                } ?: ""

                csv.append("${escapeCsv(name)},${escapeCsv(formTypeClass.phpClassName)},${escapeCsv(filePath)}\n")
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
