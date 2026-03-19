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
    // TwigTemplateUsageReference.bindToElement — Twig tag usages are a no-op
    // (path updates are handled by TwigMoveFileHandler.retargetUsages())
    // -------------------------------------------------------------------------

    /**
     * {@code bindToElement} is a no-op for Twig tag usages (include/extends/embed/…).
     * Path updates during file move are handled exclusively by
     * {@link fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigMoveFileHandler#retargetUsages}.
     *
     * @see TwigTemplateUsageReference#bindToElement
     */
    public void testTwigTemplateUsageReferenceBindToElementIsNoOpForIncludeTag() {
        myFixture.addFileToProject("templates/old/partial.html.twig", "");
        PsiFile newFile = myFixture.addFileToProject("templates/new/partial.html.twig", "");

        PsiFile sourceTwig = myFixture.addFileToProject(
            "templates/page.html.twig",
            "{% include 'old/partial.html.twig' %}"
        );

        TwigTemplateUsageReference ref = findTwigUsageReference("templates/old/partial.html.twig", sourceTwig);
        assertNotNull("Expected TwigTemplateUsageReference for include", ref);

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.bindToElement(newFile);
        });

        // bindToElement is a no-op for Twig tag usages; text must remain unchanged.
        assertEquals("{% include 'old/partial.html.twig' %}", sourceTwig.getText());
    }

    /**
     * {@code bindToElement} is a no-op for Twig tag usages (include/extends/embed/…).
     * Path updates during file move are handled exclusively by
     * {@link fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigMoveFileHandler#retargetUsages}.
     *
     * @see TwigTemplateUsageReference#bindToElement
     */
    public void testTwigTemplateUsageReferenceBindToElementIsNoOpForExtendsTag() {
        myFixture.addFileToProject("templates/base.html.twig", "");
        PsiFile newFile = myFixture.addFileToProject("templates/layout/base.html.twig", "");

        PsiFile childTwig = myFixture.addFileToProject(
            "templates/child.html.twig",
            "{% extends 'base.html.twig' %}"
        );

        TwigTemplateUsageReference ref = findTwigUsageReference("templates/base.html.twig", childTwig);
        assertNotNull("Expected TwigTemplateUsageReference for extends", ref);

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.bindToElement(newFile);
        });

        // bindToElement is a no-op for Twig tag usages; text must remain unchanged.
        assertEquals("{% extends 'base.html.twig' %}", childTwig.getText());
    }

    /**
     * {@code bindToElement} is a no-op for Twig tag usages (include/extends/embed/…).
     * Path updates during file move are handled exclusively by
     * {@link fr.adrienbrault.idea.symfony2plugin.templating.usages.TwigMoveFileHandler#retargetUsages}.
     *
     * @see TwigTemplateUsageReference#bindToElement
     */
    public void testTwigTemplateUsageReferenceBindToElementIsNoOpForEmbedTag() {
        myFixture.addFileToProject("templates/widgets/card.html.twig", "");
        PsiFile newFile = myFixture.addFileToProject("templates/shared/card.html.twig", "");

        PsiFile sourceTwig = myFixture.addFileToProject(
            "templates/home.html.twig",
            "{% embed 'widgets/card.html.twig' %}{% endembed %}"
        );

        TwigTemplateUsageReference ref = findTwigUsageReference("templates/widgets/card.html.twig", sourceTwig);
        assertNotNull("Expected TwigTemplateUsageReference for embed", ref);

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.bindToElement(newFile);
        });

        // bindToElement is a no-op for Twig tag usages; text must remain unchanged.
        assertEquals("{% embed 'widgets/card.html.twig' %}{% endembed %}", sourceTwig.getText());
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
    // TwigTemplateUsageReference.bindToElement — anonymous UX component updates
    // -------------------------------------------------------------------------

    /**
     * Moving an anonymous UX component template must update {@code {{ component('Alert') }}} usages.
     *
     * @see TwigTemplateUsageReference#bindToElement
     */
    public void testTwigTemplateUsageReferenceBindToElementUpdatesAnonymousComponentFunctionUsage() {
        myFixture.addFileToProject("templates/components/Alert.html.twig", "");
        PsiFile newFile = myFixture.addFileToProject("templates/components/nav/Alert.html.twig", "");

        PsiFile usageFile = myFixture.addFileToProject(
            "templates/page.html.twig",
            "{{ component('Alert') }}"
        );

        TwigTemplateUsageReference ref = findTwigUsageReference("templates/components/Alert.html.twig", usageFile);
        assertNotNull("Expected TwigTemplateUsageReference for component() function", ref);
        assertTrue("Expected isComponentUsage reference", ref.isComponentUsage());

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.bindToElement(newFile);
        });

        assertTrue(
            "Expected component name to be updated to nav:Alert",
            usageFile.getText().contains("nav:Alert")
        );
    }

    /**
     * Moving an anonymous UX component template must update {@code {% component Alert %}} tag usages.
     *
     * @see TwigTemplateUsageReference#bindToElement
     */
    public void testTwigTemplateUsageReferenceBindToElementUpdatesAnonymousComponentTagUsage() {
        myFixture.addFileToProject("templates/components/Card.html.twig", "");
        PsiFile newFile = myFixture.addFileToProject("templates/components/shared/Card.html.twig", "");

        PsiFile usageFile = myFixture.addFileToProject(
            "templates/home.html.twig",
            "{% component Card %}{% endcomponent %}"
        );

        TwigTemplateUsageReference ref = findTwigUsageReference("templates/components/Card.html.twig", usageFile);
        assertNotNull("Expected TwigTemplateUsageReference for {% component %} tag", ref);

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.bindToElement(newFile);
        });

        assertTrue(
            "Expected component name to be updated to shared:Card",
            usageFile.getText().contains("shared:Card")
        );
    }

    /**
     * Moving a PHP-backed UX component template must NOT update component name usages
     * because the name comes from the {@code #[AsTwigComponent]} class, not the file path.
     *
     * @see TwigTemplateUsageReference#bindToElement
     */
    public void testTwigTemplateUsageReferenceBindToElementSkipsPhpBackedComponent() {
        // Register a PHP-backed component named "Alert" so getTwigComponentPhpClasses returns non-empty.
        myFixture.addFileToProject("src/Components/Alert.php",
            "<?php\n" +
            "namespace App\\Twig\\Components;\n" +
            "use Symfony\\UX\\TwigComponent\\Attribute\\AsTwigComponent;\n" +
            "#[AsTwigComponent]\n" +
            "class Alert {}\n"
        );

        myFixture.addFileToProject("templates/components/Alert.html.twig", "");
        PsiFile newFile = myFixture.addFileToProject("templates/components/nav/Alert.html.twig", "");

        PsiFile usageFile = myFixture.addFileToProject(
            "templates/other.html.twig",
            "{{ component('Alert') }}"
        );

        TwigTemplateUsageReference ref = findTwigUsageReference("templates/components/Alert.html.twig", usageFile);
        assertNotNull("Expected TwigTemplateUsageReference for PHP-backed component", ref);

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            ref.bindToElement(newFile);
        });

        // PHP-backed: name derived from class, not path — must stay unchanged.
        assertEquals("{{ component('Alert') }}", usageFile.getText());
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

        for (PsiReference ref : ReferencesSearch.search(oldPsiFile, GlobalSearchScope.projectScope(getProject())).findAll()) {
            if (ref instanceof TwigTemplateUsageReference usageRef
                    && sourceTwig.equals(usageRef.getElement().getContainingFile())) {
                return usageRef;
            }
        }

        return null;
    }
}
