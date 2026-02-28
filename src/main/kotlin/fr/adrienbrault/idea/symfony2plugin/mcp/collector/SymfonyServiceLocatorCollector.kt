package fr.adrienbrault.idea.symfony2plugin.mcp.collector

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.mcp.McpCsvUtil
import fr.adrienbrault.idea.symfony2plugin.mcp.McpPathUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil
import org.apache.commons.lang3.StringUtils

class SymfonyServiceLocatorCollector(private val project: Project) {
    fun collect(identifier: String): String {
        val services = ContainerCollectionResolver.getServices(project)
        val csv = StringBuilder("serviceName,className,filePath,lineNumber\\n")

        val isClassName = identifier.contains("\\")

        if (isClassName) {
            val stripped = StringUtils.stripStart(identifier, "\\")
            val normalizedIdentifier = "\\$stripped"
            val serviceCollector = ContainerCollectionResolver.ServiceCollector.create(project)
            val serviceNames = serviceCollector.convertClassNameToServices(normalizedIdentifier)

            if (serviceNames.isNotEmpty()) {
                val className = normalizedIdentifier
                for (serviceName in serviceNames) {
                    val elements = ServiceIndexUtil.findServiceDefinitions(project, serviceName)
                    appendServiceDefinitions(csv, serviceName, className, elements)
                }
            }

            val directServiceElements = ServiceIndexUtil.findServiceDefinitions(project, identifier)
            if (directServiceElements.isNotEmpty()) {
                val containerService = services[identifier.lowercase()]
                val className = containerService?.className ?: normalizedIdentifier
                appendServiceDefinitions(csv, identifier, className, directServiceElements)
            }
        } else {
            val directServiceElements = ServiceIndexUtil.findServiceDefinitions(project, identifier)
            val containerService = services[identifier.lowercase()]
            val className = containerService?.className ?: ""
            appendServiceDefinitions(csv, identifier, className, directServiceElements)
        }

        return csv.toString()
    }

    private fun appendServiceDefinitions(
        csv: StringBuilder,
        serviceName: String,
        className: String,
        elements: List<PsiElement>
    ) {
        if (elements.isEmpty()) {
            return
        }

        elements.forEach { psiElement ->
            val psiFile = psiElement.containingFile
            val filePath = psiFile?.virtualFile
                ?.let { McpPathUtil.getRelativeProjectPath(project, it) }
                ?: ""

            val lineNumber = getLineNumber(psiElement, psiFile)

            csv.append("${McpCsvUtil.escape(serviceName)},${McpCsvUtil.escape(className)},${McpCsvUtil.escape(filePath)},$lineNumber\\n")
        }
    }

    private fun getLineNumber(element: PsiElement, file: PsiFile?): Int {
        if (file == null) {
            return 1
        }

        val document = PsiDocumentManager.getInstance(element.project).getDocument(file)
            ?: return 1

        val startOffset = element.textRange.startOffset
        return document.getLineNumber(startOffset) + 1
    }
}
