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
                "        #[Autowire('fo<caret>o')]\n" +
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
                "        #[Autowire(value: 'fo<caret>o')]\n" +
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
}
