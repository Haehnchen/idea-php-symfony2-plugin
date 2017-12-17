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
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testThatValuesAreInIndex() {
        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "block", value -> value.containsAll(Arrays.asList("foo", "foo_inner", "foobar_print")));

        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "block", value -> !value.contains("left_teaser"));
        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "block", value -> !value.contains("right_teaser"));
        assertIndexContainsKeyWithValue(TwigBlockIndexExtension.KEY, "block", value -> !value.contains("foobar_print_embed"));
    }
}
