package fr.adrienbrault.idea.symfony2plugin.tests.completion;

import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.completion.IncompletePropertyServiceInjectionContributor
 */
public class IncompletePropertyServiceInjectionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("classes_services.yml");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/completion/yaml/fixtures";
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
}
