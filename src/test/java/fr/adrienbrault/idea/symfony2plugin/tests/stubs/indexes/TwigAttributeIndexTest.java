package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigAttributeIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigAttributeIndex
 */
public class TwigAttributeIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("TwigAttributeIndex.php");
    }

    public void testThatValuesAreInIndex() {
        assertIndexContainsKeyWithValue(TwigAttributeIndex.KEY, "product_number_filter", "#M#C\\App\\Twig\\AppExtension.formatProductNumberFilter");
        assertIndexContainsKeyWithValue(TwigAttributeIndex.KEY, "product_number_function", "#M#C\\App\\Twig\\AppExtension.formatProductNumberFunction");
        assertIndexContainsKeyWithValue(TwigAttributeIndex.KEY, "product_number_test", "#M#C\\App\\Twig\\AppExtension.formatProductNumberTest");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }
}
