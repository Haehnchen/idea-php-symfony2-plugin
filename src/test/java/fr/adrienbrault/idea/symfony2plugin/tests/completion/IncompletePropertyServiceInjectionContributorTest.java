package fr.adrienbrault.idea.symfony2plugin.tests.completion;

import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpPsiElementFactory;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.completion.IncompletePropertyServiceInjectionContributor;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.completion.IncompletePropertyServiceInjectionContributor
 */
public class IncompletePropertyServiceInjectionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("IncompletePropertyServiceInjectionContributor.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/completion/fixtures";
    }

    public void testInjectionCompletionUnknownPropertyProvidesInjectionCompletion() {
        if (true) {
            return;
        }

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    public function __construct()\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->trans<caret>lator\n" +
                "    }\n" +
                "}",
            "translator", "translator->trans();"
        );

        assertCompletionContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->trans<caret>lator\n" +
                "    }\n" +
                "}",
            "translator", "translator->trans();"
        );
    }

    public void testInjectionCompletionUnknownPropertyProvidesWithConstructor() {
        if (true) {
            return;
        }

        assertCompletionResultEquals(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    public function __construct()\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->translator<caret>\n" +
                "    }\n" +
                "}",
            "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "use Symfony\\Contracts\\Translation\\TranslatorInterface;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    /**\n" +
                "     * @var TranslatorInterface\n" +
                "     */\n" +
                "    private $translator;\n" +
                "\n" +
                "    public function __construct(TranslatorInterface $translator)\n" +
                "    {\n" +
                "        $this->translator = $translator;\n" +
                "    }\n" +
                "\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->translator\n" +
                "    }\n" +
                "}",
            lookupElement -> lookupElement.getLookupString().equals("translator")
        );
    }

    public void testInjectionCompletionUnknownPropertyWithoutConstructorCompletion() {
        if (true) {
            return;
        }

        assertCompletionResultEquals(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar" +
                "{\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->translator<caret>\n" +
                "    }\n" +
                "}",
            "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "use Symfony\\Contracts\\Translation\\TranslatorInterface;\n" +
                "\n" +
                "class TestFoobar" +
                "{\n" +
                "    /**\n" +
                "     * @var TranslatorInterface\n" +
                "     */\n" +
                "    private $translator;\n" +
                "\n" +
                "    public function __construct(TranslatorInterface $translator)\n" +
                "    {\n" +
                "        $this->translator = $translator;\n" +
                "    }\n" +
                "\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->translator\n" +
                "    }\n" +
                "}",
            lookupElement -> lookupElement.getLookupString().equals("translator")
        );
    }

    public void testInjectionCompletionNotProvidedForPrivateConstructor() {
        if (true) {
            return;
        }

        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    private function __construct()\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->trans<caret>lator\n" +
                "    }\n" +
                "}",
            "translator"
        );
    }

    public void testInjectionCompletionNotProvidedForInvalidConstructor() {
        if (true) {
            return;
        }

        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    private function __construct()\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->trans<caret>lator\n" +
                "    }\n" +
                "}",
            "translator"
        );
    }

    public void testInjectionCompletionNotProvidedForAlreadyExistingTypPath() {
        if (true) {
            return;
        }

        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    private $translator;\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->trans<caret>lator\n" +
                "    }\n" +
                "}",
            "translator->trans();"
        );

        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    /**\n" +
                "     * @var \\Symfony\\Contracts\\Translation\\TranslatorInterface\n" +
                "     */\n" +
                "    private $t;\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->trans<caret>lator\n" +
                "    }\n" +
                "}",
            "translator"
        );

        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    public function __construct($translator)\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->trans<caret>lator\n" +
                "    }\n" +
                "}",
            "translator"
        );

        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class TestFoobar\n" +
                "{\n" +
                "    public function __construct(\\Symfony\\Contracts\\Translation\\TranslatorInterface $x)\n" +
                "    {\n" +
                "    }\n" +
                "\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->trans<caret>lator\n" +
                "    }\n" +
                "}",
            "translator"
        );
    }

    public void testInjectionCompletionNotProvidedForNonService() {
        if (true) {
            return;
        }
        
        assertCompletionNotContains(PhpFileType.INSTANCE, "<?php\n" +
                "\n" +
                "namespace App;\n" +
                "\n" +
                "class UnknownService\n" +
                "{\n" +
                "    public function testFoo()\n" +
                "    {\n" +
                "       $this->trans<caret>lator\n" +
                "    }\n" +
                "}",
            "translator"
        );
    }

    public void testAppendPropertyInjection() {
        PhpClass fromText = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "\n" +
            "class Foobar\n" +
            "{\n" +
            "    public function __construct(private readonly \\DateTime $d)\n" +
            "    {\n" +
            "    }\n" +
            "}"
        );

        IncompletePropertyServiceInjectionContributor.appendPropertyInjection(fromText, "router", "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        String text = fromText.getText();
        assertTrue(text.contains("public function __construct(private readonly \\DateTime $d,private readonly UrlGeneratorInterface $router)"));

        PhpClass fromText2 = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "\n" +
            "class Foobar\n" +
            "{\n" +
            "}"
        );

        IncompletePropertyServiceInjectionContributor.appendPropertyInjection(fromText2, "router", "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        String text2 = fromText2.getText();
        assertTrue(text2.contains("public function __construct(UrlGeneratorInterface $router)"));


        PhpClass fromText3 = PhpPsiElementFactory.createFromText(getProject(), PhpClass.class, "<?php\n" +
            "\n" +
            "readonly class Foobar\n" +
            "{\n" +
            "    public function __construct(private readonly \\DateTime $d)\n" +
            "    {\n" +
            "    }\n" +
            "}"
        );

        IncompletePropertyServiceInjectionContributor.appendPropertyInjection(fromText3, "router", "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        String text3 = fromText3.getText();
        assertTrue(text3.contains("public function __construct(private readonly \\DateTime $d,private UrlGeneratorInterface $router)"));
    }

    public void testInjectionService() {
        List<String> classes1 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "router");
        assertContainsElements(classes1, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes2 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "urlgenerator");
        assertContainsElements(classes2, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes3 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "urlGenerator");
        assertContainsElements(classes3, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes4 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "_urlGenerator");
        assertContainsElements(classes4, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes5 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "__url_generator");
        assertContainsElements(classes5, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes6 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "_router");
        assertContainsElements(classes6, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes7 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "foobar");
        assertContainsElements(classes7, "\\App\\Service\\FoobarInterface");

        List<String> classes8 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "_routerInterface");
        assertContainsElements(classes8, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes9 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "foobarCar");
        assertContainsElements(classes9, "\\App\\Service\\InterfaceFoobarCar");

        List<String> classes10 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "foobarCarInterface");
        assertContainsElements(classes10, "\\App\\Service\\InterfaceFoobarCar");

        List<String> classes11 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "fooBarLogger");
        assertContainsElements(classes11, "\\Psr\\Log\\LoggerInterface");

        List<String> classes12 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "foobarLongClassNameServiceFactory");
        assertContainsElements(classes12, "\\App\\Service\\FoobarLongClassNameServiceFactory");

        List<String> classes13 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "longClassNameServiceFactory");
        assertContainsElements(classes13, "\\App\\Service\\FoobarLongClassNameServiceFactory");

        List<String> classes14 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "nameServiceFactory");
        assertContainsElements(classes14, "\\App\\Service\\FoobarLongClassNameServiceFactory");

        List<String> classes15 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "serviceFactory");
        assertFalse(classes15.contains("\\App\\Service\\FoobarLongClassNameServiceFactory"));

        List<String> classes16 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "_name_Service__Factory");
        assertContainsElements(classes16, "\\App\\Service\\FoobarLongClassNameServiceFactory");
    }

    public void testInjectionServiceWithName() {
        List<String> classes1 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "urlGenerator", "foobarUnknown");
        assertContainsElements(classes1, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");

        List<String> classes2 = IncompletePropertyServiceInjectionContributor.getInjectionService(getProject(), "urlGenerator", "generate");
        assertContainsElements(classes2, "\\Symfony\\Component\\Routing\\Generator\\UrlGeneratorInterface");
    }
}
