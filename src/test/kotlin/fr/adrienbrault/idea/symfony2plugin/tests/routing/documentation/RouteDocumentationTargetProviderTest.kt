package fr.adrienbrault.idea.symfony2plugin.tests.routing.documentation

import com.intellij.model.Pointer
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import fr.adrienbrault.idea.symfony2plugin.routing.documentation.*
import fr.adrienbrault.idea.symfony2plugin.routing.Route
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class RouteDocumentationTargetProviderTest : SymfonyLightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        myFixture.addFileToProject("config/routes.yaml", """
            app_blog:
              path: /blog/{slug}/{page}
              methods: [GET]
              defaults: { page: 0 }
              requirements: { slug: '[a-z<>]+' }
        """.trimIndent())

        myFixture.addFileToProject("controller.php", $$"""
            <?php
            namespace Symfony\Bundle\FrameworkBundle\Controller;
            class AbstractController {
                public function generateUrl($name, array $parameters = []) {}
                public function redirectToRoute($route, array $parameters = []) {}
            }
        """.trimIndent())
    }

    private fun doc(text: String, file: String = "test.html.twig"): String? {
        val psi = myFixture.configureByText(file, text)
        val targets = RouteDocumentationTargetProvider().documentationTargets(psi, myFixture.caretOffset)
        val target = targets.singleOrNull() ?: return null

        return computeDocumentationBlocking(Pointer.hardPointer(target))?.html
    }

    fun testTwigRouteNames() {
        assertTrue(doc("{{ path('app_<caret>blog', {slug: article.slug}) }}")!!.contains("/blog/{slug}/{page}"))
        assertTrue(doc("{{ url(<caret>\"app_blog\") }}")!!.contains("/blog/{slug}/{page}"))
        assertTrue(doc("{{ path('app_blog<caret>') }}")!!.contains("/blog/{slug}/{page}"))
    }

    fun testPhpRoutesAndNamedArguments() {
        val prefix = "<?php class Blog extends \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController { function test() { "
        assertNotNull(doc($$"$$prefix$this->generateUrl('app_<caret>blog', ['slug' => 1]); }}", "test.php"))
        assertNotNull(doc($$"$$prefix$this->redirectToRoute('app_<caret>blog'); }}", "test.php"))
        assertNotNull(doc($$"$$prefix$this->generateUrl(parameters: ['slug' => 1], name: 'app_<caret>blog'); }}", "test.php"))
        assertNotNull(doc($$"$$prefix$this->redirectToRoute(parameters: ['slug' => 1], route: 'app_<caret>blog'); }}", "test.php"))
    }

    fun testPhpRouteAttributeName() {
        myFixture.addFileToProject("templates/foobar.html.twig", "{{ path('app_foobar_index') }}")
        val html = doc("""
            <?php
            namespace App\Controller;
            use Symfony\Component\Routing\Attribute\Route;
            class FoobarController {
                #[Route("/test/{test}/test", name: "app_foobar_<caret>index")]
                public function index() {}
            }
        """.trimIndent(), "FoobarController.php")!!

        assertTrue(html.contains("Path: <code>/test/{test}/test</code>"))
        assertTrue(html.contains("Twig usages: 1"))
        assertTrue(html.contains("Find usages</a>"))
    }

    fun testNegativeContexts() {
        assertNull(doc("{{ asset('app_<caret>blog') }}"))
        assertNull(doc("{{ path('missing<caret>') }}"))
        assertNull(doc("{{ path('app_<caret>blog' ~ suffix) }}"))

        assertNull(doc($$"<?php $foo = 'app_<caret>blog';", "test.php"))
        assertNull(doc($$"<?php class X { function generate($name) {} } (new X)->generate('app_<caret>blog');", "test.php"))
    }

    fun testRendererMatchesReferenceLayoutAndOmitsMissingFields() {
        val route = Route(
            "<route>", setOf("code"), mapOf("_format" to "html", "_controller" to "<controller>"),
            mapOf("code" to "[0-9<>]+"), emptyList(), "/_error/{code}.{_format}",
        )

        val html = renderRouteDocumentation(listOf(route))
        assertTrue(html.contains("Path: <code>/_error/{code}.{_format}</code>"))
        assertTrue(html.contains("<br>Defaults: <code>_format</code>"))
        assertTrue(html.contains("<br>Requirements: <code>code: [0-9&lt;&gt;]+</code>"))
        assertTrue(html.contains("<br>Controller: <code>&lt;controller&gt;</code>"))
        assertFalse(html.contains("<code>_controller</code>"))

        val withoutValues = Route("route", setOf("page"), emptyMap(), emptyMap(), emptyList(), "/{page}")
        val withoutValuesHtml = renderRouteDocumentation(listOf(withoutValues))

        assertTrue(withoutValuesHtml.contains("Path: <code>/{page}</code>"))
        assertFalse(withoutValuesHtml.contains("Defaults:"))
        assertFalse(withoutValuesHtml.contains("Requirements:"))
        assertFalse(withoutValuesHtml.contains("Controller:"))
        assertFalse(withoutValuesHtml.contains("Methods:"))
        assertTrue(withoutValuesHtml.contains("Find usages</a>"))

        val controllerOnly = Route("route", emptySet(), mapOf("_controller" to "App\\Controller::index"), emptyMap(), emptyList())
        val controllerOnlyHtml = renderRouteDocumentation(listOf(controllerOnly))

        assertTrue(controllerOnlyHtml.contains("Controller: <code>App\\Controller::index</code>"))
        assertFalse(controllerOnlyHtml.contains("Defaults:"))
    }

    fun testGeneratorInterface() {
        myFixture.addFileToProject("generator.php", $$"""
            <?php
            namespace Symfony\Component\Routing\Generator;
            interface UrlGeneratorInterface {
                public function generate($name, array $parameters = []);
            }
        """.trimIndent())

        val html = doc($$"""
            <?php
            function link(\Symfony\Component\Routing\Generator\UrlGeneratorInterface $generator) {
                $generator->generate(parameters: ['slug' => 'blog'], name: 'app_<caret>blog');
            }
        """.trimIndent(), "test.php")

        assertTrue(html!!.contains("Find usages</a>"))
    }
}
