package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex
 */
public class TwigIncludeStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("include.html.twig");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex#getIndexer()
     */
    public void testTemplateIncludeIndexer() {
        assertIndexContains(TwigIncludeStubIndex.KEY,
            "include_foo_quote.html.twig", "include_foo_double_quote.html.twig", "include_func_func_quote.html.twig",
            "include_func_func_quote.html.twig", "source_quote.html.twig", "source_double_quote.html.twig",
            "include_func_space.html.twig"
        );
    }

}
