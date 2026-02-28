package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityFieldsCollector;

public class DoctrineEntityFieldsCollectorTest extends McpCollectorTestCase {

    /**
     * Test that collect returns valid CSV format with correct header.
     */
    public void testCollectReturnsValidCsvFormat() {
        String result = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");

        // Check header is present
        assertTrue("CSV should contain header:\n" + result,
            result.contains("name,column,type,relation,relationType,enumType,propertyType"));
    }

    /**
     * Test that YAML-mapped entity fields are collected correctly.
     */
    public void testCollectReturnsYamlMappedFields() {
        String result = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");

        // Check for the 'name' field
        assertTrue("Should contain 'name' field:\n" + result,
            result.contains("name,"));
        assertTrue("Should contain 'string' type for name field:\n" + result,
            result.contains("string"));
    }

    /**
     * Test full CSV line format for the name field.
     */
    public void testCollectNameFieldFullLine() {
        String result = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");

        // Verify the name field line structure: name,<empty>,string,<empty>,<empty>,<empty>,<empty>
        assertTrue("Should contain name field with string type:\n" + result,
            result.contains("name,,string,,,"));
    }

    /**
     * Test that leading backslash is normalized in class name.
     */
    public void testCollectNormalizesLeadingBackslash() {
        // Should work with or without leading backslash
        String result1 = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");
        String result2 = new DoctrineEntityFieldsCollector(getProject()).collect("\\FooBundle\\Entity\\Yaml");

        // Both should return the same result
        assertEquals("Results should be identical with or without leading backslash",
            result1, result2);
    }

    /**
     * Test that non-existent entity throws appropriate error.
     */
    public void testCollectNonExistentEntityFails() {
        try {
            new DoctrineEntityFieldsCollector(getProject()).collect("NonExistent\\Entity\\NotFound");
            fail("Should throw exception for non-existent entity");
        } catch (Exception e) {
            assertTrue("Error should mention entity not found: " + e.getMessage(),
                e.getMessage().contains("not found"));
        }
    }

    /**
     * Test that entity class name lookup works correctly.
     */
    public void testCollectClassNameLookup() {
        // Should work with exact case
        String result = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");
        assertNotNull("Should return result for correct case", result);
        assertFalse("Result should not be empty", result.isEmpty());
    }

    /**
     * Test that multiple collect calls work consistently.
     */
    public void testCollectMultipleCallsConsistent() {
        String result1 = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");
        String result2 = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");

        assertEquals("Multiple calls should return identical results", result1, result2);
    }

    /**
     * Test that the collector returns output for the YAML entity.
     */
    public void testCollectReturnsOutput() {
        String result = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");

        assertNotNull("Result should not be null", result);
        assertFalse("Result should not be empty", result.isEmpty());
    }

    /**
     * Test that CSV output is not empty.
     */
    public void testCollectOutputNotEmpty() {
        String result = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");

        assertFalse("Result should not be empty", result.isEmpty());
        assertFalse("Result should not be just whitespace", result.trim().isEmpty());
    }

    /**
     * Test that output contains the expected field from YAML fixture.
     */
    public void testCollectContainsExpectedYamlField() {
        String result = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");

        // The fixture doctrine.orm.yml defines a 'name' field of type 'string'
        assertTrue("Should contain field with 'name':\n" + result, result.contains("name"));
        assertTrue("Should contain 'string' type:\n" + result, result.contains("string"));
    }

    /**
     * Test header format.
     */
    public void testCollectHeaderFormat() {
        String result = new DoctrineEntityFieldsCollector(getProject()).collect("FooBundle\\Entity\\Yaml");

        // Header should contain all column names
        assertTrue("Should have complete header", result.contains("name,column,type,relation,relationType,enumType,propertyType"));
    }
}
