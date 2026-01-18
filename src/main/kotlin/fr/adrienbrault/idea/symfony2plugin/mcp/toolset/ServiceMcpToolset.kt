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
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.mcp.McpUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil
import kotlinx.coroutines.currentCoroutineContext
import org.apache.commons.lang3.StringUtils

/**
 * MCP toolset for Symfony service container.
 * Provides access to service definitions configured in the Symfony project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ServiceMcpToolset : McpToolset {

    @McpTool
    @McpDescription("""
        Locate in which lineNumber a Symfony service is defined in configuration files by service name or class name.

        Returns CSV format with columns: serviceName,className,filePath,lineNumber
        - serviceName: The service ID/name (from service definition)
        - className: FQN of the service class (if available)
        - filePath: Relative path from project root
        - lineNumber: Line number where the service definition starts (1-indexed)

        IMPORTANT: The lineNumber indicates only the START of the service definition.
        Service definitions are multi-line YAML/XML/PHP blocks. You MUST read a range
        of lines (around the lineNumber, typically 10-20 lines depending on complexity)
        to capture the complete service definition.

        Note: Autowired services (automatically registered by Symfony based on class names)
        do not have explicit definitions in config files.

        Example output:
        serviceName,className,filePath,lineNumber
        app.service.my_service,\App\Service\MyService,config/services.yaml,15
        app.my_service_alias,\App\Service\MyService,config/services.yaml,25
    """)
    suspend fun locate_symfony_service(identifier: String): String {
        val project = currentCoroutineContext().project

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            mcpFail("Symfony plugin is not enabled for this project.")
        }

        McpUtil.checkToolEnabled(project, "locate_symfony_service")

        if (identifier.isBlank()) {
            mcpFail("identifier parameter is required.")
        }

        return readAction {
            val services = ContainerCollectionResolver.getServices(project)
            val projectDir = ProjectUtil.getProjectDir(project)
            val csv = StringBuilder("serviceName,className,filePath,lineNumber\n")

            // Check if identifier looks like a class name (contains namespace separator)
            val isClassName = identifier.contains("\\")

            if (isClassName) {
                // Resolve as a class name
                // Strip leading backslash and then add it back to ensure proper FQN format
                val stripped = StringUtils.stripStart(identifier, "\\")
                val normalizedIdentifier = "\\$stripped"
                val serviceCollector = ContainerCollectionResolver.ServiceCollector.create(project)
                val serviceNames = serviceCollector.convertClassNameToServices(normalizedIdentifier)

                if (serviceNames.isNotEmpty()) {
                    // Found services for this class
                    val className = normalizedIdentifier
                    for (serviceName in serviceNames) {
                        val elements = ServiceIndexUtil.findServiceDefinitions(project, serviceName)
                        appendServiceDefinitions(csv, serviceName, className, elements, projectDir)
                    }
                }

                // Also check if identifier is directly a service ID (can be both class name and service ID)
                val directServiceElements = ServiceIndexUtil.findServiceDefinitions(project, identifier)
                if (directServiceElements.isNotEmpty()) {
                    val containerService = services[identifier.lowercase()]
                    val className = containerService?.className ?: normalizedIdentifier
                    appendServiceDefinitions(csv, identifier, className, directServiceElements, projectDir)
                }
            } else {
                // Resolve as a service name
                val directServiceElements = ServiceIndexUtil.findServiceDefinitions(project, identifier)
                val containerService = services[identifier.lowercase()]
                val className = containerService?.className ?: ""
                appendServiceDefinitions(csv, identifier, className, directServiceElements, projectDir)
            }

            csv.toString()
        }
    }

    private fun appendServiceDefinitions(
        csv: StringBuilder,
        serviceName: String,
        className: String,
        elements: List<PsiElement>,
        projectDir: com.intellij.openapi.vfs.VirtualFile?
    ) {
        if (elements.isEmpty()) {
            return
        }

        elements.forEach { psiElement ->
            val psiFile = psiElement.containingFile
            val filePath = psiFile?.virtualFile?.let { virtualFile ->
                projectDir?.let { dir ->
                    VfsUtil.getRelativePath(virtualFile, dir, '/')
                        ?: FileUtil.getRelativePath(dir.path, virtualFile.path, '/')
                }
            } ?: ""

            val lineNumber = getLineNumber(psiElement, psiFile)

            csv.append("${escapeCsv(serviceName)},${escapeCsv(className)},${escapeCsv(filePath)},$lineNumber\n")
        }
    }

    /**
     * Gets the line number for a PSI element.
     * Returns the line number (1-indexed).
     */
    private fun getLineNumber(element: PsiElement, file: PsiFile?): Int {
        if (file == null) {
            return 1
        }

        val document = PsiDocumentManager.getInstance(element.project).getDocument(file)
            ?: return 1

        val startOffset = element.textRange.startOffset

        // Document line numbers are 0-indexed, convert to 1-indexed
        return document.getLineNumber(startOffset) + 1
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
