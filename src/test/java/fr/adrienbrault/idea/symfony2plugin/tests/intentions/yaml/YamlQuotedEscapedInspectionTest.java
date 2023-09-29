package fr.adrienbrault.idea.symfony2plugin.tests.intentions.yaml;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlQuotedEscapedInspection
 */
public class YamlQuotedEscapedInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testDeprecatedNonEscapedInDoubleQuotedStrings() {
        this.initVersion();

        assertLocalInspectionContains("foo.yml",
            "class: \"Foo<caret>\\Bar\"",
            "Not escaping a backslash in a double-quoted string is deprecated"
        );

        assertLocalInspectionNotContains("foo.yml",
            "class: \"Foo<caret>\\\\Bar\"",
            "Not escaping a backslash in a double-quoted string is deprecated"
        );

        assertLocalInspectionNotContains("foo.yml",
            "class: 'Foo<caret>\\Bar'",
            "Not escaping a backslash in a double-quoted string is deprecated"
        );

        assertLocalInspectionNotContains("foo.yml",
            "class: Foo<caret>\\Bar",
            "Not escaping a backslash in a double-quoted string is deprecated"
        );
    }

    public void testDeprecatedNonEscapedInDoubleQuotedForWrongSymfonyVersion() {
        this.initVersion("2.5");

        assertLocalInspectionNotContains("foo.yml",
            "class: \"Foo<caret>\\Bar\"",
            "Not escaping a backslash in a double-quoted string is deprecated"
        );
    }

    public void testDeprecatedNonEscapedWhitelistCharInDoubleQuotedStrings() {
        this.initVersion();

        for (String s : new String[]{"\\n", "\\r", "\\t", "\\_", " "}) {
            assertLocalInspectionNotContains("foo.yml",
                "class: \"Foo<caret>" + s +"Bar\"",
                "Not escaping a backslash in a double-quoted string is deprecated"
            );
        }
    }

    public void testDeprecatedNonEscapedBlacklistConditionInDoubleQuotedStrings() {
        this.initVersion();

        assertLocalInspectionNotContains("foo.yml",
            "class: \"Foo<caret>\\Bar" + StringUtils.repeat("a", 255) + "\"",
            "Not escaping a backslash in a double-quoted string is deprecated"
        );
    }

    public void testDeprecatedUsageOfAtCharAtTheBeginningOfUnquotedStrings() {
        this.initVersion();

        assertLocalInspectionContains("foo.yml",
            "class: @f<caret>oo",
            "Deprecated usage of '@' at the beginning of unquoted string"
        );

        assertLocalInspectionContains("foo.yml",
            "class: `f<caret>oo",
            "Deprecated usage of '`' at the beginning of unquoted string"
        );

        assertLocalInspectionContains("foo.yml",
            "class: |f<caret>oo",
            "Deprecated usage of '|' at the beginning of unquoted string"
        );

        assertLocalInspectionContains("foo.yml",
            "class: >f<caret>oo",
            "Deprecated usage of '>' at the beginning of unquoted string"
        );

        assertLocalInspectionContains("foo.yml",
            "class: %f<caret>oo",
            "Not quoting a scalar starting with the '%' indicator character is deprecated since Symfony 3.1"
        );

        assertLocalInspectionNotContains("foo.yml",
            "class: '%f<caret>oo'",
            "Not quoting a scalar starting with the '%' indicator character is deprecated since Symfony 3.1"
        );

        assertLocalInspectionNotContains("foo.yml",
            "class: '@f<caret>oo'",
            "Deprecated usage of '@' at the beginning of unquoted string"
        );

        assertLocalInspectionNotContains("foo.yml",
            "class: \"@f<caret>oo\"",
            "Deprecated usage of '@' at the beginning of unquoted string"
        );
    }

    public void testDeprecatedUsageOfAtCharAtTheBeginningOfUnquotedStringsOnWrongSymfonyVersion() {
        this.initVersion("2.5");

        assertLocalInspectionNotContains("foo.yml",
            "class: @f<caret>oo",
            "Deprecated usage of '@' at the beginning of unquoted string"
        );
    }

    private void initVersion() {
        initVersion("2.8");
    }

    private void initVersion(@NotNull String version) {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace Symfony\\Component\\HttpKernel {\n" +
            "   class Kernel {\n" +
            "       const VERSION = '" + version + "';" +
            "   }" +
            "}"
        );
    }
}
