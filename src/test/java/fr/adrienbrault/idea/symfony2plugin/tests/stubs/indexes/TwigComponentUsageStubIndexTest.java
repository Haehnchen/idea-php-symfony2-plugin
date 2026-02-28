package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigComponentUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigComponentUsageStubIndex
 */
public class TwigComponentUsageStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testThatComponentsAreIndexed() {
        myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "<twig:Alert />\n" +
            "{{ component('Card') }}\n" +
            "{{ component(foo_var) }}\n" +
            "{% component Alert %}{% endcomponent %}\n" +
            "{% component Banner with {type: 'success'} %}{% endcomponent %}\n" +
            "<twig:block name=\"headline\"></twig:block>\n"
        );

        assertIndexContains(TwigComponentUsageStubIndex.KEY, "Alert", "Card", "Banner");

        assertIndexContainsKeyWithValue(TwigComponentUsageStubIndex.KEY, "Alert", value -> value.contains("tag"));
        assertIndexContainsKeyWithValue(TwigComponentUsageStubIndex.KEY, "Card", value -> value.contains("function"));
        assertIndexContainsKeyWithValue(TwigComponentUsageStubIndex.KEY, "Banner", value -> value.contains("tag"));

        assertIndex(TwigComponentUsageStubIndex.KEY, true, "foo_var");
    }

    public void testThatNestedComponentNamesAreIndexedAcrossAllSyntaxes() {
        myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{{ component('Alert:Html:Foo_Bar_1') }}\n" +
            "{% component 'Alert:Html:Foo_Bar_1' with { title: 'Hello' } %}{% endcomponent %}\n" +
            "<twig:Alert:Html:Foo_Bar_1 title=\"Hello\" />\n" +
            "<twig:block name=\"headline\"></twig:block>\n"
        );

        assertIndexContains(TwigComponentUsageStubIndex.KEY, "Alert:Html:Foo_Bar_1");
        assertIndexContainsKeyWithValue(TwigComponentUsageStubIndex.KEY, "Alert:Html:Foo_Bar_1", value -> value.contains("function"));
        assertIndexContainsKeyWithValue(TwigComponentUsageStubIndex.KEY, "Alert:Html:Foo_Bar_1", value -> value.contains("tag"));
    }
}
