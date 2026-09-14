package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml

import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlServiceArgumentInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlServiceArgumentInspection
 */
class YamlServiceArgumentInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/config/yaml/fixtures"
    }

    fun testMissingArgumentProvidesInspection() {
        assertLocalInspectionContains(YamlServiceArgumentInspection::class.java, "services.yml", "services:\n" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Bar\n" +
                "    arguments: []",
            "Missing argument"
        )

        assertLocalInspectionContains(YamlServiceArgumentInspection::class.java, "services.yml", "services:\n" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Bar\n",
            "Missing argument"
        )
    }

    fun testQuickFixRunsOutsideWriteAction() {
        myFixture.enableInspections(YamlServiceArgumentInspection::class.java)
        myFixture.configureByText("services.yml", "services:\n" +
            "  f<caret>oo:\n" +
            "    class: \\Foo\\Bar\n" +
            "    arguments: []"
        )

        assertFalse(myFixture.findSingleIntention("Symfony: Yaml Argument").startInWriteAction())
    }

    fun testThatServiceShortcutOnIdAttributeIsProvidesInspection() {
        assertLocalInspectionContains(YamlServiceArgumentInspection::class.java, "services.yml", "services:\n" +
                "  Foo\\B<caret>ar:\n" +
                "    arguments: []",
            "Missing argument"
        )
    }

    fun testThatAllParametersAreGivenWithLastOneOptional() {
        for (service in arrayOf("@foo", "'@foo'", "\"@foo\"")) {
            assertLocalInspectionNotContains(YamlServiceArgumentInspection::class.java, "services.yml", String.format("services:\n" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Apple\n" +
                "    arguments: [%s]",
                service), "Missing argument")
        }
    }

    fun testThatNotSupportServiceAttributeNotProvidesInspection() {
        for (invalidKey in YamlServiceArgumentInspection.INVALID_KEYS) {
            assertLocalInspectionNotContains(YamlServiceArgumentInspection::class.java, "services.yml", String.format("services:\n" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Bar\n" +
                "    %s: ~",
                invalidKey), "Missing argument")
        }
    }

    fun testThatDefaultsWithAutoWireMustStopInspection() {
        assertLocalInspectionNotContains(YamlServiceArgumentInspection::class.java, "services.yml", "" +
                "services:\n" +
                "  _defaults:\n" +
                "    autowire: true\n" +
                "" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Bar\n",
            "Missing argument"
        )

        assertLocalInspectionContains(YamlServiceArgumentInspection::class.java, "services.yml", "" +
                "services:\n" +
                "  _defaults:\n" +
                "    autowire: false\n" +
                "" +
                "  f<caret>oo:\n" +
                "    class: \\Foo\\Bar\n",
            "Missing argument"
        )

        assertLocalInspectionContains(YamlServiceArgumentInspection::class.java, "services.yml", "" +
                "services:\n" +
                "  _defaults:\n" +
                "    autowire: true\n" +
                "" +
                "  Foo<caret>\\Bar:\n" +
                "    autowire: false\n",
            "Missing argument"
        )
    }
}
