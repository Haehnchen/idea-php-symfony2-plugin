package fr.adrienbrault.idea.symfony2plugin.tests.codeInsight;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.SymfonyImplicitUsageProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see SymfonyImplicitUsageProvider
 */
public class SymfonyImplicitUsageProviderTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("routes.yml"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("services.yml"));
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/codeInsight/fixtures";
    }

    public void testControllerClassImplicitUsage() {
        assertImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "#[Route()]\n" +
            "public function foo2() {}"
        ));

        assertImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "/**\n" +
            "* @Route()\n" +
            "*/\n" +
            "public function foo() {}\n"
        ));

        assertNotImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "/**\n" +
            "* @Route()\n" +
            "*/\n" +
            "private function foo() {}\n" +
            "#[Route()]\n" +
            "private function foo2() {}"
        ));
    }

    public void testControllerMethodImplicitUsage() {
        assertImplicitUsage(createPhpControllerMethodWithRouteContent("" +
            "/**\n" +
            "* @Route()\n" +
            "*/\n" +
            "public function foobar() {}"
        ));

        assertImplicitUsage(createPhpControllerMethodWithRouteContent("" +
            "#[Route()]\n" +
            "public function foobar() {}"
        ));

        assertNotImplicitUsage(createPhpControllerMethodWithRouteContent("" +
            "/**\n" +
            "* @Route()\n" +
            "*/\n" +
            "private function foobar() {}"
        ));

        assertNotImplicitUsage(createPhpControllerMethodWithRouteContent("" +
            "#[Route()]\n" +
            "private function foobar() {}"
        ));
    }

    public void testYamlControllerDefinitionsAreMarkedUsed() {
        assertImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "public function foobarYaml() {}"
        ));

        assertImplicitUsage(createPhpControllerClassWithRouteContent(
            "\\App\\Controller\\FooControllerInvoke",
            "public function __invoke() {}"
        ));

        assertImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "public function foobarYamlAction() {}"
        ));

        assertImplicitUsage(createPhpControllerClassWithRouteContent(
            "\\App\\Controller\\FooControllerService",
            "public function foo() {}"
        ));

        assertImplicitUsage(createPhpControllerClassWithRouteContent(
            "\\App\\Controller\\FooControllerService",
            "public function foo() {}"
        ));

        assertImplicitUsage(createPhpControllerClassWithRouteContent(
            "\\App\\Controller\\FooControllerServiceInvoke",
            "public function __invoke() {}"
        ));
    }

    public void testServiceRegisteredClassesAreMarkedUsed() {
        PsiFile commandPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Command;\n" +
            "class FooCommand extends \\Symfony\\Component\\Console\\Command\\Command {}"
        );

        PhpClass commandClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) commandPsiFile.getContainingFile());
        assertImplicitUsage(commandClass);

        PsiFile nonServiceCommandPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Command;\n" +
            "class FoobarServiceCommand extends \\Symfony\\Component\\Console\\Command\\Command {}"
        );

        PhpClass nonServiceCommandClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) nonServiceCommandPsiFile.getContainingFile());
        assertNotImplicitUsage(nonServiceCommandClass);

        PsiFile voterPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Voter;\n" +
            "class MyFoobarVoter implements \\Symfony\\Component\\Security\\Core\\Authorization\\Voter\\VoterInterface {}"
        );

        PhpClass voterClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) voterPsiFile.getContainingFile());
        assertImplicitUsage(voterClass);

        PsiFile repositoryPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Repository;\n" +
            "class MyFoobarEntityRepository extends \\Doctrine\\ORM\\EntityRepository {}\n"
        );

        PhpClass repositoryClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) repositoryPsiFile.getContainingFile());
        assertImplicitUsage(repositoryClass);

        PsiFile validatorPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Validator;\n" +
            "class MyFoobarConstraint extends \\Symfony\\Component\\Validator\\Constraint {}\n"
        );

        PhpClass validatorClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) validatorPsiFile.getContainingFile());
        assertImplicitUsage(validatorClass);

        PsiFile eventListenerPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\EventListener;\n" +
            "class ExceptionListener {}\n"
        );

        PhpClass eventListenerClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) eventListenerPsiFile.getContainingFile());
        assertImplicitUsage(eventListenerClass);
    }

    public void testEventSubscriberGetSubscribedEventsArray() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "<?php\n" +
            "\n" +
            "namespace App\\EventSubscriber;\n" +
            "\n" +
            "use Symfony\\Component\\EventDispatcher\\EventSubscriberInterface;\n" +
            "use Symfony\\Component\\HttpKernel\\KernelEvents;\n" +
            "\n" +
            "class ExceptionSubscriber implements EventSubscriberInterface\n" +
            "{\n" +
            "    public static function getSubscribedEvents()\n" +
            "    {\n" +
            "        return [\n" +
            "            KernelEvents::EXCEPTION => [\n" +
            "                ['processException', 10],\n" +
            "                ['notifyException', -10],\n" +
            "            ],\n" +
            "        ];\n" +
            "        return [\n" +
            "            'keyString' => 'logException'\n" +
            "        ];" +
            "    }\n" +
            "\n" +
            "    public function processException() {}\n" +
            "    public function logException() {}\n" +
            "    public function notifyException() {}\n" +
            "    public function notifyExceptionUnknown() {}\n" +
            "    public function keyString() {}\n" +
            "}"
        );

        PhpClass phpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());

        assertImplicitUsage(phpClass.findOwnMethodByName("processException"));
        assertImplicitUsage(phpClass.findOwnMethodByName("logException"));
        assertImplicitUsage(phpClass.findOwnMethodByName("notifyException"));

        assertNotImplicitUsage(phpClass.findOwnMethodByName("notifyExceptionUnknown"));
        assertNotImplicitUsage(phpClass.findOwnMethodByName("keyString"));

        assertImplicitUsage(phpClass);
    }


    public void testEventListenerAttributeUsage() {
        PsiFile classListenerPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(event: 'bar', method: 'onFoo')]\n" +
                "final class MyMultiListener\n" +
                "{\n" +
                "    public function onFoo(): void\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "}"
        );

        PhpClass classListenerClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) classListenerPsiFile.getContainingFile());
        assertImplicitUsage(classListenerClass.findOwnMethodByName("onFoo"));

        PsiFile methodListenerPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "final class MyMultiListener\n" +
                "{\n" +
                "    #[AsEventListener]\n" +
                "    public function onFoo(): void\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "}"
        );

        PhpClass methodListenerClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) methodListenerPsiFile.getContainingFile());
        assertImplicitUsage(methodListenerClass.findOwnMethodByName("onFoo"));

        PsiFile invokeListenerPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener()]\n" +
                "final class MyMultiListener\n" +
                "{\n" +
                "    public function __invoke(): void\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "}"
        );

        PhpClass invokeListenerClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) invokeListenerPsiFile.getContainingFile());
        assertImplicitUsage(invokeListenerClass.findOwnMethodByName("__invoke"));

        PsiFile inferredMethodListenerPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(event: 'bar')]\n" +
                "final class MyMultiListener\n" +
                "{\n" +
                "    public function onBar(): void\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "}"
        );

        PhpClass inferredMethodListenerClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) inferredMethodListenerPsiFile.getContainingFile());
        assertImplicitUsage(inferredMethodListenerClass.findOwnMethodByName("onBar"));

        PsiFile cleanedUpMethodListenerPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(event: 'foo-bar')]\n" +
                "final class MyMultiListener\n" +
                "{\n" +
                "    public function onFooBar(): void\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "}"
        );

        PhpClass cleanedUpMethodListenerClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) cleanedUpMethodListenerPsiFile.getContainingFile());
        assertImplicitUsage(cleanedUpMethodListenerClass.findOwnMethodByName("onFooBar"));

        PsiFile combinedListenerPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
                "namespace App\\EventListener;\n" +
                "\n" +
                "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
                "\n" +
                "#[AsEventListener(method: 'onMethodAttr')]\n" +
                "#[AsEventListener(event: 'event-name')]\n" +
                "#[AsEventListener]\n" +
                "final class MyMultiListener\n" +
                "{\n" +
                "    public function onMethodAttr(): void\n" +
                "    {\n" +
                "    }\n" +
                "    public function onEventName(): void\n" +
                "    {\n" +
                "    }\n" +
                "    public function __invoke(): void\n" +
                "    {\n" +
                "    }\n" +
                "    #[AsEventListener]\n" +
                "    public function onMethod(): void\n" +
                "    {\n" +
                "    }\n" +
                "    public function onUnregistered(): void\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "}"
        );

        PhpClass combinedListenerClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) combinedListenerPsiFile.getContainingFile());

        assertImplicitUsage(combinedListenerClass.findOwnMethodByName("onMethodAttr"));
        assertImplicitUsage(combinedListenerClass.findOwnMethodByName("onEventName"));
        assertImplicitUsage(combinedListenerClass.findOwnMethodByName("__invoke"));
        assertImplicitUsage(combinedListenerClass.findOwnMethodByName("onMethod"));
        assertNotImplicitUsage(combinedListenerClass.findOwnMethodByName("onUnregistered"));
    }

    public void testTwigExtensionsImplicitUsage() {
        PsiFile serviceTwigExtensionPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "\n" +
            "namespace App\\TwigExtension;\n" +
            "\n" +
            "class MyTwigExtension implements \\Twig\\Extension\\ExtensionInterface\n" +
            "{\n" +
            "    public function getFilters() {}\n" +
            "}"
        );

        PhpClass serviceTwigExtensionClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) serviceTwigExtensionPsiFile.getContainingFile());
        assertImplicitUsage(serviceTwigExtensionClass);

        PsiFile nonServiceTwigExtensionPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "\n" +
            "namespace App\\TwigExtension;\n" +
            "\n" +
            "class MyTwigExtension implements \\Twig\\Extension\\ExtensionInterface\n" +
            "{\n" +
            "    public function getFoobar() {}\n" +
            "}"
        );

        PhpClass nonServiceTwigExtensionClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) nonServiceTwigExtensionPsiFile.getContainingFile());
        assertNotImplicitUsage(nonServiceTwigExtensionClass);

        PsiFile attributedTwigExtensionPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "\n" +
            "namespace App\\Twig;\n" +
            "\n" +
            "use Twig\\Attribute\\AsTwigFunction;\n" +
            "use Twig\\Attribute\\AsTwigFilter;\n" +
            "use Twig\\Attribute\\AsTwigTest;\n" +
            "\n" +
            "class MyTwigExtension\n" +
            "{\n" +
            "    #[AsTwigFunction('my_function')]\n" +
            "    public function myFunction(): string\n" +
            "    {\n" +
            "        return 'Hello';\n" +
            "    }\n" +
            "\n" +
            "    #[AsTwigFilter('my_filter')]\n" +
            "    public function myFilter(): string\n" +
            "    {\n" +
            "        return 'Filtered';\n" +
            "    }\n" +
            "\n" +
            "    #[AsTwigTest('my_test')]\n" +
            "    public function myTest(): bool\n" +
            "    {\n" +
            "        return true;\n" +
            "    }\n" +
            "\n" +
            "    public function unusedMethod(): string\n" +
            "    {\n" +
            "        return 'Not used';\n" +
            "    }\n" +
            "}"
        );

        PhpClass attributedTwigExtensionClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) attributedTwigExtensionPsiFile.getContainingFile());

        assertImplicitUsage(attributedTwigExtensionClass.findOwnMethodByName("myFunction"));
        assertImplicitUsage(attributedTwigExtensionClass.findOwnMethodByName("myFilter"));
        assertImplicitUsage(attributedTwigExtensionClass.findOwnMethodByName("myTest"));

        assertNotImplicitUsage(attributedTwigExtensionClass.findOwnMethodByName("unusedMethod"));

        PsiFile multiAttributeTwigExtensionPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "\n" +
            "namespace App\\Twig;\n" +
            "\n" +
            "use Twig\\Attribute\\AsTwigFunction;\n" +
            "use Twig\\Attribute\\AsTwigFilter;\n" +
            "\n" +
            "class MyTwigExtension\n" +
            "{\n" +
            "    #[AsTwigFunction('callable_method')]\n" +
            "    #[AsTwigFilter('callable_filter')]\n" +
            "    public function callableMethod(): string\n" +
            "    {\n" +
            "        return 'Callable';\n" +
            "    }\n" +
            "}"
        );

        PhpClass multiAttributeTwigExtensionClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) multiAttributeTwigExtensionPsiFile.getContainingFile());
        assertImplicitUsage(multiAttributeTwigExtensionClass.findOwnMethodByName("callableMethod"));
    }

    public void testClassLevelFrameworkAttributesAreMarkedUsed() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Usage;\n" +
            "\n" +
            "use Doctrine\\Bundle\\DoctrineBundle\\Attribute\\AsDoctrineListener;\n" +
            "use Doctrine\\ORM\\Events;\n" +
            "use Symfony\\Component\\Console\\Attribute\\AsCommand;\n" +
            "use Symfony\\Component\\DependencyInjection\\Attribute\\AutoconfigureTag;\n" +
            "use Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener;\n" +
            "use Symfony\\Component\\HttpKernel\\KernelEvents;\n" +
            "use Symfony\\Component\\Messenger\\Attribute\\AsMessageHandler;\n" +
            "use Symfony\\Component\\Scheduler\\Attribute\\AsSchedule;\n" +
            "use Symfony\\Component\\Validator\\Constraints as Assert;\n" +
            "use Symfony\\Component\\Workflow\\Attribute\\AsAnnounceListener;\n" +
            "use Symfony\\Component\\Workflow\\Attribute\\AsCompletedListener;\n" +
            "use Symfony\\Component\\Workflow\\Attribute\\AsEnterListener;\n" +
            "use Symfony\\Component\\Workflow\\Attribute\\AsEnteredListener;\n" +
            "use Symfony\\Component\\Workflow\\Attribute\\AsGuardListener;\n" +
            "use Symfony\\Component\\Workflow\\Attribute\\AsLeaveListener;\n" +
            "use Symfony\\Component\\Workflow\\Attribute\\AsTransitionListener;\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "\n" +
            "#[AsCommand(name: 'app:usage:interactive')]\n" +
            "#[AsEventListener(event: KernelEvents::EXCEPTION, method: 'onKernelException')]\n" +
            "#[AsMessageHandler]\n" +
            "#[AsSchedule('usage')]\n" +
            "#[AsAnnounceListener]\n" +
            "#[AsCompletedListener]\n" +
            "#[AsEnterListener]\n" +
            "#[AsEnteredListener]\n" +
            "#[AsGuardListener]\n" +
            "#[AsLeaveListener]\n" +
            "#[AsTransitionListener]\n" +
            "#[AsTwigComponent('usage_status_badge')]\n" +
            "#[Assert\\Callback('validateShippingWindow')]\n" +
            "#[AsDoctrineListener(event: Events::preFlush)]\n" +
            "#[AutoconfigureTag('doctrine.event_listener', ['event' => 'postPersist'])]\n" +
            "class UsageAttributes\n" +
            "{\n" +
            "    public function validateShippingWindow() {}\n" +
            "    public function onKernelException() {}\n" +
            "    public function unrelated() {}\n" +
            "}"
        );

        PhpClass phpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());

        assertImplicitUsage(phpClass);
        assertImplicitUsage(phpClass.findOwnMethodByName("validateShippingWindow"));
        assertImplicitUsage(phpClass.findOwnMethodByName("onKernelException"));
        assertNotImplicitUsage(phpClass.findOwnMethodByName("unrelated"));
    }

    public void testMcpCapabilitiesAreMarkedUsed() {
        PsiFile invokableCapabilityPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Mcp;\n" +
            "\n" +
            "use Mcp\\Capability\\Attribute\\McpTool;\n" +
            "\n" +
            "#[McpTool(name: 'current-time')]\n" +
            "final class CurrentTimeTool\n" +
            "{\n" +
            "    public function __invoke(string $format = 'Y-m-d H:i:s'): string\n" +
            "    {\n" +
            "        return 'now';\n" +
            "    }\n" +
            "\n" +
            "    public function unused(): string\n" +
            "    {\n" +
            "        return 'unused';\n" +
            "    }\n" +
            "}"
        );

        PhpClass invokableCapabilityClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) invokableCapabilityPsiFile.getContainingFile());
        assertImplicitUsage(invokableCapabilityClass);
        assertImplicitUsage(invokableCapabilityClass.findOwnMethodByName("__invoke"));
        assertNotImplicitUsage(invokableCapabilityClass.findOwnMethodByName("unused"));

        PsiFile methodCapabilityPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Mcp;\n" +
            "\n" +
            "use Mcp\\Capability\\Attribute\\McpPrompt;\n" +
            "use Mcp\\Capability\\Attribute\\McpResource;\n" +
            "use Mcp\\Capability\\Attribute\\McpResourceTemplate;\n" +
            "\n" +
            "final class TimeCapabilities\n" +
            "{\n" +
            "    #[McpPrompt(name: 'time-analysis')]\n" +
            "    public function timeAnalysis(): array\n" +
            "    {\n" +
            "        return [];\n" +
            "    }\n" +
            "\n" +
            "    #[McpResource(uri: 'time://current', name: 'current-time')]\n" +
            "    public function currentTime(): array\n" +
            "    {\n" +
            "        return [];\n" +
            "    }\n" +
            "\n" +
            "    #[McpResourceTemplate(uriTemplate: 'time://{timezone}', name: 'time-by-timezone')]\n" +
            "    public function timeByTimezone(string $timezone): array\n" +
            "    {\n" +
            "        return [];\n" +
            "    }\n" +
            "\n" +
            "    public function unused(): array\n" +
            "    {\n" +
            "        return [];\n" +
            "    }\n" +
            "}"
        );

        PhpClass methodCapabilityClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) methodCapabilityPsiFile.getContainingFile());
        assertNotImplicitUsage(methodCapabilityClass);
        assertImplicitUsage(methodCapabilityClass.findOwnMethodByName("timeAnalysis"));
        assertImplicitUsage(methodCapabilityClass.findOwnMethodByName("currentTime"));
        assertImplicitUsage(methodCapabilityClass.findOwnMethodByName("timeByTimezone"));
        assertNotImplicitUsage(methodCapabilityClass.findOwnMethodByName("unused"));
    }

    public void testAutoconfigureTagIsMarkedUsedForNamedAndDefaultFqcnForms() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Usage;\n" +
            "\n" +
            "use Symfony\\Component\\DependencyInjection\\Attribute\\AutoconfigureTag;\n" +
            "\n" +
            "#[AutoconfigureTag('app.other_tag')]\n" +
            "class NamedAutoconfigureTagUsage\n" +
            "{\n" +
            "}"
        );

        PhpClass phpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());
        assertImplicitUsage(phpClass);

        PsiFile defaultPsiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Usage;\n" +
            "\n" +
            "use Symfony\\Component\\DependencyInjection\\Attribute\\AutoconfigureTag;\n" +
            "\n" +
            "#[AutoconfigureTag]\n" +
            "interface DefaultAutoconfigureTagUsage\n" +
            "{\n" +
            "}"
        );

        PhpClass defaultPhpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) defaultPsiFile.getContainingFile());
        assertImplicitUsage(defaultPhpClass);
    }

    public void testDoctrineLifecycleMethodsAreMarkedUsedOnEntities() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Entity;\n" +
            "\n" +
            "use Doctrine\\ORM\\Event\\PostLoadEventArgs;\n" +
            "use Doctrine\\ORM\\Event\\PreFlushEventArgs;\n" +
            "use Doctrine\\ORM\\Event\\PrePersistEventArgs;\n" +
            "use Doctrine\\ORM\\Mapping as ORM;\n" +
            "\n" +
            "#[ORM\\Entity]\n" +
            "class LifecycleEntity\n" +
            "{\n" +
            "    #[ORM\\PrePersist]\n" +
            "    public function beforePersist(PrePersistEventArgs $event): void\n" +
            "    {\n" +
            "    }\n" +
            "\n" +
            "    #[ORM\\PreFlush]\n" +
            "    public function beforeFlush(PreFlushEventArgs $event): void\n" +
            "    {\n" +
            "    }\n" +
            "\n" +
            "    #[ORM\\PostLoad]\n" +
            "    public function afterLoad(PostLoadEventArgs $event): void\n" +
            "    {\n" +
            "    }\n" +
            "\n" +
            "    public function unrelated(): void\n" +
            "    {\n" +
            "    }\n" +
            "}"
        );

        PhpClass phpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());

        assertImplicitUsage(phpClass.findOwnMethodByName("beforePersist"));
        assertImplicitUsage(phpClass.findOwnMethodByName("beforeFlush"));
        assertImplicitUsage(phpClass.findOwnMethodByName("afterLoad"));
        assertNotImplicitUsage(phpClass.findOwnMethodByName("unrelated"));
    }

    public void testDoctrineAsEntityListenerMethodIsMarkedUsed() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "declare(strict_types=1);\n" +
            "\n" +
            "namespace App\\Usage\\Doctrine;\n" +
            "\n" +
            "use Doctrine\\Bundle\\DoctrineBundle\\Attribute\\AsEntityListener;\n" +
            "use Doctrine\\ORM\\Event\\PostPersistEventArgs;\n" +
            "use Doctrine\\ORM\\Events;\n" +
            "use App\\Entity\\FooEntity;\n" +
            "\n" +
            "#[AsEntityListener(event: Events::postPersist, method: 'whenPostPersist', entity: FooEntity::class)]\n" +
            "final class AsEntityListenerFixture\n" +
            "{\n" +
            "    public function whenPostPersist(FooEntity $entity, PostPersistEventArgs $event): void\n" +
            "    {\n" +
            "    }\n" +
            "\n" +
            "    public function unrelated(): void\n" +
            "    {\n" +
            "    }\n" +
            "}"
        );

        PhpClass phpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());

        assertImplicitUsage(phpClass);
        assertImplicitUsage(phpClass.findOwnMethodByName("whenPostPersist"));
        assertNotImplicitUsage(phpClass.findOwnMethodByName("unrelated"));
    }

    private PhpClass createPhpControllerClassWithRouteContent(@NotNull String content) {
        return createPhpControllerClassWithRouteContent("\\App\\Controller\\FooController", content);
    }

    private PhpClass createPhpControllerClassWithRouteContent(@NotNull String className, @NotNull String content) {
        String[] split = StringUtils.stripStart(className, "\\").split("\\\\");

        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php" +
            "<?php\n" +
            "namespace " + StringUtils.join(Arrays.copyOf(split, split.length - 1), "\\") + ";\n" +
            "\n" +
            "use Symfony\\Component\\Routing\\Annotation\\Route;\n" +
            "\n" +
            "class " + split[split.length - 1] + "\n" +
            "{\n" +
            "" + content + "\n" +
            "}"
        );

        return PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());
    }

    @NotNull
    private Method createPhpControllerMethodWithRouteContent(@NotNull String content) {
        PhpClass phpClass = createPhpControllerClassWithRouteContent("\\App\\Controller\\FooController", content);
        return phpClass.getMethods().iterator().next();
    }

    private void assertImplicitUsage(@NotNull Method method) {
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(method));
    }

    private void assertImplicitUsage(@NotNull PhpClass phpClass) {
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass));
    }

    private void assertNotImplicitUsage(@NotNull Method method) {
        assertFalse(new SymfonyImplicitUsageProvider().isImplicitUsage(method));
    }

    private void assertNotImplicitUsage(@NotNull PhpClass phpClass) {
        assertFalse(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass));
    }
}
