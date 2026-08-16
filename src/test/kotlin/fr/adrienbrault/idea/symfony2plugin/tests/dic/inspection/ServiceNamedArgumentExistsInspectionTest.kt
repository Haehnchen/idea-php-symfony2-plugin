package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection

import fr.adrienbrault.idea.symfony2plugin.dic.inspection.ServiceNamedArgumentExistsInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ServiceNamedArgumentExistsInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
        myFixture.copyFileToProject("services.xml")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures"
    }

    fun testMissingArgumentForYaml() {
        assertLocalInspectionContains("foo.yml",
            "Foobar\\NamedArgument:\n" +
                "        arguments:\n" +
                "            \$foo<caret>bar1: ~",
            ServiceNamedArgumentExistsInspection.INSPECTION_MESSAGE
        )

        assertLocalInspectionNotContains("foo.yml",
            "Foobar\\UnknownClassNamedArgument:\n" +
                "        arguments:\n" +
                "            \$foo<caret>bar: ~",
            ServiceNamedArgumentExistsInspection.INSPECTION_MESSAGE
        )
    }

    fun testMissingArgumentForFactoryServiceIsNotTriggeredYaml() {
        assertLocalInspectionNotContains("foo.yml",
            "Foobar\\NamedArgument:\n" +
                "        factory: ~\n" +
                "        arguments:\n" +
                "            \$foo<caret>bar1: ~",
            ServiceNamedArgumentExistsInspection.INSPECTION_MESSAGE
        )
    }
}
