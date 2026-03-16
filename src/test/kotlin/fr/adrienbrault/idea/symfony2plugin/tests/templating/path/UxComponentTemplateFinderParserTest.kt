package fr.adrienbrault.idea.symfony2plugin.tests.templating.path

import fr.adrienbrault.idea.symfony2plugin.templating.path.UxComponentTemplateFinderParser
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase
import java.io.ByteArrayInputStream

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see UxComponentTemplateFinderParser
 */
class UxComponentTemplateFinderParserTest : SymfonyTempCodeInsightFixtureTestCase() {

    fun testRelativeDirectoryIsExtracted() {
        val xml = xml("""
            <service id="ux.twig_component.component_template_finder">
                <argument type="service" id="twig.loader.native_filesystem"/>
                <argument>foobar-ux/</argument>
            </service>
        """)

        createFile("foobar-ux/.keep")
        val containerFile = createFile("var/cache/dev/container.xml", xml)

        val parser = parse(xml, containerFile)

        // trailing slash must be stripped
        assertTrue(parser.templateDirectories.contains("foobar-ux"))
    }

    fun testNestedSymfonyRootPrefixIsApplied() {
        val xml = xml("""
            <service id="ux.twig_component.component_template_finder">
                <argument type="service" id="twig.loader.native_filesystem"/>
                <argument>foobar-ux/</argument>
            </service>
        """)

        createFile("symfony-app/foobar-ux/.keep")
        val containerFile = createFile("symfony-app/var/cache/dev/container.xml", xml)

        val parser = parse(xml, containerFile)

        assertTrue(parser.templateDirectories.contains("symfony-app/foobar-ux"))
    }

    fun testAbsolutePathIsResolvedViaKernelProjectDir() {
        val xml = xml("""
            <service id="ux.twig_component.component_template_finder">
                <argument type="service" id="twig.loader.native_filesystem"/>
                <argument>/app/foobar-ux/</argument>
            </service>
        """, kernelProjectDir = "/app")

        createFile("foobar-ux/.keep")
        val containerFile = createFile("var/cache/dev/container.xml", xml)

        val parser = parse(xml, containerFile)

        assertTrue(parser.templateDirectories.contains("foobar-ux"))
    }

    fun testAbsolutePathWithoutKernelProjectDirIsSkipped() {
        val xml = xml("""
            <service id="ux.twig_component.component_template_finder">
                <argument type="service" id="twig.loader.native_filesystem"/>
                <argument>/var/www/project/foobar-ux/</argument>
            </service>
        """)

        val containerFile = createFile("var/cache/dev/container.xml", xml)

        val parser = parse(xml, containerFile)

        assertTrue(parser.templateDirectories.isEmpty())
    }

    fun testNonExistentPathIsSkipped() {
        val xml = xml("""
            <service id="ux.twig_component.component_template_finder">
                <argument type="service" id="twig.loader.native_filesystem"/>
                <argument>foobar-ux/</argument>
            </service>
        """)

        val containerFile = createFile("var/cache/dev/container.xml", xml)

        val parser = parse(xml, containerFile)

        assertTrue(parser.templateDirectories.isEmpty())
    }

    private fun xml(services: String, kernelProjectDir: String? = null): String {
        val params = if (kernelProjectDir != null)
            "<parameters><parameter key=\"kernel.project_dir\">$kernelProjectDir</parameter></parameters>"
        else ""
        return """<?xml version="1.0" encoding="utf-8"?><container>$params<services>$services</services></container>"""
    }

    private fun parse(xml: String, containerFile: com.intellij.openapi.vfs.VirtualFile): UxComponentTemplateFinderParser {
        val parser = UxComponentTemplateFinderParser()
        parser.parser(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)), containerFile, project)
        return parser
    }
}
