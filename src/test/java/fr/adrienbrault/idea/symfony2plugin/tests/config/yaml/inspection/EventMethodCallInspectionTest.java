package fr.adrienbrault.idea.symfony2plugin.tests.config.yaml.inspection;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection.EventMethodCallInspection
 */
public class EventMethodCallInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/yaml/inspection/fixtures";
    }

    public void testThatYmlCallsProvidesMethodExistsCheck() {
        assertLocalInspectionContains("services.yml", "services:\n" +
                "    foo:\n" +
                "        class: Foo\\Service\\Method\\MyFoo\n" +
                "        calls:\n" +
                "            - [get<caret>Foos, []]"
            , "Missing Method");

        assertLocalInspectionContains("services.yml", "services:\n" +
                "    foo:\n" +
                "        class: Foo\\Service\\Method\\MyFoo\n" +
                "        tags:\n" +
                "            - { name: kernel.event_listener, event: kernel.exception, method: get<caret>Foos }"
            , "Missing Method");

        assertLocalInspectionNotContains("services.yml", "services:\n" +
                "    newsletter_manager:\n" +
                "        class: Foo\\Service\\Method\\MyFoo\n" +
                "        calls:\n" +
                "            - [get<caret>Foo, []]"
            , "Missing Method");
    }

    public void testThatPhpCallsProvidesMethodExistsCheck() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "" +
                "use Symfony\\Component\\EventDispatcher\\EventSubscriberInterface;\n" +
                "" +
                "class ExceptionSubscriber implements EventSubscriberInterface\n" +
                "{\n" +
                "    public static function getSubscribedEvents(): array\n" +
                "    {\n" +
                "        // return the subscribed events, their methods and priorities\n" +
                "        return [\n" +
                "            KernelEvents::EXCEPTION => [\n" +
                "                ['proces<caret>sException', 10],\n" +
                "            ],\n" +
                "        ];\n" +
                "    }\n" +
                "\n" +
                "}",
            "Missing Method"
        );
    }

    public void testThatXmlCallsProvidesMethodExistsCheck() {
        assertLocalInspectionContains("test.xml", "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<container xmlns=\"http://symfony.com/schema/dic/services\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://symfony.com/schema/dic/services\n" +
                "        https://symfony.com/schema/dic/services/services-1.0.xsd\">\n" +
                "\n" +
                "    <services>\n" +
                "        <service id=\"Foo\\Service\\Method\\MyFoo\">\n" +
                "            <call method=\"get<caret>Foos\"></call>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>",
            "Missing Method"
        );

        assertLocalInspectionContains("test.xml", "" +
                "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<container xmlns=\"http://symfony.com/schema/dic/services\"\n" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "    xsi:schemaLocation=\"http://symfony.com/schema/dic/services\n" +
                "        https://symfony.com/schema/dic/services/services-1.0.xsd\">\n" +
                "\n" +
                "    <services>\n" +
                "        <service id=\"Foo\\Service\\Method\\MyFoo\">\n" +
                "            <tag name=\"kernel.event_listener\" method=\"get<caret>Foos\"></call>\n" +
                "        </service>\n" +
                "    </services>\n" +
                "</container>",
            "Missing Method"
        );
    }

    public void testThatPhpCallsProvidesMethodExistsForPhpAttributeCheck() {
        assertLocalInspectionContains("test.php", "<?php\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(event: CustomEvent::class, method: 'onF<caret>ooBar')]\n" +
                "final class MyMultiListener\n" +
                "{\n" +
                "\n" +
                "}",
            "Missing Method"
        );

        assertLocalInspectionNotContains("test.php", "<?php\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(event: CustomEvent::class, method: 'on<caret>Foo')]\n" +
                "final class MyMultiListener\n" +
                "{\n" +
                "    public static function onFoo()\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "}",
            "Missing Method"
        );
    }
}
