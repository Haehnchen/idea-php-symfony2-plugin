package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.RenderParameterGotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.RenderParameterGotoCompletionRegistrar
 */
public class RenderParameterGotoCompletionRegistrarTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("RenderParameterGotoCompletionRegistrar.php");
        myFixture.copyFileToProject("ide-twig.json");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    public void testTemplateNameExtractionForFunction() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php foo('foo.html.twig', ['<caret>']);");
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertContainsElements(RenderParameterGotoCompletionRegistrar.getTemplatesForScope(psiElement), "foo.html.twig");

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php foo('foo.html.twig', ['<caret>' => 'foo']);");
        psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertContainsElements(RenderParameterGotoCompletionRegistrar.getTemplatesForScope(psiElement), "foo.html.twig");
    }

    public void testTemplateNameExtractionForFunctionForFunctions() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php foo('foo.html.twig', array_merge(['<caret>' => 'foo']));");
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        assertContainsElements(RenderParameterGotoCompletionRegistrar.getTemplatesForScope(psiElement), "foo.html.twig");
    }

    public void testSpecialReferencesTemplateNameResolve() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php $var = 'foo.html.twig'; foo($var, ['<caret>']);");
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        assertContainsElements(RenderParameterGotoCompletionRegistrar.getTemplatesForScope(psiElement), "foo.html.twig");
    }

    public void testTemplateNameExtractionForFunctionForFunctionsAsReturn() {
        myFixture.configureByText(PhpFileType.INSTANCE, "" +
            "<?php\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
            "class Foo\n" +
            "{\n" +
            "   /**" +
            "   * @Template(\"foo.html.twig\")" +
            "   */" +
            "   function foo()\n" +
            "   {\n" +
            "       return ['<caret>' => 'foo']);\n" +
            "   }" +
            "}\n"
        );
        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());

        assertContainsElements(RenderParameterGotoCompletionRegistrar.getTemplatesForScope(psiElement), "foo.html.twig");
    }

    /**
     * Test that navigation from controller name goes to the method.
     */
    public void testRenderControllerNavigation() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "class NavController\n" +
            "{\n" +
            "    public function menuAction()\n" +
            "    {\n" +
            "    }\n" +
            "}\n"
        );

        assertNavigationMatch(
            TwigFileType.INSTANCE,
            "{{ render(controller('App\\\\Controller\\\\Nav<caret>Controller::menuAction')) }}",
            PlatformPatterns.psiElement(Method.class).withName("menuAction")
        );
    }

    /**
     * Test that template variable completion works in render() parameter array.
     */
    public void testTemplateVariableCompletionInRender() {
        // Create a template with variables
        myFixture.addFileToProject(
            "templates/form/contact.html.twig",
            "{# @var name string #}\n" +
            "{# @var email string #}\n" +
            "{# @var message string #}\n" +
            "{{ name }}\n" +
            "{{ email }}\n" +
            "{{ message }}"
        );

        // Completion should suggest variables from the template
        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php $this->render('form/contact.html.twig', ['<caret>']);",
            "name",
            "email",
            "message"
        );
    }

    /**
     * Test that template variable completion works with array_merge.
     */
    public void testTemplateVariableCompletionWithArrayMerge() {
        myFixture.addFileToProject(
            "templates/email/welcome.html.twig",
            "{# @var user_name string #}\n" +
            "{# @var activation_link string #}\n" +
            "{{ user_name }}\n" +
            "{{ activation_link }}"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php $this->render('email/welcome.html.twig', array_merge(['<caret>'], $vars));",
            "user_name",
            "activation_link"
        );
    }

    /**
     * Test that navigation from variable name goes to template usage.
     */
    public void testTemplateVariableNavigation() {
        myFixture.addFileToProject(
            "templates/view/detail.html.twig",
            "{# @var title string #}\n" +
            "{# @var description string #}\n" +
            "<h1>{{ title }}</h1>\n" +
            "<p>{{ description }}</p>"
        );

        assertNavigationMatch(
            PhpFileType.INSTANCE,
            "<?php $this->render('view/detail.html.twig', ['tit<caret>le' => 'My Title']);",
            PlatformPatterns.psiElement()
        );
    }

    /**
     * Test that completion works in @Template annotated method return array.
     */
    public void testTemplateVariableCompletionInAnnotationReturn() {
        myFixture.addFileToProject(
            "templates/article/view.html.twig",
            "{# @var article_title string #}\n" +
            "{# @var article_content string #}\n" +
            "{{ article_title }}\n" +
            "{{ article_content }}"
        );

        assertCompletionContains(
            PhpFileType.INSTANCE,
            "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
            "\n" +
            "class ArticleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Template(\"article/view.html.twig\")\n" +
            "     */\n" +
            "    public function viewAction()\n" +
            "    {\n" +
            "        return ['<caret>'];\n" +
            "    }\n" +
            "}\n",
            "article_title",
            "article_content"
        );
    }
}
