package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigControllerStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigControllerStubIndex
 */
public class TwigControllerStubIndexTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText(TwigFileType.INSTANCE, "" +
            "{{ controller('test::action') }}\n" +
            "{{ controller      ('test::action2') }}\n" +
            "{{ controller    ('te\\\\st::action3') }}\n"
        );
    }

    public void testTemplateIncludeIndexer() {
        assertIndexContains(TwigControllerStubIndex.KEY, "test::action", "test::action2", "te\\st::action3");
    }
}
