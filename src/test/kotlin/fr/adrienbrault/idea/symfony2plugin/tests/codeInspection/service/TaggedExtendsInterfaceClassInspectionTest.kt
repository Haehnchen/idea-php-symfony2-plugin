package fr.adrienbrault.idea.symfony2plugin.tests.codeInspection.service

import fr.adrienbrault.idea.symfony2plugin.codeInspection.service.TaggedExtendsInterfaceClassInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.codeInspection.service.TaggedExtendsInterfaceClassInspection
 */
class TaggedExtendsInterfaceClassInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/codeInspection/service/fixtures"
    }

    fun testThatKnownTagsShouldInspectionForMissingServiceClassImplementationsOfYaml() {
        assertLocalInspectionContains(TaggedExtendsInterfaceClassInspection.TaggedExtendsInterfaceClassInspectionYaml::class.java, "services.yml", "services:\n" +
            "    foo:\n" +
            "        class: Tag\\Instance<caret>Check\\EmptyClass\n" +
            "        tags:\n" +
            "            -  { name: twig.extension }",
            "Class needs to implement 'Twig_ExtensionInterface' for tag 'twig.extension'"
        )
    }

    fun testThatKnownTagsShouldInspectionForMissingServiceClassImplementationsForClassAsIsOfYaml() {
        assertLocalInspectionContains(TaggedExtendsInterfaceClassInspection.TaggedExtendsInterfaceClassInspectionYaml::class.java, "services.yml", "services:\n" +
                "    Tag\\Instance<caret>Check\\EmptyClass:\n" +
                "        tags:\n" +
                "            -  { name: twig.extension }",
            "Class needs to implement 'Twig_ExtensionInterface' for tag 'twig.extension'"
        )
    }

    fun testThatKnownTagsShouldInspectionForMissingServiceClassImplementationsOfXml() {
        assertLocalInspectionContains(TaggedExtendsInterfaceClassInspection.TaggedExtendsInterfaceClassInspectionXml::class.java,
            "services.xml",
            "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"test\" class=\"Tag\\Instance<caret>Check\\EmptyClass\">\n" +
                "             <tag name=\"twig.extension\"/>" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            "Class needs to implement 'Twig_ExtensionInterface' for tag 'twig.extension'"
        )
    }

    fun testThatKnownTagsShouldInspectionForMissingServiceClassImplementationsForClassAsIsOfYamlOfYml() {
        assertLocalInspectionContains(TaggedExtendsInterfaceClassInspection.TaggedExtendsInterfaceClassInspectionXml::class.java,
            "services.xml",
            "<?xml version=\"1.0\"?>\n" +
                "<container>\n" +
                "    <services>\n" +
                "        <service id=\"Tag\\Instance<caret>Check\\EmptyClass\">\n" +
                "             <tag name=\"twig.extension\"/>" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>\n",
            "Class needs to implement 'Twig_ExtensionInterface' for tag 'twig.extension'"
        )
    }
}
