package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigExtensionCollector;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.mcp.collector.TwigExtensionCollector
 *
 * Note: TwigExtensionCollector uses a literal "\n" (backslash-n) as the row separator,
 * not a real newline character. Assertions must account for this.
 */
public class TwigExtensionCollectorTest extends McpCollectorTestCase {

    /**
     * A filter registered via #[AsTwigFilter] must produce a row containing the extension type,
     * filter name, declaring class, method name, and parameter list.
     *
     * Fixture: App\Twig\AppExtension::formatProductNumberFilter(string $number)
     */
    public void testCollectAttributeFilterFullLine() {
        String result = new TwigExtensionCollector(getProject()).collect("product_number_filter", true, false, false, false);

        // Full row content check (row separator is literal \n, not newline)
        assertTrue("Unexpected CSV:\n" + result, result.contains(
            "filter,product_number_filter,\\App\\Twig\\AppExtension,formatProductNumberFilter,number"
        ));
    }

    /**
     * A function registered via #[AsTwigFunction] must produce a row containing the extension type,
     * function name, declaring class, method name, and parameter list.
     *
     * Fixture: App\Twig\AppExtension::formatProductNumberFunction(string $number)
     */
    public void testCollectAttributeFunctionFullLine() {
        String result = new TwigExtensionCollector(getProject()).collect("product_number_function", false, true, false, false);

        assertTrue("Unexpected CSV:\n" + result, result.contains(
            "function,product_number_function,\\App\\Twig\\AppExtension,formatProductNumberFunction,number"
        ));
    }

    /**
     * A test registered via #[AsTwigTest] must produce a row containing the extension type,
     * test name, declaring class, method name, and parameter list.
     *
     * Fixture: App\Twig\AppExtension::formatProductNumberTest(string $number)
     */
    public void testCollectAttributeTestFullLine() {
        String result = new TwigExtensionCollector(getProject()).collect("product_number_test", false, false, true, false);

        assertTrue("Unexpected CSV:\n" + result, result.contains(
            "test,product_number_test,\\App\\Twig\\AppExtension,formatProductNumberTest,number"
        ));
    }

    /**
     * When includeFilters=true and includeFunction/includeTests=false, the result must contain
     * at least one filter row and must not contain function or test rows.
     */
    public void testCollectFilterOnlyExcludesFunctionsAndTests() {
        String result = new TwigExtensionCollector(getProject()).collect(null, true, false, false, false);

        assertFalse("Must not contain function rows", result.contains("function,product_number_function"));
        assertFalse("Must not contain test rows", result.contains("test,product_number_test"));
        assertTrue("Must contain filter row for product_number_filter", result.contains("filter,product_number_filter"));
    }

    /**
     * When includeFunctions=true and includeFilters/includeTests=false, the result must contain
     * at least one function row and must not contain filter or test rows.
     */
    public void testCollectFunctionOnlyExcludesFiltersAndTests() {
        String result = new TwigExtensionCollector(getProject()).collect(null, false, true, false, false);

        assertFalse("Must not contain filter rows", result.contains("filter,product_number_filter"));
        assertFalse("Must not contain test rows", result.contains("test,product_number_test"));
        assertTrue("Must contain function row for product_number_function", result.contains("function,product_number_function"));
    }

    /**
     * When includeTests=true and includeFilters/includeFunctions=false, the result must contain
     * at least one test row and must not contain filter or function rows.
     */
    public void testCollectTestOnlyExcludesFiltersAndFunctions() {
        String result = new TwigExtensionCollector(getProject()).collect(null, false, false, true, false);

        assertFalse("Must not contain filter rows", result.contains("filter,product_number_filter"));
        assertFalse("Must not contain function rows", result.contains("function,product_number_function"));
        assertTrue("Must contain test row for product_number_test", result.contains("test,product_number_test"));
    }

    /**
     * A search string that matches nothing must return only the header row.
     *
     * Row separator is a literal \n (backslash-n), so the expected string ends with \\n.
     */
    public void testCollectSearchNoMatchReturnsHeaderOnly() {
        String result = new TwigExtensionCollector(getProject()).collect("zzz_no_such_extension_zzz", true, true, true, true);

        // The collector appends literal "\n" (backslash-n) as row separator
        assertEquals(
            "extension_type,name,className,methodName,parameters\\n",
            result
        );
    }

    /**
     * A null search with all type flags enabled must return all three attribute-based extensions
     * from App\Twig\AppExtension alongside the header.
     */
    public void testCollectNullSearchReturnsAllTypes() {
        String result = new TwigExtensionCollector(getProject()).collect(null, true, true, true, false);

        assertTrue("Must have CSV header", result.startsWith("extension_type,name,className,methodName,parameters"));
        assertTrue("Must contain product_number_filter row", result.contains("filter,product_number_filter,"));
        assertTrue("Must contain product_number_function row", result.contains("function,product_number_function,"));
        assertTrue("Must contain product_number_test row", result.contains("test,product_number_test,"));
    }
}
