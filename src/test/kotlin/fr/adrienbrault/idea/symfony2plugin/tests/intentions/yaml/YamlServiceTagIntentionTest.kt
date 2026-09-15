package fr.adrienbrault.idea.symfony2plugin.tests.intentions.yaml

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import org.jetbrains.yaml.YAMLFileType

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlServiceTagIntention
 */
class YamlServiceTagIntentionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"))
    }

    override fun getTestDataPath(): String {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/intentions/yaml/fixtures"
    }

    fun testTagIntentionIsAvailable() {
        assertIntentionIsAvailable(
            YAMLFileType.YML,
            "services:\n" +
                "    foo:\n" +
                "        class: Foo<caret>\\Bar",
            "Symfony: Add Tags",
        )

        assertIntentionIsAvailable(
            YAMLFileType.YML,
            "services:\n" +
                "    foo:\n" +
                "        arguments: [Foo<caret>\\Bar] ",
            "Symfony: Add Tags",
        )
    }
}
