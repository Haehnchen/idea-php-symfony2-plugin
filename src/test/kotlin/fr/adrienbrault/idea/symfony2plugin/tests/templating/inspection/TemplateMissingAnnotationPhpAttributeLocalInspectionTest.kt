package fr.adrienbrault.idea.symfony2plugin.tests.templating.inspection

import fr.adrienbrault.idea.symfony2plugin.templating.inspection.TemplateMissingAnnotationPhpAttributeLocalInspection
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TemplateMissingAnnotationPhpAttributeLocalInspection
 */
class TemplateMissingAnnotationPhpAttributeLocalInspectionTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.copyFileToProject("classes.php")
    }

    override fun getTestDataPath(): String {
        return "src/test/kotlin/fr/adrienbrault/idea/symfony2plugin/tests/templating/inspection/fixtures"
    }

    fun testThatTemplateCreationAnnotationProvidesQuickfix() {
        assertLocalInspectionContains(TemplateMissingAnnotationPhpAttributeLocalInspection::class.java, "foobar.php", "<?php\n" +
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
        )

        assertLocalInspectionContains(TemplateMissingAnnotationPhpAttributeLocalInspection::class.java, "foobar.php", "<?php\n" +
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
        )

        assertLocalInspectionContains(TemplateMissingAnnotationPhpAttributeLocalInspection::class.java, "foobar.php", "<?php\n" +
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
        )
    }

    fun testThatTemplateCreationAnnotationProvidesQuickfixForPhpAttribute() {
        assertLocalInspectionContains(TemplateMissingAnnotationPhpAttributeLocalInspection::class.java, "foobar.php", "<?php\n" +
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
        )

        assertLocalInspectionContains(TemplateMissingAnnotationPhpAttributeLocalInspection::class.java, "foobar.php", "<?php\n" +
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
        )
    }

    fun testThatTemplateCreationForInvokeMethodProvidesQuickfixForPhpAttribute() {
        myFixture.copyFileToProject("controller_method.php")

        assertLocalInspectionContains(TemplateMissingAnnotationPhpAttributeLocalInspection::class.java, "foobar.php", "<?php\n" +
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
        )
    }

    fun testThatTemplateCreationForInvokeMethodProvidesQuickfix() {
        myFixture.copyFileToProject("controller_method.php")

        assertLocalInspectionContains(TemplateMissingAnnotationPhpAttributeLocalInspection::class.java, "foobar.php", "<?php\n" +
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
        )
    }

    fun testThatMissingTemplateForGlobalNamespaceWithoutBundleScopeForController() {
        assertLocalInspectionContains(TemplateMissingAnnotationPhpAttributeLocalInspection::class.java, "foobar.php", "<?php\n" +
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
        )
    }
}
