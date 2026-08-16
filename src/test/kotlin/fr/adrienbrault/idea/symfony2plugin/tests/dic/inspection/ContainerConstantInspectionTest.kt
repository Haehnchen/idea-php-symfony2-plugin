package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.ContainerConstantInspection
 */
class ContainerConstantInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.copyFileToProject("classes.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures"
    }

    fun testConstantInspectionForYamlFiles() {
        assertLocalInspectionContains(
            "foo.yml",
            "bar: !php/const:\\Foobar\\Car::FOOB<caret>AR_1",
            "Symfony: constant not found"
        )

        assertLocalInspectionNotContains(
            "foo.yml",
            "bar: !php/const:\\Foobar\\Car::FOOB<caret>AR",
            "Symfony: constant not found"
        )
    }

    fun testConstantInspectionForXmlFiles() {
        assertLocalInspectionContains("service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service id=\"foo\" class=\"DateTime\">\n" +
                "        <argument type=\"constant\">\\Foobar\\Car::FOOB<caret>AR_1</argument>" +
                "      </service>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: constant not found"
        )

        assertLocalInspectionNotContains("service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service id=\"foo\" class=\"DateTime\">\n" +
                "        <argument type=\"constant\">\\Foobar\\Car::FOOB<caret>AR</argument>" +
                "      </service>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: constant not found"
        )

        assertLocalInspectionNotContains("service.xml",
            "<container>\n" +
                "  <services>\n" +
                "      <service id=\"foo\" class=\"DateTime\">\n" +
                "        <argument type=\"constant\">Foobar\\Car::FOOB<caret>AR</argument>" +
                "      </service>\n" +
                "  </services>\n" +
                "</container>\n",
            "Symfony: constant not found"
        )
    }
}
