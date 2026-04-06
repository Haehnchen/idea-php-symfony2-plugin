package fr.adrienbrault.idea.symfony2plugin.tests.templating.path

import fr.adrienbrault.idea.symfony2plugin.templating.path.UxComponentTemplateFinderParser
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import java.io.ByteArrayInputStream

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see UxComponentTemplateFinderParser
 */
class UxComponentTemplateFinderParserTest : SymfonyLightCodeInsightFixtureTestCase() {

    fun testAbsolutePathWithoutKernelProjectDirIsSkipped() {
        val xml = UxComponentTemplateFinderParserTempTest.xml("""
            <service id="ux.twig_component.component_template_finder">
                <argument type="service" id="twig.loader.native_filesystem"/>
                <argument>/var/www/project/foobar-ux/</argument>
            </service>
        """)

        val containerFile = myFixture.addFileToProject("var/cache/dev/container.xml", xml).virtualFile

        val parser = parse(xml, containerFile)

        assertTrue(parser.templateDirectories.isEmpty())
    }

    fun testNonExistentPathIsSkipped() {
        val xml = UxComponentTemplateFinderParserTempTest.xml("""
            <service id="ux.twig_component.component_template_finder">
                <argument type="service" id="twig.loader.native_filesystem"/>
                <argument>foobar-ux/</argument>
            </service>
        """)

        val containerFile = myFixture.addFileToProject("var/cache/dev/container.xml", xml).virtualFile

        val parser = parse(xml, containerFile)

        assertTrue(parser.templateDirectories.isEmpty())
    }

    private fun parse(xml: String, containerFile: com.intellij.openapi.vfs.VirtualFile): UxComponentTemplateFinderParser {
        val parser = UxComponentTemplateFinderParser()
        parser.parser(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)), containerFile, project)
        return parser
    }
}
