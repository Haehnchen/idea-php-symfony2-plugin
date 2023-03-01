package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigMacroFunctionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigMacroFunctionStubIndex
 */
public class TwigMacroFunctionStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void testThatMacrosAreInIndex() {
        myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{% macro foobar %}{% endmacro %}\n" +
            "{% macro foobar2(foobar, foo) %}{% endmacro %}\n"
        );

        assertIndexContains(TwigMacroFunctionStubIndex.KEY, "foobar", "foobar2");

        assertIndexContainsKeyWithValue(TwigMacroFunctionStubIndex.KEY, "foobar2", value ->
            "(foobar, foo)".equals(value.parameters())
        );
    }
}
