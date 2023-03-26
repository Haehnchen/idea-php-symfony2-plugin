package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpEventDispatcherGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("PhpEventDispatcherGotoCompletionRegistrar.php");
    }

    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/php/fixtures";
    }

    public void testGetSubscribedEventsForMethodArrayReturn() {
        assertCompletionContains("test.php", "<?php\n" +
                "namespace App\\EventSubscriber;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\EventSubscriberInterface;\n" +
                "\n" +
                "class ExceptionSubscriber implements EventSubscriberInterface\n" +
                "{\n" +
                "    public static function getSubscribedEvents(): array\n" +
                "    {\n" +
                "        return [\n" +
                "            KernelEvents::EXCEPTION => [\n" +
                "                ['<caret>', 10],\n" +
                "            ],\n" +
                "        ];\n" +
                "    }\n" +
                "    public function processException(ExceptionEvent $event)\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "}",
            "processException"
        );
    }

    public void testCompletionNavigationForAsEventListenerMethodNamedArgument() {
        assertCompletionContains("test.php", "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(event: 'bar', method: '<caret>')]\n" +
                "final class MyMultiListener\n" +
                "{\n" +
                "    public function onFoo(): void\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "}",
            "onFoo"
        );

        assertNavigationMatch("test.php", "<?php\n" +
            "namespace App\\EventListener;\n" +
            "\n" +
            "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
            "\n" +
            "#[AsEventListener(event: 'bar', method: 'onF<caret>oo')]\n" +
            "final class MyMultiListener\n" +
            "{\n" +
            "    public function onFoo(): void\n" +
            "    {\n" +
            "    }\n" +
            "\n" +
            "}", PlatformPatterns.psiElement(Method.class)
        );
    }
}
