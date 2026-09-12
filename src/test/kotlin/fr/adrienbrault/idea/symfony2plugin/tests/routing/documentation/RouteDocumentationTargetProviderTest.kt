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
        myFixture.addFileToProject("controller.php", """
            <?php
            namespace Symfony\Bundle\FrameworkBundle\Controller;
            class AbstractController {
                public function generateUrl(${'$'}name, array ${'$'}parameters = []) {}
                public function redirectToRoute(${'$'}route, array ${'$'}parameters = []) {}
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
        for (function in listOf("path", "url")) {
            for (argument in listOf(
                "'app_<caret>blog'", "'<caret>app_blog'", "'app_blog<caret>'",
                "<caret>'app_blog'", "<caret>\"app_blog\"", "\"app_blog<caret>\"",
            )) {
                val html = doc("<p>{{ $function($argument, {slug: article.slug}) }}</p>")
                assertNotNull(html)
                assertTrue(html!!.contains("/blog/{slug}/{page}"))
                assertTrue(html.contains("Methods: <code>GET</code>"))
            }
        }
    }

    fun testPhpRoutesAndNamedArguments() {
        val prefix = "<?php class Blog extends \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController { function test() { "
        for (call in listOf(
            "\$this->generateUrl('app_<caret>blog', ['slug' => 1]);",
            "\$this->redirectToRoute('app_<caret>blog');",
            "\$this->generateUrl(parameters: ['slug' => 1], name: 'app_<caret>blog');",
            "\$this->redirectToRoute(parameters: ['slug' => 1], route: 'app_<caret>blog');"
        )) assertNotNull(call, doc(prefix + call + " }}", "test.php"))
    }

    fun testNegativeContexts() {
        for (text in listOf(
            "{{ asset('app_<caret>blog') }}",
            "{{ path('app_blog', {sl<caret>ug: article.slug}) }}",
            "{{ url('app_blog', {'sl<caret>ug': article.slug}) }}",
            "{{ path('missing<caret>') }}",
            "{{ path('app_blog', {slug: 'sl<caret>ug'}) }}",
            "{{ path('app_blog', {slug: {sl<caret>ug: 1}}) }}",
            "{{ path('app_<caret>blog' ~ suffix) }}",
            "{{ path(dynamic, {sl<caret>ug: 1}) }}"
        )) assertNull(text, doc(text))
        assertNull(doc("<?php \$foo = 'app_<caret>blog';", "test.php"))
        assertNull(doc("<?php (new \\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController)->generateUrl('app_blog', ['sl<caret>ug' => 1]);", "test.php"))
        assertNull(doc("<?php class X { function generate(\$name) {} } (new X)->generate('app_<caret>blog');", "test.php"))
    }

    fun testRendererMatchesReferenceLayoutAndOmitsMissingFields() {
        val route = Route(
            "<route>", setOf("code"), mapOf("_format" to "html", "_controller" to "<controller>"),
            mapOf("code" to "[0-9<>]+"), emptyList(), "/_error/{code}.{_format}",
        )
        val html = renderRouteDocumentation(listOf(route))
        assertTrue(html.contains("<code>&lt;route&gt;</code>"))
        assertTrue(html.contains("<br>Path: <code>/_error/{code}.{_format}</code>"))
        assertTrue(html.contains("<br>Defaults: <code>_format</code>"))
        assertTrue(html.contains("<br>Requirements: <code>code: [0-9&lt;&gt;]+</code>"))
        assertTrue(html.contains("<br>Controller: <code>&lt;controller&gt;</code>"))
        assertFalse(html.contains("<code>_controller</code>"))

        val withoutValues = Route("route", setOf("page"), emptyMap(), emptyMap(), emptyList(), "/{page}")
        val withoutValuesHtml = renderRouteDocumentation(listOf(withoutValues))
        assertTrue(withoutValuesHtml.contains("<br>Path: <code>/{page}</code>"))
        assertFalse(withoutValuesHtml.contains("Defaults:"))
        assertFalse(withoutValuesHtml.contains("Requirements:"))
        assertFalse(withoutValuesHtml.contains("Controller:"))
        assertFalse(withoutValuesHtml.contains("Methods:"))

        val controllerOnly = Route("route", emptySet(), mapOf("_controller" to "App\\Controller::index"), emptyMap(), emptyList())
        val controllerOnlyHtml = renderRouteDocumentation(listOf(controllerOnly))
        assertTrue(controllerOnlyHtml.contains("Controller: <code>App\\Controller::index</code>"))
        assertFalse(controllerOnlyHtml.contains("Defaults:"))
    }

    fun testGeneratorInterface() {
        myFixture.addFileToProject("generator.php", """
            <?php
            namespace Symfony\Component\Routing\Generator;
            interface UrlGeneratorInterface {
                public function generate(${'$'}name, array ${'$'}parameters = []);
            }
        """.trimIndent())

        val html = doc("""
            <?php
            function link(\Symfony\Component\Routing\Generator\UrlGeneratorInterface ${'$'}generator) {
                ${'$'}generator->generate(parameters: ['slug' => 'blog'], name: 'app_<caret>blog');
            }
        """.trimIndent(), "test.php")

        assertTrue(html!!.contains("app_blog"))
    }
}
