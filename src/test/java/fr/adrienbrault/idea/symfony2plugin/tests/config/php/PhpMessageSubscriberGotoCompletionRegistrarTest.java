package fr.adrienbrault.idea.symfony2plugin.tests.config.php;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Stefano Arlandini <sarlandini@alice.it>
 *
 * @see fr.adrienbrault.idea.symfony2plugin.config.php.PhpMessageSubscriberGotoCompletionRegistrar
 */
public class PhpMessageSubscriberGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("classes.php");
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/config/php/fixtures";
    }

    public void testAutocompletionForGetHandledMessagesMethodOfMessageSubscriberInterface() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
            "final class FooMessageSubscriber implements Symfony\\Component\\Messenger\\Handler\\MessageSubscriberInterface\n" +
            "{\n" +
            "    public function handleFooEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function handleBarEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    private function handleBazEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    protected function handleQuxEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function __toString(): string\n" +
            "    {\n" +
            "        return '';\n" +
            "    }\n" +
            "    public function getHandledMessages(): iterable\n" +
            "    {\n" +
            "        yield FooEvent::class => ['method' => '<caret>'];\n" +
            "    }\n" +
            "}",
            "handleFooEvent",
            "handleBarEvent"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
            "final class FooMessageSubscriber implements Symfony\\Component\\Messenger\\Handler\\MessageSubscriberInterface\n" +
            "{\n" +
            "    public function handleFooEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function getHandledMessages(): iterable\n" +
            "    {\n" +
            "        yield FooEvent::class => ['method' => '<caret>handleFooEvent'];\n" +
            "    }\n" +
            "}",
            PlatformPatterns.psiElement(Method.class).withName("handleFooEvent")
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
            "final class FooMessageSubscriber implements Symfony\\Component\\Messenger\\Handler\\MessageSubscriberInterface\n" +
            "{\n" +
            "    public function handleFooEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function handleBarEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    private function handleBazEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    protected function handleQuxEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function __toString(): string\n" +
            "    {\n" +
            "        return '';\n" +
            "    }\n" +
            "    public function getHandledMessages(): iterable\n" +
            "    {\n" +
            "        yield 'FooEvent' => ['method' => '<caret>'];\n" +
            "    }\n" +
            "}",
            "handleFooEvent",
            "handleBarEvent"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
            "final class FooMessageSubscriber implements Symfony\\Component\\Messenger\\Handler\\MessageSubscriberInterface\n" +
            "{\n" +
            "    public function handleFooEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function handleBarEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    private function handleBazEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    protected function handleQuxEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function __toString(): string\n" +
            "    {\n" +
            "        return '';\n" +
            "    }\n" +
            "    public function getHandledMessages(): iterable\n" +
            "    {\n" +
            "        return [FooEvent::class => '<caret>'];\n" +
            "    }\n" +
            "}",
            "handleFooEvent",
            "handleBarEvent"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
            "final class FooMessageSubscriber implements Symfony\\Component\\Messenger\\Handler\\MessageSubscriberInterface\n" +
            "{\n" +
            "    public function handleFooEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function handleBarEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    private function handleBazEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    protected function handleQuxEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function __toString(): string\n" +
            "    {\n" +
            "        return '';\n" +
            "    }\n" +
            "    public function getHandledMessages(): iterable\n" +
            "    {\n" +
            "        return ['FooEvent' => '<caret>'];\n" +
            "    }\n" +
            "}",
            "handleFooEvent",
            "handleBarEvent"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
            "final class FooMessageSubscriber implements Symfony\\Component\\Messenger\\Handler\\MessageSubscriberInterface\n" +
            "{\n" +
            "    public function handleFooEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function getHandledMessages(): iterable\n" +
            "    {\n" +
            "        return [FooEvent::class => '<caret>handleFooEvent'];\n" +
            "    }\n" +
            "}",
            PlatformPatterns.psiElement(Method.class).withName("handleFooEvent")
        );

        assertCompletionIsEmpty(PhpFileType.INSTANCE, "<?php\n" +
            "final class FooMessageSubscriber implements Symfony\\Component\\Messenger\\Handler\\MessageSubscriberInterface\n" +
            "{\n" +
            "    public function handleFooEvent(): void\n" +
            "    {\n" +
            "    }\n" +
            "    public function getHandledMessages(): iterable\n" +
            "    {\n" +
            "        $foo = static function (): iterable {" +
            "            return ['FooEvent' => '<caret>'];\n" +
            "        };" +
            "    }\n" +
            "}"
        );
    }
}
