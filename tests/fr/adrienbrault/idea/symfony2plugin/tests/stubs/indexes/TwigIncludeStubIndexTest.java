package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex
 */
public class TwigIncludeStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{% include 'include_foo_quote.html.twig' %}\n" +
            "{% include \"include_foo_double_quote.html.twig\" %}\n" +
            "\n" +
            "{{ include('include_func_func_quote.html.twig') }}\n" +
            "{{ include(\"include_func_double_quote.html.twig\") }}\n" +
            "{{ include      (       'include_func_space.html.twig') }}\n" +
            "\n" +
            "{{ source('source_quote.html.twig') }}\n" +
            "{{ source(\"source_double_quote.html.twig\", ignore_missing = true) }}\n" +
            "\n" +
            "{% include ajax ? 'include_statement_0.html.twig' : 'include_statement_0.html.twig' %}\n" +
            "{% include ['include_array_0.html.twig', 'include_array_1.html.twig'] %}"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex#getIndexer()
     */
    public void testTemplateIncludeIndexer() {
        assertIndexContains(TwigIncludeStubIndex.KEY,
            "include_foo_quote.html.twig", "include_foo_double_quote.html.twig", "include_func_func_quote.html.twig",
            "include_func_func_quote.html.twig", "source_quote.html.twig", "source_double_quote.html.twig",
            "include_func_space.html.twig", "include_statement_0.html.twig", "include_statement_0.html.twig",
            "include_array_0.html.twig", "include_array_1.html.twig"
        );
    }

}
