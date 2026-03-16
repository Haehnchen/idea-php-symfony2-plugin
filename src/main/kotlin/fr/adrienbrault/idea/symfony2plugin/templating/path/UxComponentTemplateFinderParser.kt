package fr.adrienbrault.idea.symfony2plugin.templating.path

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser
import org.apache.commons.lang3.StringUtils
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.InputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * Parses the compiled Symfony container XML to extract the anonymous template directory
 * configured for the UX TwigComponent bundle.
 *
 * Reads the second argument of the service "ux.twig_component.component_template_finder":
 *
 *   <service id="ux.twig_component.component_template_finder" ...>
 *       <argument type="service" id="twig.loader.native_filesystem"/>
 *       <argument>foobar-ux/</argument>
 *   </service>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class UxComponentTemplateFinderParser : AbstractServiceParser() {

    val templateDirectories: MutableSet<String> = HashSet()

    override fun getXPathFilter(): String =
        "/container/services/service[@id='ux.twig_component.component_template_finder']"

    @Synchronized
    override fun parser(file: InputStream, sourceFile: VirtualFile, project: Project) {
        val document = parseDocument(file) ?: return

        val kernelProjectDir = TwigPathServiceParser.extractKernelProjectDir(document)
        val symfonyRootPrefix = TwigPathServiceParser.findSymfonyRootPrefix(project, sourceFile)

        val services = queryServices(document) ?: return

        for (i in 0 until services.length) {
            val service = services.item(i) as Element
            val args = service.getElementsByTagName("argument")

            // 1st argument = service ref (twig.loader.native_filesystem)
            // 2nd argument = template directory string
            if (args.length < 2) continue

            val path = args.item(1).textContent.trim()
            if (path.isBlank()) continue

            var relativePath: String = if (VfsExUtil.isAbsolutePath(path)) {
                val dir = kernelProjectDir ?: continue
                TwigPathServiceParser.normalizeAbsolutePath(path, dir) ?: continue
            } else {
                path
            }

            if (!symfonyRootPrefix.isNullOrEmpty()) {
                relativePath = "$symfonyRootPrefix/$relativePath"
            }

            relativePath = StringUtils.stripEnd(relativePath, "/")

            if (TwigPathServiceParser.existsInProjectRoot(project, relativePath)) {
                templateDirectories.add(relativePath)
            }
        }
    }

    private fun parseDocument(inputStream: InputStream): Document? = runCatching {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
    }.getOrNull()

    private fun queryServices(document: Document): NodeList? = runCatching {
        XPathFactory.newInstance().newXPath()
            .compile(xPathFilter)
            .evaluate(document, XPathConstants.NODESET) as NodeList
    }.getOrNull()
}
