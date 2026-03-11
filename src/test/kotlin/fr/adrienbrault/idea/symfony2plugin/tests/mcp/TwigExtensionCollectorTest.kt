package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigExtensionCollector

/**
 * @see TwigExtensionCollector
 *
 * Note: TwigExtensionCollector uses a literal "\n" (backslash-n) as the row separator,
 * not a real newline character. Assertions must account for this.
 */
class TwigExtensionCollectorTest : McpCollectorTestCase() {

    fun testCollectAttributeFilterFullLine() {
        val result = TwigExtensionCollector(project).collect("product_number_filter", true, false, false, false)

        assertTrue("Unexpected CSV:\n$result", result.contains(
            "filter,product_number_filter,\\App\\Twig\\AppExtension,formatProductNumberFilter,number"
        ))
    }

    fun testCollectAttributeFunctionFullLine() {
        val result = TwigExtensionCollector(project).collect("product_number_function", false, true, false, false)

        assertTrue("Unexpected CSV:\n$result", result.contains(
            "function,product_number_function,\\App\\Twig\\AppExtension,formatProductNumberFunction,number"
        ))
    }

    fun testCollectAttributeTestFullLine() {
        val result = TwigExtensionCollector(project).collect("product_number_test", false, false, true, false)

        assertTrue("Unexpected CSV:\n$result", result.contains(
            "test,product_number_test,\\App\\Twig\\AppExtension,formatProductNumberTest,number"
        ))
    }

    fun testCollectFilterOnlyExcludesFunctionsAndTests() {
        val result = TwigExtensionCollector(project).collect(null, true, false, false, false)

        assertFalse("Must not contain function rows", result.contains("function,product_number_function"))
        assertFalse("Must not contain test rows", result.contains("test,product_number_test"))
        assertTrue("Must contain filter row for product_number_filter", result.contains("filter,product_number_filter"))
    }

    fun testCollectFunctionOnlyExcludesFiltersAndTests() {
        val result = TwigExtensionCollector(project).collect(null, false, true, false, false)

        assertFalse("Must not contain filter rows", result.contains("filter,product_number_filter"))
        assertFalse("Must not contain test rows", result.contains("test,product_number_test"))
        assertTrue("Must contain function row for product_number_function", result.contains("function,product_number_function"))
    }

    fun testCollectTestOnlyExcludesFiltersAndFunctions() {
        val result = TwigExtensionCollector(project).collect(null, false, false, true, false)

        assertFalse("Must not contain filter rows", result.contains("filter,product_number_filter"))
        assertFalse("Must not contain function rows", result.contains("function,product_number_function"))
        assertTrue("Must contain test row for product_number_test", result.contains("test,product_number_test"))
    }

    fun testCollectSearchNoMatchReturnsHeaderOnly() {
        val result = TwigExtensionCollector(project).collect("zzz_no_such_extension_zzz", true, true, true, true)

        assertEquals(
            "extension_type,name,className,methodName,parameters\\n",
            result
        )
    }

    fun testCollectNullSearchReturnsAllTypes() {
        val result = TwigExtensionCollector(project).collect(null, true, true, true, false)

        assertTrue("Must have CSV header", result.startsWith("extension_type,name,className,methodName,parameters"))
        assertTrue("Must contain product_number_filter row", result.contains("filter,product_number_filter,"))
        assertTrue("Must contain product_number_function row", result.contains("function,product_number_function,"))
        assertTrue("Must contain product_number_test row", result.contains("test,product_number_test,"))
    }
}
