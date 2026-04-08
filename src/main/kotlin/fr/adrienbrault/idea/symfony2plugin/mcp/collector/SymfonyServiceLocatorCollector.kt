package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.psi.YAMLKeyValue

class SymfonyServiceLocatorCollector(private val project: Project) {
    fun collect(identifier: String): String = buildString {
        val sections = buildServiceSections(identifier)

        if (sections.isEmpty()) {
            append("No service found for: $identifier")
            return@buildString
        }

        sections.forEachIndexed { index, section ->
            append(section)
            if (index < sections.lastIndex) {
                appendLine()
                appendLine("---")
                appendLine()
            }
        }
    }

    private fun buildServiceSections(identifier: String): List<String> {
        val services = ContainerCollectionResolver.getServices(project)
        val sections = mutableListOf<String>()
        val visitedServiceNames = linkedSetOf<String>()
        val isClassName = "\\" in identifier

        if (isClassName) {
            val strippedIdentifier = StringUtils.stripStart(identifier, "\\")
            val normalizedIdentifier = "\\$strippedIdentifier"
            val serviceCollector = ContainerCollectionResolver.ServiceCollector.create(project)

            for (serviceName in serviceCollector.convertClassNameToServices(normalizedIdentifier)) {
                if (!visitedServiceNames.add(serviceName)) {
                    continue
                }

                val containerService = findContainerService(services, serviceName)
                val className = containerService?.className ?: normalizedIdentifier
                buildSection(serviceName, className, ServiceIndexUtil.findServiceDefinitionBlocks(project, serviceName), containerService)
                    ?.let { sections.add(it) }
            }

            val directServiceName = strippedIdentifier
            val directElements = ServiceIndexUtil.findServiceDefinitionBlocks(project, directServiceName)
            if (directElements.isNotEmpty() && visitedServiceNames.add(directServiceName)) {
                val containerService = findContainerService(services, directServiceName)
                val className = containerService?.className ?: normalizedIdentifier
                buildSection(directServiceName, className, directElements, containerService)
                    ?.let { sections.add(it) }
            }
        } else {
            val containerService = findContainerService(services, identifier)
            val className = containerService?.className.orEmpty()
            buildSection(identifier, className, ServiceIndexUtil.findServiceDefinitionBlocks(project, identifier), containerService)
                ?.let { sections.add(it) }
        }

        return sections
    }

    private fun findContainerService(
        services: Map<String, ContainerService>,
        serviceName: String,
    ): ContainerService? {
        val lower = serviceName.lowercase()
        return services[lower]
            ?: services[StringUtils.stripStart(serviceName, "\\").lowercase()]
            ?: services["\\${StringUtils.stripStart(serviceName, "\\").lowercase()}"]
    }

    private fun buildSection(
        serviceName: String,
        className: String,
        elements: List<PsiElement>,
        containerService: ContainerService?,
    ): String? {
        val fileBlocks = elements.mapNotNull { buildFileBlock(it).takeIf(String::isNotBlank) }
        val resourceBlocks = linkedSetOf<String>()

        if (fileBlocks.isEmpty() && containerService != null) {
            for (resourceServiceId in containerService.resourceServiceIds) {
                for (resourceElement in ServiceIndexUtil.findServiceDefinitionBlocks(project, resourceServiceId)) {
                    buildFileBlock(resourceElement)
                        .takeIf(String::isNotBlank)
                        ?.let { resourceBlocks.add(it) }
                }
            }
        }

        if (fileBlocks.isEmpty() && resourceBlocks.isEmpty() && containerService == null) {
            return null
        }

        val resourceBlockList = resourceBlocks.toList()
        return buildString {
            appendLine("## $serviceName")
            appendLine()

            when {
                fileBlocks.isNotEmpty() -> fileBlocks.forEachIndexed { index, block ->
                    append(block)
                    if (index < fileBlocks.lastIndex) {
                        appendLine()
                        appendLine()
                    }
                }

                resourceBlockList.isNotEmpty() -> {
                    appendLine("[AUTOWIRED] Auto-registered via resource/prototype definition.")
                    appendLine()
                    resourceBlockList.forEachIndexed { index, block ->
                        append(block)
                        if (index < resourceBlockList.lastIndex) {
                            appendLine()
                            appendLine()
                        }
                    }
                }

                containerService != null && (containerService.isAutowireEnabled || containerService.hasResourcePrototypeMetadata()) -> {
                    appendLine("[AUTOWIRED] Auto-registered; no explicit source block was resolved.")
                }

                else -> return null
            }
        }
    }

    private fun buildFileBlock(element: PsiElement): String = buildString {
        val psiFile = element.containingFile ?: return@buildString
        val virtualFile = psiFile.virtualFile ?: return@buildString
        val relativePath = McpPathUtil.getRelativeProjectPath(project, virtualFile)
        if (relativePath.isBlank()) {
            return@buildString
        }

        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return@buildString
        val blockRange = resolveBlockTextRange(element)
        val startLine = document.getLineNumber(blockRange.startOffset)
        val endLine = document.getLineNumber(blockRange.endOffset.coerceAtLeast(blockRange.startOffset + 1) - 1)
        val lineNumberWidth = (endLine + 1).toString().length

        appendLine("File: $relativePath")

        for (lineIndex in startLine..endLine) {
            val lineStartOffset = document.getLineStartOffset(lineIndex)
            val lineEndOffset = document.getLineEndOffset(lineIndex)
            var lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
            if (lineText.length > 120) {
                lineText = lineText.take(120) + "..."
            }

            val lineNumber = (lineIndex + 1).toString().padStart(lineNumberWidth)
            appendLine("$lineNumber: $lineText")
        }
    }

    private fun resolveBlockTextRange(element: PsiElement): TextRange {
        return when (element) {
            is YAMLKeyValue -> element.textRange
            is XmlTag -> element.textRange
            else -> element.textRange
        }
    }
}
