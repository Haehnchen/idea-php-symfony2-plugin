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
                    override fun getRenderedTemplates() = listOf("product/show.html.twig", "base.html.twig")
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

    fun testFormatRequestsProvidesControllerIndexedTemplatesColumn() {
        myFixture.addFileToProject(
            "src/Controller/ProductController.php",
            "<?php\nnamespace App\\Controller;\n" +
                "class ProductController {\n" +
                "    public function show() {\n" +
                "        \$this->render('product/show.html.twig');\n" +
                "        \$this->renderView('product/_card.html.twig');\n" +
                "    }\n" +
                "}\n"
        )

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
                    override fun getTemplate() = "profiler/fragment.html.twig"
                    override fun getRenderedTemplates() = listOf(
                        "profiler/fragment.html.twig",
                        "layout/base.html.twig",
                        "widgets/card.html.twig",
                        "widgets/list.html.twig",
                        "widgets/footer.html.twig",
                        "widgets/ignored.html.twig"
                    )
                    override fun getFormTypes() = emptyList<String>()
                }
            )
        )

        val csv = collector.formatRequests(requests)

        assertTrue(csv.startsWith("hash,method,url,statusCode,profilerUrl,controller,route,entryView,renderTemplate,renderedTemplates,formTypes\n"))
        assertTrue(csv.contains(",profiler/fragment.html.twig,product/_card.html.twig;product/show.html.twig,profiler/fragment.html.twig;layout/base.html.twig;widgets/card.html.twig,"))
    }

    fun testFormatRequestsRespectsCustomLimit() {
        val collector = SymfonyProfilerRequestsCollector(project)
        val requests = (1..35).map { index ->
            HttpProfilerRequest(
                200,
                "token$index",
                "_profiler/token$index",
                "GET",
                "http://127.0.0.1:8000/test/$index"
            )
        }

        val csv = collector.formatRequests(requests, limit = 25)

        assertFalse(csv.contains("\ntoken26,GET,"))
        assertTrue(csv.contains("\ntoken25,GET,"))
    }
}
