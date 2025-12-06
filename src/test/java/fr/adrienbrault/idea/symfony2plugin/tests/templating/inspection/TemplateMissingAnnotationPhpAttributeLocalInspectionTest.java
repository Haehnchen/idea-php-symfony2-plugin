package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection;

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TemplateMissingAnnotationPhpAttributeLocalInspection;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TemplateMissingAnnotationPhpAttributeLocalInspection
 */
public class TemplateMissingAnnotationPhpAttributeLocalInspectionTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();

        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/inspection/fixtures";
    }

    public void testThatTemplateCreationAnnotationProvidesQuickfix() {
        assertLocalInspectionContains("foobar.php", "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "\n" +
                "class Foobar\n" +
                "{\n" +
                "   /**\n" +
                "   * @Temp<caret>late(\"foobar.html.twig\")\n" +
                "   */\n" +
                "   public function fooAction()\n" +
                "   {\n" +
                "   }\n" +
                "}\n" +
                "",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains("foobar.php", "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "\n" +
                "class Foobar\n" +
                "{\n" +
                "   /**\n" +
                "   * @Temp<caret>late(template=\"foobar.html.twig\")\n" +
                "   */\n" +
                "   public function fooAction()\n" +
                "   {\n" +
                "   }\n" +
                "}\n" +
                "",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains("foobar.php", "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "\n" +
                "class Foobar\n" +
                "{\n" +
                "   /**\n" +
                "   * @Temp<caret>late(\"foobar.html.twig\")\n" +
                "   */\n" +
                "   #[Route]\n" +
                "   public function fooAction()\n" +
                "   {\n" +
                "   }\n" +
                "}\n" +
                "",
            "Twig: Missing Template"
        );
    }

    public void testThatTemplateCreationAnnotationProvidesQuickfixForPhpAttribute() {
        assertLocalInspectionContains("foobar.php", "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "\n" +
                "class Foobar\n" +
                "{\n" +
                "   #[Temp<caret>late(\"foobar.html.twig\")]\n" +
                "   public function fooAction()\n" +
                "   {\n" +
                "   }\n" +
                "}\n" +
                "",
            "Twig: Missing Template"
        );

        assertLocalInspectionContains("foobar.php", "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "\n" +
                "class Foobar\n" +
                "{\n" +
                "   #[Temp<caret>late(template: \"foobar.html.twig\")]\n" +
                "   public function fooAction()\n" +
                "   {\n" +
                "   }\n" +
                "}\n" +
                "",
            "Twig: Missing Template"
        );
    }

    public void testThatTemplateCreationForInvokeMethodProvidesQuickfixForPhpAttribute() {
        myFixture.copyFileToProject("controller_method.php");

        assertLocalInspectionContains("foobar.php", "<?php\n" +
                "namespace FooBundle\\Controller;\n" +
                "\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "\n" +
                "class FoobarController\n" +
                "{\n" +
                "   #[Temp<caret>late(\"foobar.html.twig\")]\n" +
                "   public function __invoke()\n" +
                "   {\n" +
                "   }\n" +
                "}\n" +
                "",
            "Twig: Missing Template"
        );
    }

    public void testThatTemplateCreationForInvokeMethodProvidesQuickfix() {
        myFixture.copyFileToProject("controller_method.php");

        assertLocalInspectionContains("foobar.php", "<?php\n" +
                "namespace FooBundle\\Controller;\n" +
                "\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "\n" +
                "class FoobarController\n" +
                "{\n" +
                "   /**\n" +
                "   * @Temp<caret>late()\n" +
                "   */\n" +
                "   public function __invoke()\n" +
                "   {\n" +
                "   }\n" +
                "}\n" +
                "",
            "Twig: Missing Template"
        );
    }

    public void testThatMissingTemplateForGlobalNamespaceWithoutBundleScopeForController() {
        assertLocalInspectionContains("foobar.php", "<?php\n" +
                "namespace FoobarApp\\Controller;\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "\n" +
                "class FoobarController\n" +
                "{\n" +
                "   /**\n" +
                "   * @Temp<caret>late()\n" +
                "   */\n" +
                "   public function fooAction()\n" +
                "   {\n" +
                "   }\n" +
                "}\n" +
                "",
            "Twig: Missing Template"
        );
    }
}
