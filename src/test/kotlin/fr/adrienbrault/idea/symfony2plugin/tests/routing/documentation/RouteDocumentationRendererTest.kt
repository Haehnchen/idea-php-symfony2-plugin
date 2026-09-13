package fr.adrienbrault.idea.symfony2plugin.tests.routing.documentation

import fr.adrienbrault.idea.symfony2plugin.routing.Route
import fr.adrienbrault.idea.symfony2plugin.routing.documentation.renderRouteDocumentation
import junit.framework.TestCase

class RouteDocumentationRendererTest : TestCase() {
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
}
