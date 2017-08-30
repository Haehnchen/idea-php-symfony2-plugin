package fr.adrienbrault.idea.symfony2plugin.tests.dic.yaml;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlDicInspectionsTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText("classes.php", "<?php\n" +
            "namespace Foo\\Name;\n" +
            "class FooClass {}"
        );
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("..").getFile()).getAbsolutePath();
    }

    public void testThatUnknownClassIsHighlighted() {
        assertCheckHighlighting("service.yml", "services:\n" +
            "    newsletter_manager:\n" +
            "        class: <warning descr=\"Missing Class\">Foo\\Class</warning>"
        );

        assertCheckHighlighting("service.yml", "services:\n" +
            "    newsletter_manager:\n" +
            "        class: <warning descr=\"Missing Class\">\"Foo\\Class\"</warning>"
        );

        assertCheckHighlighting("service.yml", "services:\n" +
            "    newsletter_manager:\n" +
            "        class: <warning descr=\"Missing Class\">'Foo\\Class'</warning>"
        );

        assertCheckHighlighting("service.yml", "services:\n" +
            "    newsletter_manager:\n" +
            "        factory_class: <warning descr=\"Missing Class\">Foo\\Class</warning>"
        );

        assertCheckHighlighting("service.yml", "services:\n" +
            "    newsletter_manager:\n" +
            "        factory_class: <warning descr=\"Missing Class\">\"Foo\\Class\"</warning>"
        );

        assertCheckHighlighting("service.yml", "services:\n" +
                "    newsletter_manager:\n" +
                "        factory_class: <warning descr=\"Missing Class\">'foo.service'</warning>"
        );
    }
}
