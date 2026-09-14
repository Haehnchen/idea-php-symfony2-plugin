package fr.adrienbrault.idea.symfony2plugin.tests.intentions.yaml

import com.jetbrains.php.lang.PhpFileType
import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlQuotedEscapedInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlQuotedEscapedInspection
 */
class YamlQuotedEscapedInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testDeprecatedNonEscapedInDoubleQuotedStrings() {
        initVersion()

        assertLocalInspectionContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: \"Foo<caret>\\Bar\"",
            "Not escaping a backslash in a double-quoted string is deprecated"
        )

        assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: \"Foo<caret>\\\\Bar\"",
            "Not escaping a backslash in a double-quoted string is deprecated"
        )

        assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: 'Foo<caret>\\Bar'",
            "Not escaping a backslash in a double-quoted string is deprecated"
        )

        assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: Foo<caret>\\Bar",
            "Not escaping a backslash in a double-quoted string is deprecated"
        )
    }

    fun testDeprecatedNonEscapedInDoubleQuotedForWrongSymfonyVersion() {
        initVersion("2.5")

        assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: \"Foo<caret>\\Bar\"",
            "Not escaping a backslash in a double-quoted string is deprecated"
        )
    }

    fun testDeprecatedNonEscapedWhitelistCharInDoubleQuotedStrings() {
        initVersion()

        for (value in arrayOf("\\n", "\\r", "\\t", "\\_", " ")) {
            assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
                "class: \"Foo<caret>" + value + "Bar\"",
                "Not escaping a backslash in a double-quoted string is deprecated"
            )
        }
    }

    fun testDeprecatedNonEscapedBlacklistConditionInDoubleQuotedStrings() {
        initVersion()

        assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: \"Foo<caret>\\Bar" + StringUtils.repeat("a", 255) + "\"",
            "Not escaping a backslash in a double-quoted string is deprecated"
        )
    }

    fun testDeprecatedUsageOfAtCharAtTheBeginningOfUnquotedStrings() {
        initVersion()

        assertLocalInspectionContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: @f<caret>oo",
            "Deprecated usage of '@' at the beginning of unquoted string"
        )

        assertLocalInspectionContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: `f<caret>oo",
            "Deprecated usage of '`' at the beginning of unquoted string"
        )

        assertLocalInspectionContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: |f<caret>oo",
            "Deprecated usage of '|' at the beginning of unquoted string"
        )

        assertLocalInspectionContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: >f<caret>oo",
            "Deprecated usage of '>' at the beginning of unquoted string"
        )

        assertLocalInspectionContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: %f<caret>oo",
            "Not quoting a scalar starting with the '%' indicator character is deprecated since Symfony 3.1"
        )

        assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: '%f<caret>oo'",
            "Not quoting a scalar starting with the '%' indicator character is deprecated since Symfony 3.1"
        )

        assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: '@f<caret>oo'",
            "Deprecated usage of '@' at the beginning of unquoted string"
        )

        assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: \"@f<caret>oo\"",
            "Deprecated usage of '@' at the beginning of unquoted string"
        )
    }

    fun testDeprecatedUsageOfAtCharAtTheBeginningOfUnquotedStringsOnWrongSymfonyVersion() {
        initVersion("2.5")

        assertLocalInspectionNotContains(YamlQuotedEscapedInspection::class.java, "foo.yml",
            "class: @f<caret>oo",
            "Deprecated usage of '@' at the beginning of unquoted string"
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
