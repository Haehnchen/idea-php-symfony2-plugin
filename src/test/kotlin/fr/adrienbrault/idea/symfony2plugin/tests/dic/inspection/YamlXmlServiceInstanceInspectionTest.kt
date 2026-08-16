package fr.adrienbrault.idea.symfony2plugin.tests.dic.inspection

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.dic.inspection.YamlXmlServiceInstanceInspection
 */
class YamlXmlServiceInstanceInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("YamlXmlServiceInstanceInspection.php"))
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("YamlXmlServiceInstanceInspection.xml"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/dic/inspection/fixtures"
    }

    fun testInspectionForConstructorArguments() {
        val strings = arrayOf(
            "@ar<caret>gs_bar",
            "\"@ar<caret>gs_bar\"",
            "'@ar<caret>gs_bar'",
        )

        for (s in strings) {
            assertLocalInspectionContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    arguments: [" + s + "]",
                "Expect instance of: Args\\Foo"
            )

            assertLocalInspectionContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    arguments: [ @foo, %foo%, " + s + "]",
                "Expect instance of: Args\\Foo"
            )
        }
    }

    fun testInspectionForConstructorArgumentsForServiceIdShortcut() {
        assertLocalInspectionContains("services.yml",
            "services:\n" +
                "  Args\\Foo:\n" +
                "    arguments: [ @foo, %foo%, '@ar<caret>gs_bar']",
            "Expect instance of: Args\\Foo"
        )

        assertLocalInspectionContains("services.yml",
            "services:\n" +
                "  foo:\n" +
                "    class: \\Args\\Foo\n" +
                "    calls:\n" +
                "     - [ setFoo, ['@ar<caret>gs_bar'] ]\n",
            "Expect instance of: Args\\Foo"
        )
    }

    fun testInspectionForConstructorArgumentsAsSequence() {
        val strings = arrayOf(
            "@ar<caret>gs_bar",
            "\"@ar<caret>gs_bar\"",
            "'@ar<caret>gs_bar'",
        )

        for (s in strings) {
            assertLocalInspectionContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    arguments:\n" +
                    "     - " + s + "\n",
                "Expect instance of: Args\\Foo"
            )

            assertLocalInspectionContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    arguments:\n" +
                    "     - @foo\n" +
                    "     - %foo%\n" +
                    "     - " + s + "\n",
                "Expect instance of: Args\\Foo"
            )
        }
    }

    fun testInspectionForCallsArguments() {
        val strings = arrayOf(
            "@ar<caret>gs_bar",
            "\"@ar<caret>gs_bar\"",
            "'@ar<caret>gs_bar'",
        )

        for (s in strings) {
            assertLocalInspectionContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    calls:\n" +
                    "     - [ setFoo, [" + s + "] ]\n",
                "Expect instance of: Args\\Foo"
            )

            assertLocalInspectionContains("services.yml",
                "services:\n" +
                    "  foo:\n" +
                    "    class: \\Args\\Foo\n" +
                    "    calls:\n" +
                    "     - [ setFoo, [@foo, %foo%, " + s + "] ]\n",
                "Expect instance of: Args\\Foo"
            )
        }
    }
}
