package fr.adrienbrault.idea.symfony2plugin.tests.twig.annotation;

import com.intellij.codeInsight.daemon.impl.AnnotationHolderImpl;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationDocTagAnnotatorParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import fr.adrienbrault.idea.symfony2plugin.twig.annotation.TemplateAnnotationAnnotator;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TemplateAnnotationAnnotator
 */
public class TemplateAnnotationAnnotatorTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("classes.php");
    }

    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/twig/annotation/fixtures";
    }

    /**
     * @see TemplateAnnotationAnnotator#annotate
     */
    public void testThatTemplateCreationAnnotationProvidesQuickfix() {
        PsiFile psiFile = myFixture.configureByText("foobar.php", "<?php\n" +
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
            ""
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement phpDocTag = psiElement.getParent();

        AnnotationHolderImpl annotations = new AnnotationHolderImpl(new AnnotationSession(psiFile));

        new TemplateAnnotationAnnotator().annotate(new PhpAnnotationDocTagAnnotatorParameter(
            PhpIndex.getInstance(getProject()).getAnyByFQN(TwigUtil.TEMPLATE_ANNOTATION_CLASS).iterator().next(),
            (PhpDocTag) phpDocTag,
            annotations
        ));

        assertNotNull(
            annotations.stream().findFirst().filter(annotation -> annotation.getMessage().contains("Create Template"))
        );
    }

    /**
     * @see TemplateAnnotationAnnotator#annotate
     */
    public void testThatTemplateCreationForInvokeMethodProvidesQuickfix() {
        myFixture.copyFileToProject("controller_method.php");

        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace FooBundle\\Controller;\n" +
            "class FoobarController\n" +
            "{\n" +
            "   public function __in<caret>voke() {}\n" +
            "" +
            "}\n"
        );

        PsiFile psiFile = myFixture.configureByText("foobar.php", "<?php\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
            "\n" +
            "class Foobar\n" +
            "{\n" +
            "   /**\n" +
            "   * @Temp<caret>late()\n" +
            "   */\n" +
            "   public function __invoke()\n" +
            "   {\n" +
            "   }\n" +
            "}\n" +
            ""
        );

        PsiElement psiElement = myFixture.getFile().findElementAt(myFixture.getCaretOffset());
        PsiElement phpDocTag = psiElement.getParent();

        AnnotationHolderImpl annotations = new AnnotationHolderImpl(new AnnotationSession(psiFile));

        new TemplateAnnotationAnnotator().annotate(new PhpAnnotationDocTagAnnotatorParameter(
            PhpIndex.getInstance(getProject()).getAnyByFQN(TwigUtil.TEMPLATE_ANNOTATION_CLASS).iterator().next(),
            (PhpDocTag) phpDocTag,
            annotations
        ));

        assertNotNull(
            annotations.stream().findFirst().filter(annotation -> annotation.getMessage().contains("Create Template"))
        );
    }
}
