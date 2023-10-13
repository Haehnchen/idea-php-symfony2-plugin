package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockEmbedIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockEmbedIndex
 */
public class TwigBlockEmbedIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("blocks_embed.html.twig");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testThatValuesAreInIndex() {
        assertIndexContainsKeyWithValue(TwigBlockEmbedIndex.KEY, "teasers/skeleton.html.twig", value -> value.containsAll(Arrays.asList("left_teaser", "right_teaser")));
    }
}
