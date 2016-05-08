package fr.adrienbrault.idea.symfony2plugin.tests.intentions.yaml;

import fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlUnquotedColon;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.intentions.yaml.YamlUnquotedColon
 */
public class YamlUnquotedColonTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testColonInUnquotedMappingShouldDeprecated() {
        assertLocalInspectionContains("foo.yml",
            "class: fo<caret>obar:fff",
            YamlUnquotedColon.MESSAGE
        );
    }

    public void testColonInUnquotedWithoutMappingScopeShouldNotDeprecated() {
        assertLocalInspectionNotContains("foo.yml",
            "class: [fo<caret>obar:fff]",
            YamlUnquotedColon.MESSAGE
        );
        assertLocalInspectionNotContains("foo.yml",
            "class: [foo, fo<caret>obar:fff]",
            YamlUnquotedColon.MESSAGE
        );

        assertLocalInspectionNotContains("foo.yml",
            "class: {fo<caret>obar:fff}",
            YamlUnquotedColon.MESSAGE
        );
    }

}
