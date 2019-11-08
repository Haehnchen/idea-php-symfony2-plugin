package fr.adrienbrault.idea.symfony2plugin.tests.templating.variable.collector;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.variable.collector.MarcoScopeVariableCollector
 */
public class MarcoScopeVariableCollectorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatMacroProvidesCompletion() {
        assertCompletionContains(TwigFileType.INSTANCE, "" +
            "{% macro foo(foobar, foo, bar) %}\n" +
            "\n" +
            "    {{ <caret> }}\n" +
            "\n" +
            "{% endmacro %}",
        "foobar", "foo", "bar"
        );
    }
}
