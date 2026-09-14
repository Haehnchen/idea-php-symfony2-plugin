package fr.adrienbrault.idea.symfony2plugin.tests.config.xml.inspection

import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil
import fr.adrienbrault.idea.symfony2plugin.config.xml.inspection.XmlServiceArgumentInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.xml.inspection.XmlServiceArgumentInspection
 */
class XmlServiceArgumentInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/config/xml/inspection/fixtures"
    }

    fun testMissingArgumentProvidesInspection() {
        assertLocalInspectionContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Bar\"/>"),
            "Missing argument"
        )

        assertLocalInspectionContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Bar\"><argument>foo</argument></service>"),
            "Missing argument"
        )
    }

    fun testMissingArgumentNotProvidesInspection() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            "<foo<caret>bar class=\"Foo\\Bar\"/>",
            "Missing argument"
        )

        assertLocalInspectionNotContains(XmlServiceArgumentInspection::class.java,
            "services.html.twig",
            "<serv<caret>ice class=\"Foo\\Bar\"/>",
            "Missing argument"
        )
    }

    fun testThatAllParametersAreGiven() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Bar\"><argument>foo</argument><argument>foo</argument></service>"),
            "Missing argument"
        )
    }

    fun testThatAllParametersAreGivenWithLastOneOptional() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<serv<caret>ice class=\"Foo\\Apple\"><argument>foo</argument></service>"),
            "Missing argument"
        )
    }

    fun testThatNotSupportServiceAttributeNotProvidesInspection() {
        for (attribute in ServiceActionUtil.INVALID_ARGUMENT_ATTRIBUTES) {
            assertLocalInspectionNotContains(XmlServiceArgumentInspection::class.java,
                "services.xml",
                createContainer("<serv<caret>ice $attribute=\"foo\" class=\"Foo\\Bar\"/>"),
                "Missing argument"
            )
        }
    }

    fun testThatServiceResourceMustNotProvideInspection() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<serv<caret>ice resource=\"foo\" class=\"Foo\\Bar\"/>"),
            "Missing argument"
        )
    }

    fun testThatServiceFactoryServiceMustNotProvideInspection() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<serv<caret>ice factory-service=\"foo\" class=\"Foo\\Bar\"/>"),
            "Missing argument"
        )
    }

    fun testThatFactoryServiceOfSymfony26NotProvidesInspection() {
        for (attribute in ServiceActionUtil.INVALID_ARGUMENT_ATTRIBUTES) {
            assertLocalInspectionNotContains(XmlServiceArgumentInspection::class.java,
                "services.xml",
                createContainer("<serv<caret>ice $attribute=\"foo\" class=\"Foo\\Bar\"><factory/></service>"),
                "Missing argument"
            )
        }
    }

    fun testThatDefaultValueMustNotProvideInspection() {
        assertLocalInspectionNotContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<defaults autowire=\"true\" /><serv<caret>ice class=\"Foo\\Bar\"/>"),
            "Missing argument"
        )

        assertLocalInspectionContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<defaults autowire=\"false\" /><serv<caret>ice class=\"Foo\\Bar\"/>"),
            "Missing argument"
        )

        assertLocalInspectionContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<defaults autowire=\"true\" /><serv<caret>ice autowire=\"false\" class=\"Foo\\Bar\"/>"),
            "Missing argument"
        )
    }

    fun testThatServiceShortcutOnIdAttributeIsProvidesInspection() {
        assertLocalInspectionContains(XmlServiceArgumentInspection::class.java,
            "services.xml",
            createContainer("<serv<caret>ice id=\"Foo\\Bar\"/>"),
            "Missing argument"
        )
    }

    private fun createContainer(serviceDefinition: String): String {
        return "<container><services>$serviceDefinition</services></container>"
    }
}
