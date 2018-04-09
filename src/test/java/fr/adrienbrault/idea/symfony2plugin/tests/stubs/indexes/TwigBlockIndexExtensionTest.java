package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;
import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension
 */
public class TwigBlockIndexExtensionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("blocks.html.twig");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/stubs/indexes/fixtures";
    }

    public void testThatValuesAreInIndex() {
        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "block", value -> value.containsAll(Arrays.asList("foo", "foo_inner", "foobar_print")));

        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "block", value -> !value.contains("left_teaser"));
        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "block", value -> !value.contains("right_teaser"));
        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "block", value -> !value.contains("foobar_print_embed"));

        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "extends", value -> value.contains("extends/foo.html.twig"));
        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "use", value -> value.contains("use/foo.html.twig"));

        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "extends", value -> !value.contains("embed_extends/foo.html.twig"));
        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "use", value -> !value.contains("embed_use/foo.html.twig"));
    }
}
