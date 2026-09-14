package fr.adrienbrault.idea.symfony2plugin.tests.intentions.yaml

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlUnquotedColon
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlUnquotedColon
 */
class YamlUnquotedColonTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testColonInUnquotedMappingFollowedBySpaceShouldDeprecated() {
        initVersion()

        assertLocalInspectionContains(YamlUnquotedColon::class.java, "foo.yml",
            "class: fo<caret>obar: fff",
            YamlUnquotedColon.MESSAGE
        )

        assertLocalInspectionContains(YamlUnquotedColon::class.java, "foo.yml",
            "services:\n" +
            "   class: fo<caret>obar: fff\n",
            YamlUnquotedColon.MESSAGE
        )
    }

    fun testColonInUnquotedMappingFollowedBySpaceShouldNotDeprecatedOnWrongSymfonyVersion() {
        initVersion("2.7")

        assertLocalInspectionNotContains(YamlUnquotedColon::class.java, "foo.yml",
            "class: fo<caret>obar: fff",
            YamlUnquotedColon.MESSAGE
        )
    }

    fun testColonInUnquotedWithoutMappingScopeShouldNotDeprecated() {
        initVersion()

        assertLocalInspectionNotContains(YamlUnquotedColon::class.java, "foo.yml",
            "class: [fo<caret>obar:fff]",
            YamlUnquotedColon.MESSAGE
        )

        assertLocalInspectionNotContains(YamlUnquotedColon::class.java, "foo.yml",
            "class: [foo, fo<caret>obar:fff]",
            YamlUnquotedColon.MESSAGE
        )

        assertLocalInspectionNotContains(YamlUnquotedColon::class.java, "foo.yml",
            "class: {fo<caret>obar:fff}",
            YamlUnquotedColon.MESSAGE
        )

        assertLocalInspectionNotContains(YamlUnquotedColon::class.java, "foo.yml",
            "class: fo<caret>obar:ddd",
            YamlUnquotedColon.MESSAGE
        )

        assertLocalInspectionNotContains(YamlUnquotedColon::class.java, "foo.yml",
            "class: fo<caret>obar: ddd \n" +
                " fff",
            YamlUnquotedColon.MESSAGE
        )
    }

    private fun initVersion() {
        initVersion("2.8")
    }

    private fun initVersion(version: String) {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '$version';" +
            "   }" +
            "}"
        )
    }
}
