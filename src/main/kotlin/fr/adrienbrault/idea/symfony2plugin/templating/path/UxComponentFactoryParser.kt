package fr.adrienbrault.idea.symfony2plugin.templating.path

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import fr.adrienbrault.idea.symfony2plugin.util.dict.CompiledTwigComponent
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream

/**
 * Parses Symfony UX TwigComponent runtime metadata from the compiled container.
 *
 * Symfony writes the final component configuration to the service
 * "ux.twig_component.component_factory":
 *
 * - top-level argument 4: component metadata keyed by component name
 * - top-level argument 5: class FQN to component name map
 */
class UxComponentFactoryParser : AbstractServiceParser() {

    val components: MutableMap<String, CompiledTwigComponent> = linkedMapOf()
    val classMap: MutableMap<String, String> = linkedMapOf()

    override fun getXPathFilter(): String =
        "/container/services/service[@id='ux.twig_component.component_factory']"

    @Synchronized
    override fun parser(file: InputStream, sourceFile: VirtualFile, project: Project) {
        val services = parserer(file) ?: return

        for (i in 0 until services.length) {
            val service = services.item(i) as? Element ?: continue
            val arguments = directArgumentChildren(service)
            val componentsArgument = arguments.getOrNull(4)
            if (componentsArgument != null) {
                parseComponentsArgument(componentsArgument)
            }

            val classMapArgument = arguments.getOrNull(5)
            if (classMapArgument != null) {
                parseClassMapArgument(classMapArgument)
            }
        }
    }

    private fun parseComponentsArgument(argument: Element) {
        for (componentArgument in directArgumentChildren(argument)) {
            val outerName = componentArgument.getAttribute("key").trim().takeIf { it.isNotBlank() }
            val values = keyedDirectArgumentValues(componentArgument)

            val name = values["key"]?.takeIf { it.isNotBlank() } ?: outerName ?: continue
            val phpClass = normalizePhpClass(values["class"])
            val template = values["template"]?.takeIf { it.isNotBlank() }
            val templateFromMethod = values["template_from_method"]?.takeIf { it.isNotBlank() }

            components[name] = CompiledTwigComponent(name, phpClass, template, templateFromMethod)
        }
    }

    private fun parseClassMapArgument(argument: Element) {
        for (mapArgument in directArgumentChildren(argument)) {
            val phpClass = normalizePhpClass(mapArgument.getAttribute("key")) ?: continue
            val componentName = mapArgument.textContent.trim()
            if (componentName.isNotBlank()) {
                classMap[phpClass] = componentName
            }
        }
    }

    private fun keyedDirectArgumentValues(argument: Element): Map<String, String> {
        val values = linkedMapOf<String, String>()
        for (child in directArgumentChildren(argument)) {
            val key = child.getAttribute("key").trim()
            if (key.isNotBlank()) {
                values[key] = child.textContent.trim()
            }
        }

        return values
    }

    private fun directArgumentChildren(element: Element): List<Element> {
        val arguments = mutableListOf<Element>()
        val childNodes = element.childNodes
        for (i in 0 until childNodes.length) {
            val child = childNodes.item(i)
            if (child.nodeType == Node.ELEMENT_NODE && child.nodeName == "argument") {
                arguments.add(child as Element)
            }
        }

        return arguments
    }

    private fun normalizePhpClass(value: String?): String? {
        val className = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return "\\" + className.trimStart('\\')
    }
}
