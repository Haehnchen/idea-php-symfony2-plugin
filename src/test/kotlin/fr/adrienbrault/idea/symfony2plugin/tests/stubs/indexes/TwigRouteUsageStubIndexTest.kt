package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes

import com.jetbrains.twig.TwigFileType
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigRouteUsageStubIndex
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase

class TwigRouteUsageStubIndexTest : SymfonyLightCodeInsightFixtureTestCase() {
    fun testRouteUsageIndexer() {
        myFixture.configureByText(
            TwigFileType.INSTANCE,
            """
            {{ path('route_path') }}
            {{ url("route_url") }}
            {% if app.request.attributes.get('_route') == 'route_compare' %}{% endif %}
            {% if app.request.attributes.get('_route') is same as('route_same_as') %}{% endif %}
            {% if app.request.attributes.get('_route') in ['route_in_array', 'route_in_array_2'] %}{% endif %}
            {% if app.request.attributes.get('_route') starts with 'route_prefix' %}{% endif %}
            {{ path('foo/' ~ suffix) }}
            """.trimIndent()
        )

        assertIndexContains(TwigRouteUsageStubIndex.KEY, "route_path", "route_url", "route_compare", "route_same_as", "route_in_array", "route_in_array_2")
        assertIndexContainsKeyWithValue(TwigRouteUsageStubIndex.KEY, "route_path") { it.contains("path") }
        assertIndexContainsKeyWithValue(TwigRouteUsageStubIndex.KEY, "route_url") { it.contains("url") }
        assertIndexContainsKeyWithValue(TwigRouteUsageStubIndex.KEY, "route_compare") { it.contains("compare") }
        assertIndexContainsKeyWithValue(TwigRouteUsageStubIndex.KEY, "route_same_as") { it.contains("same_as") }
        assertIndexContainsKeyWithValue(TwigRouteUsageStubIndex.KEY, "route_in_array") { it.contains("in_array") }

        assertIndexNotContains(TwigRouteUsageStubIndex.KEY, "route_prefix", "foo/")
    }
}
