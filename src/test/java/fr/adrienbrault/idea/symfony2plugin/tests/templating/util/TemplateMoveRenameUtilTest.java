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

    // -------------------------------------------------------------------------
    // pickBestTemplateName
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // extractNamespacePrefix
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // TemplateReference.bindToElement — PHP render() call update
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // TemplateMoveRenameUtil.applyRangeReplacement
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

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
