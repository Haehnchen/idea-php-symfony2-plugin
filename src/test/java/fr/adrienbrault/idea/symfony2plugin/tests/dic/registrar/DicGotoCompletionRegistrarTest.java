package fr.adrienbrault.idea.symfony2plugin.tests.dic.registrar;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DicGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));
        myFixture.copyFileToProject("services.yml");
        myFixture.copyFileToProject(".env");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/dic/registrar/fixtures";
    }

    public void testParameterContributor() {
        for (String s : new String[]{"getParameter", "hasParameter"}) {
            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $f \\Symfony\\Component\\DependencyInjection\\ContainerInterface */ \n" +
                    String.format("$f->%s('<caret>')", s),
                "foo"
            );

            assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $f \\Symfony\\Component\\DependencyInjection\\ContainerInterface */ \n" +
                    String.format("$f->%s('foo<caret>')", s),
                PlatformPatterns.psiElement()
            );
        }
    }

    public void testParameterContributorProxied() {
        for (String s : new String[]{"foo", "bar"}) {
            assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                    String.format("(new \\Foo())->%s('<caret>')", s),
                "foo"
            );

            assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                    "/** @var $f \\Symfony\\Component\\DependencyInjection\\ContainerInterface */ \n" +
                    String.format("(new \\Foo())->%s('foo<caret>')", s),
                PlatformPatterns.psiElement()
            );
        }
    }

    public void testParameterContributorFor() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "param('<caret>')",
            "foo"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "param('fo<caret>o')",
            PlatformPatterns.psiElement()
        );
    }

    public void testParameterContributorForDefaultAttribute() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire('<caret>')]\n" +
                "        private $parameter2" +
                "    ) {}\n" +
                "}",
            "foo"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(\"<caret>\")]\n" +
                "        private $parameter2" +
                "    ) {}\n" +
                "}",
            "foo"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire('%fo<caret>o%')]\n" +
                "        private $parameter2" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testParameterContributorForNamedAttribute() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(value: '<caret>')]\n" +
                "        private $parameter2" +
                "    ) {}\n" +
                "}",
            "foo"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(value: '%fo<caret>o%')]\n" +
                "        private $parameter2" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testTagContributorForTaggedIterator() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\TaggedIterator;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "    public function __construct(\n" +
                "        #[TaggedIterator('<caret>')] iterable $handlers\n" +
                "    ) {}\n" +
                "}",
            "yaml_type_tag"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\TaggedIterator;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "    public function __construct(\n" +
                "        #[TaggedIterator(tag: '<caret>')] iterable $handlers\n" +
                "    ) {}\n" +
                "}",
            "yaml_type_tag"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\TaggedIterator;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[TaggedIterator('yaml_t<caret>ype_tag')] iterable $handlers\n" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testTagContributorForTaggedLocator() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\TaggedLocator;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "    public function __construct(\n" +
                "        #[TaggedLocator('<caret>')] ContainerInterface $handlers\n" +
                "    ) {}\n" +
                "}",
            "yaml_type_tag"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\TaggedLocator;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "    public function __construct(\n" +
                "        #[TaggedLocator(tag: '<caret>')] ContainerInterface $handlers\n" +
                "    ) {}\n" +
                "}",
            "yaml_type_tag"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\TaggedLocator;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[TaggedLocator('yaml_t<caret>ype_tag')] ContainerInterface $handlers\n" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testServiceContributorForNamedAttribute() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(service: '<caret>')]" +
                "    ) {}\n" +
                "}",
            "foo_bar_service"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(service: 'foo_bar<caret>_service')] $handlers\n" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testServiceContributorDecoratesAttribute() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AsDecorator;\n" +
                "#[AsDecorator('<caret>')]\n" +
                "class HandlerCollection {}",
            "foo_bar_service"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AsDecorator;\n" +
                "#[AsDecorator(decorates: 'foo_bar<caret>_service')]\n" +
                "class HandlerCollection {}",
            PlatformPatterns.psiElement()
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AsDecorator;\n" +
                "#[AsDecorator(decorates: '<caret>')]\n" +
                "class HandlerCollection {}",
            "foo_bar_service"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AsDecorator;\n" +
                "#[AsDecorator(decorates: 'foo_bar<caret>_service')]\n" +
                "class HandlerCollection {}",
            PlatformPatterns.psiElement()
        );
    }

    public void testTagContributorForAutoconfigureTagsAttribute() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autoconfigure;\n" +
                "#[Autoconfigure(['<caret>'])]\n" +
                "class HandlerCollection {}",
            "yaml_type_tag"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autoconfigure;\n" +
                "#[Autoconfigure(['yaml_<caret>type_tag'])]\n" +
                "class HandlerCollection {}",
            PlatformPatterns.psiElement()
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autoconfigure;\n" +
                "#[Autoconfigure(tags: ['<caret>'])]\n" +
                "class HandlerCollection {}",
            "yaml_type_tag"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autoconfigure;\n" +
                "#[Autoconfigure(tags: ['yaml_<caret>type_tag'])]\n" +
                "class HandlerCollection {}",
            PlatformPatterns.psiElement()
        );
    }

    public void testTagContributorForWhenAttribute() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\When;\n" +
                "#[When('<caret>')]\n" +
                "class HandlerCollection {}",
            "dev", "test", "prod"
        );
    }

    public void testParameterContributorForParamAttribute() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(param: '<caret>')]\n" +
                "        private bool $debugMode" +
                "    ) {}\n" +
                "}",
            "foo"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(param: 'fo<caret>o')]\n" +
                "        private bool $debugMode" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testEnvironmentVariableContributorForEnvAttribute() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(env: '<caret>')]\n" +
                "        private string $senderName" +
                "    ) {}\n" +
                "}",
            "DATABASE_URL", "APP_ENV", "SOME_ENV_VAR"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(env: 'DATABASE<caret>_URL')]\n" +
                "        private string $senderName" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testEnvironmentVariableContributorForEnvAttributeWithProcessor() {
        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(env: 'bool:SOME_ENV<caret>_VAR')]\n" +
                "        private bool $allowAttachments" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(env: 'int:APP<caret>_ENV')]\n" +
                "        private int $someValue" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\Autowire;\n" +
                "\n" +
                "class MyService\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[Autowire(env: 'resolve:DATABASE<caret>_URL')]\n" +
                "        private string $dbUrl" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testServiceContributorForAutowireServiceClosure() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireServiceClosure;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireServiceClosure('<caret>')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            "foo_bar_service"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireServiceClosure;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireServiceClosure(service: '<caret>')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            "foo_bar_service"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireServiceClosure;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireServiceClosure('foo_bar<caret>_service')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testServiceContributorForAutowireMethodOf() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireMethodOf;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireMethodOf('<caret>')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            "foo_bar_service"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireMethodOf;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireMethodOf(service: '<caret>')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            "foo_bar_service"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireMethodOf;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireMethodOf('foo_bar<caret>_service')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testServiceContributorForAutowireCallable() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireCallable;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireCallable(service: '<caret>')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            "foo_bar_service"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireCallable;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireCallable(service: 'foo_bar<caret>_service')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testMethodContributorForAutowireCallable() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireCallable;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireCallable(service: 'foo_bar_service', method: '<caret>')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            "format", "process"
        );

        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireCallable;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireCallable(service: 'foo_bar_service', method: '<caret>')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            "privateMethod", "staticMethod"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireCallable;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireCallable(service: 'foo_bar_service', method: 'for<caret>mat')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }

    public void testMethodContributorForAutowireCallableWithClassConstant() {
        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireCallable;\n" +
                "use Foo\\Bar;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireCallable(service: Bar::class, method: '<caret>')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            "format", "process"
        );

        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireCallable;\n" +
                "use Foo\\Bar;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireCallable(service: Bar::class, method: '<caret>')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            "privateMethod", "staticMethod"
        );

        assertNavigationMatch(PhpFileType.INSTANCE, "<?php\n" +
                "use Symfony\\Component\\DependencyInjection\\Attribute\\AutowireCallable;\n" +
                "use Foo\\Bar;\n" +
                "\n" +
                "class HandlerCollection\n" +
                "{\n" +
                "    public function __construct(\n" +
                "        #[AutowireCallable(service: Bar::class, method: 'for<caret>mat')]\n" +
                "        private $formatter" +
                "    ) {}\n" +
                "}",
            PlatformPatterns.psiElement()
        );
    }
}
