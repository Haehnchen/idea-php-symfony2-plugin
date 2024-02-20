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

    public void testControllerClassIsUsedWhenAMethodHasRoute() {
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "#[Route()]\n" +
            "public function foo2() {}"
        )));

        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "/**\n" +
            "* @Route()\n" +
            "*/\n" +
            "public function foo() {}\n"
        )));
    }

    public void testControllerClassIsUnusedIfRoutesArePrivate() {
        assertFalse(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "/**\n" +
            "* @Route()\n" +
            "*/\n" +
            "private function foo() {}\n" +
            "#[Route()]\n" +
            "private function foo2() {}"
        )));
    }

    public void testControllerMethodIsUsedWhenAMethodIsHasRouteDefinition() {
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerMethodWithRouteContent("" +
            "/**\n" +
            "* @Route()\n" +
            "*/\n" +
            "public function foobar() {}"
        )));

        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerMethodWithRouteContent("" +
            "#[Route()]\n" +
            "public function foobar() {}"
        )));
    }

    public void testControllerMethodIsUntouchedForPrivateMethods() {
        assertFalse(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerMethodWithRouteContent("" +
            "/**\n" +
            "* @Route()\n" +
            "*/\n" +
            "private function foobar() {}"
        )));

        assertFalse(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerMethodWithRouteContent("" +
            "#[Route()]\n" +
            "private function foobar() {}"
        )));
    }

    public void testControllerForDefinitionInsideYaml() {
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "public function foobarYaml() {}"
        )));

        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerClassWithRouteContent(
            "\\App\\Controller\\FooControllerInvoke",
            "public function __invoke() {}"
        )));
    }

    public void testControllerForDefinitionInsideYamlWithAction() {
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerClassWithRouteContent("" +
            "public function foobarYamlAction() {}"
        )));
    }

    public void testControllerForDefinitionInsideYamlAsService() {
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerClassWithRouteContent(
            "\\App\\Controller\\FooControllerService",
            "public function foo() {}"
        )));

        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerClassWithRouteContent(
            "\\App\\Controller\\FooControllerService",
            "public function foo() {}"
        )));

        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(createPhpControllerClassWithRouteContent(
            "\\App\\Controller\\FooControllerServiceInvoke",
            "public function __invoke() {}"
        )));
    }

    public void testCommandRegisteredAsServiceAreMarkedUsed() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Command;\n" +
            "class FooCommand extends \\Symfony\\Component\\Console\\Command\\Command {}"
        );

        PhpClass firstClassFromFile = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(firstClassFromFile));
    }

    public void testCommandRegisteredNotAsServiceIsUntouched() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Command;\n" +
            "class FoobarServiceCommand extends \\Symfony\\Component\\Console\\Command\\Command {}"
        );

        PhpClass firstClassFromFile = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());
        assertFalse(new SymfonyImplicitUsageProvider().isImplicitUsage(firstClassFromFile));
    }

    public void testVoterRegisteredAsServiceAreMarkedUsed() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Voter;\n" +
            "class MyFoobarVoter implements \\Symfony\\Component\\Security\\Core\\Authorization\\Voter\\VoterInterface {}"
        );

        PhpClass firstClassFromFile = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(firstClassFromFile));
    }

    public void testEntityRepositoryInsideDoctrineMetadataIsMarkedAsUsed() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Repository;\n" +
            "class MyFoobarEntityRepository extends \\Doctrine\\ORM\\EntityRepository {}\n"
        );

        PhpClass firstClassFromFile = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(firstClassFromFile));
    }

    public void testConstraintValidatorReferenceIsMarkedAsUsed() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Validator;\n" +
            "class MyFoobarConstraint extends \\Symfony\\Component\\Validator\\Constraint {}\n"
        );

        PhpClass firstClassFromFile = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(firstClassFromFile));
    }

    public void testClassWithTaggedEventListenerIsMarkedAsUsed() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\EventListener;\n" +
            "class ExceptionListener {}\n"
        );

        PhpClass firstClassFromFile = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(firstClassFromFile));
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

        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass.findOwnMethodByName("processException")));
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass.findOwnMethodByName("logException")));
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass.findOwnMethodByName("notifyException")));

        assertFalse(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass.findOwnMethodByName("notifyExceptionUnknown")));
        assertFalse(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass.findOwnMethodByName("keyString")));

        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass));
    }


    public void testEventSubscriberGetAsEventListenerOnClass() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
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

        PhpClass phpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());

        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass.findOwnMethodByName("onFoo")));
    }

    public void testEventSubscriberGetAsEventListenerOnMethod() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
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

        PhpClass phpClass = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());

        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(phpClass.findOwnMethodByName("onFoo")));
    }

    public void testTwigExtensionRegisteredAsServiceWithFunctionMethodImplementedIsMarkedUsed() {
        PsiFile psiFile = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "\n" +
            "namespace App\\TwigExtension;\n" +
            "\n" +
            "class MyTwigExtension implements \\Twig\\Extension\\ExtensionInterface\n" +
            "{\n" +
            "    public function getFilters() {}\n" +
            "}"
        );

        PhpClass firstClassFromFile = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile.getContainingFile());
        assertTrue(new SymfonyImplicitUsageProvider().isImplicitUsage(firstClassFromFile));

        PsiFile psiFile2 = myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "\n" +
            "namespace App\\TwigExtension;\n" +
            "\n" +
            "class MyTwigExtension implements \\Twig\\Extension\\ExtensionInterface\n" +
            "{\n" +
            "    public function getFoobar() {}\n" +
            "}"
        );

        PhpClass firstClassFromFile2 = PhpElementsUtil.getFirstClassFromFile((PhpFile) psiFile2.getContainingFile());
        assertFalse(new SymfonyImplicitUsageProvider().isImplicitUsage(firstClassFromFile2));
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
}
