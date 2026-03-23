package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyProfilerRequestsCollector
import fr.adrienbrault.idea.symfony2plugin.profiler.collector.DefaultDataCollectorInterface
import fr.adrienbrault.idea.symfony2plugin.profiler.dict.HttpProfilerRequest

class SymfonyProfilerRequestsCollectorTest : McpCollectorTestCase() {
    fun testCollectFailsWithoutProfilerIndexOnLightVfs() {
        try {
            SymfonyProfilerRequestsCollector(project).collect(null, null, null, null)
            fail("Expected profiler collector to fail when no file-system profiler index is available")
        } catch (e: Throwable) {
            assertNotNull(e.message)
            assertTrue(e.message!!.contains("No profiler index available"))
        }
    }

    fun testFormatRequestsUsesRealLineBreaks() {
        val collector = SymfonyProfilerRequestsCollector(project)
        val requests = listOf(
            HttpProfilerRequest(
                200,
                "837681",
                "_profiler/837681",
                "GET",
                "http://127.0.0.1:8000/products/42",
                object : DefaultDataCollectorInterface {
                    override fun getController() = "App\\Controller\\ProductController::show"
                    override fun getRoute() = "product_show"
                    override fun getTemplate() = "product/show.html.twig"
                    override fun getFormTypes() = listOf("App\\Form\\ProductFilterType")
                }
            ),
            HttpProfilerRequest(
                200,
                "802072",
                "_profiler/802072",
                "GET",
                "http://127.0.0.1:8000/orders/recent"
            )
        )

        val csv = collector.formatRequests(requests)

        assertTrue(csv.contains("\n837681,GET,"))
        assertTrue(csv.contains("\n802072,GET,"))
        assertFalse(csv.contains("\\n837681,GET,"))
        assertFalse(csv.contains("\\n802072,GET,"))
    }
}
