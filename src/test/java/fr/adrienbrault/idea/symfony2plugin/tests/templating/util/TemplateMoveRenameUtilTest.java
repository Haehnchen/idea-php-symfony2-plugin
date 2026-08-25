package fr.adrienbrault.idea.symfony2plugin.tests.templating.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigTemplateUsageReference;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TemplateMoveRenameUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Tests for {@link TemplateMoveRenameUtil} and the {@code bindToElement} implementations in
 * {@link TemplateReference} and {@link TwigTemplateUsageReference}.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateMoveRenameUtilTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Expose both a plain namespace ("") and a named one ("App") so both prefix styles resolve.
        myFixture.addFileToProject(
            "ide-twig.json",
            "{\"namespaces\":[{\"namespace\":\"\",\"path\":\"templates\"},{\"namespace\":\"App\",\"path\":\"templates\"}]}"
        );
    }

    /**
     * @see TemplateMoveRenameUtil#pickBestTemplateName
     */
    public void testPickBestTemplateNameReturnsSingleName() {
        assertEquals(
            "new/template.html.twig",
            TemplateMoveRenameUtil.pickBestTemplateName(List.of("new/template.html.twig"), "old/template.html.twig")
        );
    }

    /**
     * @see TemplateMoveRenameUtil#pickBestTemplateName
     */
    public void testPickBestTemplateNamePrefersMatchingAtNamespace() {
        assertEquals(
            "@App/new/template.html.twig",
            TemplateMoveRenameUtil.pickBestTemplateName(
                List.of("new/template.html.twig", "@App/new/template.html.twig"),
                "@App/old/template.html.twig"
            )
        );
    }

    /**
     * @see TemplateMoveRenameUtil#pickBestTemplateName
     */
    public void testPickBestTemplateNamePrefersMatchingBundleStyle() {
        assertEquals(
            "FooBundle:dir:new.html.twig",
            TemplateMoveRenameUtil.pickBestTemplateName(
                List.of("dir/new.html.twig", "FooBundle:dir:new.html.twig"),
                "FooBundle:dir:old.html.twig"
            )
        );
    }

    /**
     * @see TemplateMoveRenameUtil#pickBestTemplateName
     */
    public void testPickBestTemplateNameFallsBackToFirstWhenNoPrefixMatch() {
        assertEquals(
            "new/template.html.twig",
            TemplateMoveRenameUtil.pickBestTemplateName(
                List.of("new/template.html.twig", "@Other/template.html.twig"),
                "@App/old/template.html.twig"
            )
        );
    }

    /**
     * @see TemplateMoveRenameUtil#extractNamespacePrefix
     */
    public void testExtractNamespacePrefixAtStyle() {
        assertEquals("@App", TemplateMoveRenameUtil.extractNamespacePrefix("@App/foo/bar.html.twig"));
    }

    /**
     * @see TemplateMoveRenameUtil#extractNamespacePrefix
     */
    public void testExtractNamespacePrefixAtStyleWithoutPath() {
        assertEquals("@App", TemplateMoveRenameUtil.extractNamespacePrefix("@App"));
    }

    /**
     * @see TemplateMoveRenameUtil#extractNamespacePrefix
     */
    public void testExtractNamespacePrefixBundleStyle() {
        assertEquals("FooBundle", TemplateMoveRenameUtil.extractNamespacePrefix("FooBundle:dir:bar.html.twig"));
    }

    /**
     * @see TemplateMoveRenameUtil#extractNamespacePrefix
     */
    public void testExtractNamespacePrefixPlainStyle() {
        assertEquals("", TemplateMoveRenameUtil.extractNamespacePrefix("foo/bar.html.twig"));
    }

    /**
     * @see TemplateMoveRenameUtil#extractNamespacePrefix
     */
    public void testExtractNamespacePrefixPlainFilename() {
        assertEquals("", TemplateMoveRenameUtil.extractNamespacePrefix("bar.html.twig"));
    }

    /**
     * @see TemplateMoveRenameUtil#renameTemplateName
     */
    public void testRenameTemplateNameKeepsPlainPathStyle() {
        assertEquals(
            "foo/baz.html.twig",
            TemplateMoveRenameUtil.renameTemplateName("foo/bar.html.twig", "baz.html.twig")
        );
    }

    /**
     * @see TemplateMoveRenameUtil#renameTemplateName
     */
    public void testRenameTemplateNameKeepsNamespaceStyle() {
        assertEquals(
            "@App/foo/baz.html.twig",
            TemplateMoveRenameUtil.renameTemplateName("@App/foo/bar.html.twig", "baz.html.twig")
        );
    }

    /**
     * @see TemplateMoveRenameUtil#renameTemplateName
     */
    public void testRenameTemplateNameKeepsBundleStyle() {
        assertEquals(
            "FooBundle:dir:baz.html.twig",
            TemplateMoveRenameUtil.renameTemplateName("FooBundle:dir:bar.html.twig", "baz.html.twig")
        );
    }

    /**
     * Moving a Twig template must update the path inside a PHP render() call.
     *
     * @see TemplateReference#bindToElement
     */
    public void testTemplateReferenceBindToElementUpdatesPhpRenderCall() {
        myFixture.addFileToProject("templates/old/page.html.twig", "");
        PsiFile newFile = myFixture.addFileToProject("templates/new/page.html.twig", "");

        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php class C { function i() { $this->render('old/page.html.twig'); } }"
        );

        StringLiteralExpression literal = PsiTreeUtil.findChildOfType(myFixture.getFile(), StringLiteralExpression.class);
        assertNotNull("Expected StringLiteralExpression in PHP file", literal);
        assertEquals("old/page.html.twig", literal.getContents());

        TemplateReference ref = new TemplateReference(literal);

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.bindToElement(newFile);
        });

        // The PHP ElementManipulator may replace the PSI node, making `literal` stale.
        // Check the file text directly — it is always up to date after the write action.
        assertTrue(
            "PHP file must contain the new template path after bindToElement",
            myFixture.getFile().getText().contains("new/page.html.twig")
        );
    }

    /**
     * Renaming a Twig template must update the filename portion inside a PHP render() call while
     * preserving the existing logical path style.
     *
     * @see TemplateReference#handleElementRename
     */
    public void testTemplateReferenceHandleElementRenameUpdatesPhpRenderCall() {
        myFixture.configureByText(
            PhpFileType.INSTANCE,
            "<?php class C { function i() { $this->render('@App/old/page.html.twig'); } }"
        );

        StringLiteralExpression literal = PsiTreeUtil.findChildOfType(myFixture.getFile(), StringLiteralExpression.class);
        assertNotNull("Expected StringLiteralExpression in PHP file", literal);

        TemplateReference ref = new TemplateReference(literal);

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.handleElementRename("renamed.html.twig");
        });

        assertEquals(
            "<?php class C { function i() { $this->render('@App/old/renamed.html.twig'); } }",
            myFixture.getFile().getText()
        );
    }

    /**
     * Renaming the Twig file through IntelliJ's refactoring pipeline must keep the complete PHP
     * string literal intact, including its closing quote.
     */
    public void testRenameTwigTemplateKeepsClosingQuoteInPhpRenderCall() {
        PsiFile template = myFixture.addFileToProject("templates/event_participant/manage.html.twig", "");
        PsiFile phpFile = myFixture.addFileToProject(
            "src/Controller/EventParticipantController.php",
            "<?php class C { function i() { $this->render('event_participant/manage.html.twig'); } }"
        );

        myFixture.renameElement(template, "manages.html.twig");

        assertEquals(
            "<?php class C { function i() { $this->render('event_participant/manages.html.twig'); } }",
            phpFile.getText()
        );
    }

    /**
     * A resolved template variable produces a synthetic reference on the complete render() call.
     * Renaming its target must not replace that call with the new filename.
     */
    public void testRenameTwigTemplateKeepsVariableRenderCallIntact() {
        PsiFile template = myFixture.addFileToProject("templates/event_participant/manage.html.twig", "");
        PsiFile phpFile = myFixture.addFileToProject(
            "src/Controller/VariableController.php",
            "<?php class C { function i() { $template = 'event_participant/manage.html.twig'; $this->render($template); } }"
        );

        TwigTemplateUsageReference reference = findTwigUsageReference(
            "templates/event_participant/manage.html.twig",
            phpFile
        );
        assertNotNull("Expected TwigTemplateUsageReference for resolved template variable", reference);
        assertTrue("Expected the render() call as navigation element", reference.getElement() instanceof FunctionReference);

        myFixture.renameElement(template, "manages.html.twig");

        assertEquals(
            "<?php class C { function i() { $template = 'event_participant/manage.html.twig'; $this->render($template); } }",
            phpFile.getText()
        );
    }

    /**
     * The synthetic usage for an @Template annotation navigates to the method identifier. It is
     * navigation-only and must not rename that method.
     */
    public void testRenameTwigTemplateKeepsAnnotatedMethodNameIntact() {
        PsiFile template = myFixture.addFileToProject("templates/event_participant/manage.html.twig", "");
        PsiFile phpFile = myFixture.addFileToProject(
            "src/Controller/AnnotatedController.php",
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "class C {\n" +
                "    /** @Template(\"event_participant/manage.html.twig\") */\n" +
                "    public function index() {}\n" +
                "}\n"
        );

        TwigTemplateUsageReference reference = findTwigUsageReference(
            "templates/event_participant/manage.html.twig",
            phpFile
        );
        assertNotNull("Expected TwigTemplateUsageReference for @Template", reference);
        assertEquals("index", reference.getElement().getText());

        myFixture.renameElement(template, "manages.html.twig");

        assertEquals(
            "<?php\n" +
                "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
                "class C {\n" +
                "    /** @Template(\"event_participant/manage.html.twig\") */\n" +
                "    public function index() {}\n" +
                "}\n",
            phpFile.getText()
        );
    }

    /**
     * The synthetic usage for a #[Template] attribute also navigates to the method identifier. It
     * must not rename that identifier; the attribute's own TemplateReference updates the literal.
     */
    public void testRenameTwigTemplateKeepsAttributedMethodNameIntact() {
        PsiFile template = myFixture.addFileToProject("templates/event_participant/manage.html.twig", "");
        PsiFile phpFile = myFixture.addFileToProject(
            "src/Controller/AttributedController.php",
            "<?php\n" +
                "use Symfony\\Bridge\\Twig\\Attribute\\Template;\n" +
                "class C {\n" +
                "    #[Template(template: 'event_participant/manage.html.twig')]\n" +
                "    public function index() {}\n" +
                "}\n"
        );

        TwigTemplateUsageReference reference = findTwigUsageReference(
            "templates/event_participant/manage.html.twig",
            phpFile
        );
        assertNotNull("Expected TwigTemplateUsageReference for #[Template]", reference);
        assertEquals("index", reference.getElement().getText());

        myFixture.renameElement(template, "manages.html.twig");

        assertEquals(
            "<?php\n" +
                "use Symfony\\Bridge\\Twig\\Attribute\\Template;\n" +
                "class C {\n" +
                "    #[Template(template: 'event_participant/manages.html.twig')]\n" +
                "    public function index() {}\n" +
                "}\n",
            phpFile.getText()
        );
    }

    public void testRenameTwigTemplateUpdatesMultiplePhpUsages() {
        PsiFile template = myFixture.addFileToProject("templates/test/foobaraaaa.html.twig", "");
        PsiFile phpFile = myFixture.addFileToProject(
            "src/Controller/FoobarController.php",
            "<?php\n" +
                "use Symfony\\Bridge\\Twig\\Attribute\\Template;\n" +
                "class FoobarController {\n" +
                "    #[Template(template: 'test/foobaraaaa.html.twig')]\n" +
                "    public function index() {\n" +
                "        return $this->render('test/foobaraaaa.html.twig');\n" +
                "    }\n" +
                "}\n"
        );

        myFixture.renameElement(template, "foobar.html.twig");

        assertEquals(
            "<?php\n" +
                "use Symfony\\Bridge\\Twig\\Attribute\\Template;\n" +
                "class FoobarController {\n" +
                "    #[Template(template: 'test/foobar.html.twig')]\n" +
                "    public function index() {\n" +
                "        return $this->render('test/foobar.html.twig');\n" +
                "    }\n" +
                "}\n",
            phpFile.getText()
        );
    }

    /**
     * @see TemplateMoveRenameUtil#applyRangeReplacement
     */
    public void testApplyRangeReplacementUpdatesDocumentAtRange() {
        PsiFile file = myFixture.addFileToProject("templates/replace_test.html.twig", "Hello World");

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            TemplateMoveRenameUtil.applyRangeReplacement(file, TextRange.create(6, 11), "Twig");
        });

        assertEquals("Hello Twig", file.getText());
    }

    /**
     * Renaming a Twig template must update the filename portion in Twig include usage while
     * preserving the original namespace/path style.
     *
     * @see TwigTemplateUsageReference#handleElementRename
     */
    public void testTwigTemplateUsageReferenceHandleElementRenameUpdatesIncludeTag() {
        myFixture.addFileToProject("templates/old/partial.html.twig", "");

        PsiFile sourceTwig = myFixture.addFileToProject(
            "templates/page.html.twig",
            "{% include '@App/old/partial.html.twig' %}"
        );

        TwigTemplateUsageReference ref = findTwigUsageReference("templates/old/partial.html.twig", sourceTwig);
        assertNotNull("Expected TwigTemplateUsageReference for include", ref);

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.handleElementRename("renamed.html.twig");
        });

        assertEquals("{% include '@App/old/renamed.html.twig' %}", sourceTwig.getText());
    }

    /**
     * Renaming a Twig template must update the template argument of a Twig block() call while
     * preserving the original namespace/path style.
     *
     * @see TwigTemplateUsageReference#handleElementRename
     */
    public void testTwigTemplateUsageReferenceHandleElementRenameUpdatesBlockFunctionTemplateArg() {
        myFixture.addFileToProject("templates/old/partial.html.twig", "");

        PsiFile sourceTwig = myFixture.addFileToProject(
            "templates/page_block.html.twig",
            "{{ block('title', '@App/old/partial.html.twig') }}"
        );

        TwigTemplateUsageReference ref = findTwigUsageReference("templates/old/partial.html.twig", sourceTwig);
        assertNotNull("Expected TwigTemplateUsageReference for block() template argument", ref);

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.handleElementRename("renamed.html.twig");
        });

        assertEquals("{{ block('title', '@App/old/renamed.html.twig') }}", sourceTwig.getText());
    }

    /**
     * Finds the {@link TwigTemplateUsageReference} in {@code sourceTwig} that points to
     * the Twig file at {@code oldTemplateRelativePath} (relative to the temp VFS root).
     */
    @Nullable
    private TwigTemplateUsageReference findTwigUsageReference(
            @NotNull String oldTemplateRelativePath,
            @NotNull PsiFile sourceTwig) {

        com.intellij.openapi.vfs.VirtualFile vFile = myFixture.findFileInTempDir(oldTemplateRelativePath);
        assertNotNull("Old template VirtualFile must exist at: " + oldTemplateRelativePath, vFile);

        PsiFile oldPsiFile = PsiManager.getInstance(getProject()).findFile(vFile);
        assertTrue("Old template must be a TwigFile", oldPsiFile instanceof TwigFile);

        com.intellij.openapi.vfs.VirtualFile sourceVFile = sourceTwig.getVirtualFile();
        assertNotNull("Source Twig VirtualFile must exist", sourceVFile);

        for (PsiReference ref : ReferencesSearch.search(oldPsiFile, GlobalSearchScope.projectScope(getProject())).findAll()) {
            if (ref instanceof TwigTemplateUsageReference usageRef
                    && sourceVFile.equals(usageRef.getElement().getContainingFile().getVirtualFile())) {
                return usageRef;
            }
        }

        return null;
    }
}
