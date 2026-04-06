package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.SymfonyRouteCollector

class SymfonyRouteCollectorTest : McpCollectorTestCase() {
    fun testCollectReturnsControllerAndPath() {
        val result = SymfonyRouteCollector(project).collect(null, null, null)

        assertTrue("Unexpected CSV:\n$result", result.startsWith("name,controller,path,filePath,lineNumber,templates"))
        assertTrue("Unexpected CSV:\n$result", result.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$result", result.contains("CarController::fooAction"))
        assertTrue("Unexpected CSV:\n$result", result.contains("src/Controller/RouteHelper.php,30,"))
        assertTrue("Unexpected CSV:\n$result", result.contains("/edit/{id}"))
        assertUsesRealLineBreaks(result)
    }

    fun testCollectCanFilterByFullRequestUrl() {
        val result = SymfonyRouteCollector(project).collect(urlPath = "/edit/12")

        assertTrue("Unexpected CSV:\n$result", result.startsWith("name,controller,path,filePath,lineNumber,templates"))
        assertTrue("Unexpected CSV:\n$result", result.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$result", result.contains("src/Controller/RouteHelper.php,30,"))
        assertTrue("Unexpected CSV:\n$result", result.contains("/edit/{id}"))
        assertUsesRealLineBreaks(result)
    }

    fun testCollectCanFilterByPartialUrlPath() {
        val result = SymfonyRouteCollector(project).collect(urlPath = "/edit")

        assertTrue("Unexpected CSV:\n$result", result.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$result", result.contains("/edit/{id}"))
        assertUsesRealLineBreaks(result)
    }

    fun testCollectCanFilterByRouteControllerAndFileGlob() {
        val routeNameResult = SymfonyRouteCollector(project).collect(routeName = "my_car")
        assertTrue("Unexpected CSV:\n$routeNameResult", routeNameResult.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$routeNameResult", routeNameResult.contains("my_car_foo_stuff_2"))
        assertFalse("Unexpected CSV:\n$routeNameResult", routeNameResult.contains("my_foo,"))
        assertUsesRealLineBreaks(routeNameResult)

        val classLikeRouteNameResult = SymfonyRouteCollector(project).collect(routeName = "App\\Controller\\ClassLikeRoute")
        assertTrue("Unexpected CSV:\n$classLikeRouteNameResult", classLikeRouteNameResult.contains("App\\Controller\\ClassLikeRoute"))
        assertTrue("Unexpected CSV:\n$classLikeRouteNameResult", classLikeRouteNameResult.contains("CarAttributeController::classLikeRouteAction"))
        assertUsesRealLineBreaks(classLikeRouteNameResult)

        val controllerClassResult = SymfonyRouteCollector(project).collect(controller = "CarController")
        assertTrue("Unexpected CSV:\n$controllerClassResult", controllerClassResult.contains("CarController::fooAction"))
        assertTrue("Unexpected CSV:\n$controllerClassResult", controllerClassResult.contains("my_car_foo_stuff"))
        assertFalse("Unexpected CSV:\n$controllerClassResult", controllerClassResult.contains("AppleController::fooAction"))
        assertUsesRealLineBreaks(controllerClassResult)

        val controllerMethodResult = SymfonyRouteCollector(project).collect(controller = "CarController::fooAction")
        assertTrue("Unexpected CSV:\n$controllerMethodResult", controllerMethodResult.contains("CarController::fooAction"))
        assertTrue("Unexpected CSV:\n$controllerMethodResult", controllerMethodResult.contains("my_car_foo_stuff"))
        assertFalse("Unexpected CSV:\n$controllerMethodResult", controllerMethodResult.contains("CarController::foo2Action"))
        assertFalse("Unexpected CSV:\n$controllerMethodResult", controllerMethodResult.contains("AppleController::fooAction"))
        assertUsesRealLineBreaks(controllerMethodResult)

        val fileGlobResult = SymfonyRouteCollector(project).collect(fileGlob = "**/RouteHelper.php")
        assertTrue("Unexpected CSV:\n$fileGlobResult", fileGlobResult.startsWith("name,controller,path,filePath,lineNumber,templates"))
        assertTrue("Unexpected CSV:\n$fileGlobResult", fileGlobResult.contains("my_car_foo_stuff"))
        assertTrue("Unexpected CSV:\n$fileGlobResult", fileGlobResult.contains("src/Controller/RouteHelper.php"))
        assertUsesRealLineBreaks(fileGlobResult)
    }
}
