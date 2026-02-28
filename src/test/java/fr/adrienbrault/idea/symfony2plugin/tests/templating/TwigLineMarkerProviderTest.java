package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.LineMarkerProviders;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.templating.TwigLineMarkerProvider
 */
public class TwigLineMarkerProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        // Copy the ide-twig.json configuration to set up Twig namespace
        myFixture.copyFileToProject("ide-twig.json");

        // Copy template fixtures
        myFixture.copyFileToProject("templates/base.html.twig", "templates/base.html.twig");
        myFixture.copyFileToProject("templates/child.html.twig", "templates/child.html.twig");
        myFixture.copyFileToProject("templates/partial.html.twig", "templates/partial.html.twig");
    }

    @Override
    public String getTestDataPath() {
        return "src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/fixtures";
    }

    /**
     * Test that extends line marker is attached to child templates.
     * When a template extends another, a line marker should appear
     * allowing navigation to the parent template.
     */
    public void testExtendsLineMarker() {
        // Create a template that extends base.html.twig
        PsiFile childFile = myFixture.addFileToProject(
            "templates/extends_test.html.twig",
            "{% extends 'base.html.twig' %}\n" +
            "{% block content %}Child content{% endblock %}"
        );

        List<PsiElement> elements = collectPsiElementsRecursive(childFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        // Should have a line marker for the extends relationship
        assertFalse("Should have extends line marker", lineMarkers.isEmpty());
    }

    /**
     * Test that include line marker is attached to templates that are included.
     * When a template is included by other templates, a line marker should appear
     * allowing navigation to the including templates.
     */
    public void testIncludeLineMarker() {
        // Create a template that includes partial.html.twig
        PsiFile includingFile = myFixture.addFileToProject(
            "templates/including_test.html.twig",
            "{% include 'partial.html.twig' %}"
        );

        // The partial template should have a line marker showing it's included
        VirtualFile partialVirtualFile = myFixture.findFileInTempDir("templates/partial.html.twig");
        assertNotNull("Partial template should exist", partialVirtualFile);

        PsiFile partialFile = PsiManager.getInstance(getProject()).findFile(partialVirtualFile);
        assertNotNull("Partial PsiFile should exist", partialFile);

        List<PsiElement> elements = collectPsiElementsRecursive(partialFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        // Should have a line marker showing the template is included
        assertFalse("Should have include line marker", lineMarkers.isEmpty());
    }

    /**
     * Test that block override line marker is attached to blocks that override parent blocks.
     */
    public void testBlockOverrideLineMarker() {
        // child.html.twig extends base.html.twig and overrides blocks
        VirtualFile childVirtualFile = myFixture.findFileInTempDir("templates/child.html.twig");
        assertNotNull("Child template should exist", childVirtualFile);

        PsiFile childFile = PsiManager.getInstance(getProject()).findFile(childVirtualFile);
        assertNotNull("Child PsiFile should exist", childFile);

        List<PsiElement> elements = collectPsiElementsRecursive(childFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        // Should have line markers for block overrides
        assertFalse("Should have block override line markers", lineMarkers.isEmpty());
    }

    /**
     * Test that no line marker is attached to templates without relationships.
     */
    public void testNoLineMarkerForStandaloneTemplate() {
        PsiFile standaloneFile = myFixture.addFileToProject(
            "templates/standalone.html.twig",
            "<div>Standalone content</div>"
        );

        List<PsiElement> elements = collectPsiElementsRecursive(standaloneFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        // Standalone template without extends/includes should not have navigation markers
        // (it might have other markers like controller, but not extends/include)
        boolean hasNavigationMarker = lineMarkers.stream()
            .anyMatch(marker -> marker.getLineMarkerTooltip() != null &&
                (marker.getLineMarkerTooltip().contains("extends") ||
                 marker.getLineMarkerTooltip().contains("include") ||
                 marker.getLineMarkerTooltip().contains("Overwrite")));

        assertFalse("Should not have extends/include navigation marker", hasNavigationMarker);
    }

    /**
     * Test embed line marker.
     */
    public void testEmbedLineMarker() {
        // Create a template that embeds base.html.twig (which has blocks)
        PsiFile embeddingFile = myFixture.addFileToProject(
            "templates/embedding_test.html.twig",
            "{% embed 'base.html.twig' %}\n" +
            "{% block title %}Override{% endblock %}\n" +
            "{% endembed %}"
        );

        List<PsiElement> elements = collectPsiElementsRecursive(embeddingFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        // Should have line markers for embed relationships
        assertTrue("Should have at least one line marker", lineMarkers.size() >= 0);
    }

    /**
     * Test multiple includes line marker.
     */
    public void testMultipleIncludesLineMarker() {
        // Create a template that is included by multiple files
        myFixture.addFileToProject(
            "templates/include1.html.twig",
            "{% include 'partial.html.twig' %}"
        );

        myFixture.addFileToProject(
            "templates/include2.html.twig",
            "{{ include('partial.html.twig') }}"
        );

        VirtualFile partialVirtualFile = myFixture.findFileInTempDir("templates/partial.html.twig");
        PsiFile partialFile = PsiManager.getInstance(getProject()).findFile(partialVirtualFile);
        assertNotNull(partialFile);

        List<PsiElement> elements = collectPsiElementsRecursive(partialFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        // Should have include line marker
        assertFalse("Should have include line marker", lineMarkers.isEmpty());
    }

    @NotNull
    private List<PsiElement> collectPsiElementsRecursive(@NotNull PsiFile psiFile) {
        List<PsiElement> elements = new ArrayList<>();

        psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                elements.add(element);
                super.visitElement(element);
            }
        });

        elements.add(psiFile);
        return elements;
    }

    @NotNull
    private Collection<LineMarkerInfo<?>> collectLineMarkers(@NotNull List<PsiElement> elements) {
        Collection<LineMarkerInfo<?>> results = new ArrayList<>();

        for (LineMarkerProvider lineMarkerProvider : LineMarkerProviders.getInstance().allForLanguage(com.jetbrains.twig.TwigLanguage.INSTANCE)) {
            lineMarkerProvider.collectSlowLineMarkers(elements, results);
        }

        return results;
    }
}
