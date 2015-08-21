package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex
 */
public class TwigExtendsStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("extends.html.twig");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex#getIndexer()
     */
    public void testTemplateExtendsIndexer() {
        assertIndexContains(TwigExtendsStubIndex.KEY,
            "extends.html.twig", "extends_statement_0.html.twig", "extends_statement_1.html.twig",
            "extends_statement_3.html.twig"
        );

        assertIndexNotContains(TwigExtendsStubIndex.KEY,
            "extends_statement_2.html.twig"
        );
    }

}
