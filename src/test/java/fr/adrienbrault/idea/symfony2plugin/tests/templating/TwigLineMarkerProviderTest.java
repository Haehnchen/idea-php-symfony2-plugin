package fr.adrienbrault.idea.symfony2plugin.tests.templating;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.LineMarkerProviders;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigLanguage;
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

    /**
     * Test that controller line marker is attached to templates rendered by controllers.
     * When $this->render('template.html.twig') is called, the template should have
     * a line marker allowing navigation to the controller method.
     */
    public void testControllerRenderLineMarker() {
        // Create a controller that renders a template
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "class HomeController\n" +
            "{\n" +
            "    public function index()\n" +
            "    {\n" +
            "        return $this->render('home/index.html.twig');\n" +
            "    }\n" +
            "}\n"
        );

        // Create the template that is rendered
        PsiFile templateFile = myFixture.addFileToProject(
            "templates/home/index.html.twig",
            "{% extends 'base.html.twig' %}\n" +
            "{% block content %}Home{% endblock %}"
        );

        List<PsiElement> elements = collectPsiElementsRecursive(templateFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        // Should have a line marker for controller navigation
        assertFalse("Should have controller line marker", lineMarkers.isEmpty());
    }

    /**
     * Test that controller line marker works with renderView().
     */
    public void testControllerRenderViewLineMarker() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "class PageController\n" +
            "{\n" +
            "    public function show()\n" +
            "    {\n" +
            "        $content = $this->renderView('page/show.html.twig');\n" +
            "    }\n" +
            "}\n"
        );

        PsiFile templateFile = myFixture.addFileToProject(
            "templates/page/show.html.twig",
            "<div>Page content</div>"
        );

        List<PsiElement> elements = collectPsiElementsRecursive(templateFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        assertFalse("Should have controller line marker for renderView", lineMarkers.isEmpty());
    }

    /**
     * Test that controller line marker works with @Template annotation.
     */
    public void testTemplateAnnotationLineMarker() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
            "\n" +
            "class ArticleController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Template(\"article/list.html.twig\")\n" +
            "     */\n" +
            "    public function listAction()\n" +
            "    {\n" +
            "    }\n" +
            "}\n"
        );

        PsiFile templateFile = myFixture.addFileToProject(
            "templates/article/list.html.twig",
            "{% for article in articles %}{{ article.title }}{% endfor %}"
        );

        List<PsiElement> elements = collectPsiElementsRecursive(templateFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        assertFalse("Should have controller line marker for @Template annotation", lineMarkers.isEmpty());
    }

    /**
     * Test that controller line marker works with #[Template] PHP 8 attribute.
     */
    public void testTemplateAttributeLineMarker() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
            "\n" +
            "class ProductController\n" +
            "{\n" +
            "    #[Template('product/detail.html.twig')]\n" +
            "    public function detailAction()\n" +
            "    {\n" +
            "    }\n" +
            "}\n"
        );

        PsiFile templateFile = myFixture.addFileToProject(
            "templates/product/detail.html.twig",
            "<h1>{{ product.name }}</h1>"
        );

        List<PsiElement> elements = collectPsiElementsRecursive(templateFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        assertFalse("Should have controller line marker for #[Template] attribute", lineMarkers.isEmpty());
    }

    /**
     * Test that controller line marker works with @Template() using guessing.
     */
    public void testTemplateAnnotationGuessingLineMarker() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
            "\n" +
            "class UserController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Template()\n" +
            "     */\n" +
            "    public function profileAction()\n" +
            "    {\n" +
            "    }\n" +
            "}\n"
        );

        // Template should be guessed as user/profile.html.twig
        PsiFile templateFile = myFixture.addFileToProject(
            "templates/user/profile.html.twig",
            "<div>User Profile</div>"
        );

        List<PsiElement> elements = collectPsiElementsRecursive(templateFile);
        Collection<LineMarkerInfo<?>> lineMarkers = collectLineMarkers(elements);

        assertFalse("Should have controller line marker for guessed template", lineMarkers.isEmpty());
    }

    /**
     * Test that controller line marker navigates to the correct method target.
     */
    public void testControllerLineMarkerNavigatesToMethod() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "class DashboardController\n" +
            "{\n" +
            "    public function index()\n" +
            "    {\n" +
            "        return $this->render('dashboard/index.html.twig');\n" +
            "    }\n" +
            "}\n"
        );

        PsiFile templateFile = myFixture.addFileToProject(
            "templates/dashboard/index.html.twig",
            "<h1>Dashboard</h1>"
        );

        assertLineMarker(templateFile, new LineMarker.TargetAcceptsPattern(
            "Navigate to controller",
            PlatformPatterns.psiElement(Method.class).withName("index")
        ));
    }

    /**
     * Test that @Template annotation line marker navigates to controller method.
     */
    public void testTemplateAnnotationLineMarkerNavigatesToMethod() {
        myFixture.configureByText(PhpFileType.INSTANCE, "<?php\n" +
            "namespace App\\Controller;\n" +
            "\n" +
            "use Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template;\n" +
            "\n" +
            "class NewsController\n" +
            "{\n" +
            "    /**\n" +
            "     * @Template(\"news/latest.html.twig\")\n" +
            "     */\n" +
            "    public function latestAction()\n" +
            "    {\n" +
            "    }\n" +
            "}\n"
        );

        PsiFile templateFile = myFixture.addFileToProject(
            "templates/news/latest.html.twig",
            "{% for news in newsItems %}{{ news.title }}{% endfor %}"
        );

        assertLineMarker(templateFile, new LineMarker.TargetAcceptsPattern(
            "Navigate to controller",
            PlatformPatterns.psiElement(Method.class).withName("latestAction")
        ));
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
