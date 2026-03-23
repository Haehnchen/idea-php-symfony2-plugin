package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityFieldsCollector

class DoctrineEntityFieldsCollectorTest : McpCollectorTestCase() {

    /**
     * Test that collect returns valid CSV format with correct header.
     */
    fun testCollectReturnsValidCsvFormat() {
        val result = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")

        assertTrue("CSV should contain header:\n$result", result.contains("name,column,type,relation,relationType,enumType,propertyType"))
        assertUsesRealLineBreaks(result)
    }

    /**
     * Test that YAML-mapped entity fields are collected correctly.
     */
    fun testCollectReturnsYamlMappedFields() {
        val result = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")

        assertTrue("Should contain 'name' field:\n$result", result.contains("name,"))
        assertTrue("Should contain 'string' type for name field:\n$result", result.contains("string"))
    }

    /**
     * Test full CSV line format for the name field.
     */
    fun testCollectNameFieldFullLine() {
        val result = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")

        assertTrue("Should contain name field with string type:\n$result", result.contains("name,,string,,,"))
    }

    /**
     * Test that leading backslash is normalized in class name.
     */
    fun testCollectNormalizesLeadingBackslash() {
        val result1 = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")
        val result2 = DoctrineEntityFieldsCollector(project).collect("\\FooBundle\\Entity\\Yaml")

        assertEquals("Results should be identical with or without leading backslash", result1, result2)
    }

    /**
     * Test that non-existent entity throws appropriate error.
     */
    fun testCollectNonExistentEntityFails() {
        try {
            DoctrineEntityFieldsCollector(project).collect("NonExistent\\Entity\\NotFound")
            fail("Should throw exception for non-existent entity")
        } catch (e: Exception) {
            assertTrue("Error should mention entity not found: ${e.message}", e.message!!.contains("not found"))
        }
    }

    /**
     * Test that entity class name lookup works correctly.
     */
    fun testCollectClassNameLookup() {
        val result = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")
        assertNotNull("Should return result for correct case", result)
        assertFalse("Result should not be empty", result.isEmpty())
    }

    /**
     * Test that multiple collect calls work consistently.
     */
    fun testCollectMultipleCallsConsistent() {
        val result1 = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")
        val result2 = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")

        assertEquals("Multiple calls should return identical results", result1, result2)
    }

    /**
     * Test that the collector returns output for the YAML entity.
     */
    fun testCollectReturnsOutput() {
        val result = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")

        assertNotNull("Result should not be null", result)
        assertFalse("Result should not be empty", result.isEmpty())
    }

    /**
     * Test that CSV output is not empty.
     */
    fun testCollectOutputNotEmpty() {
        val result = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")

        assertFalse("Result should not be empty", result.isEmpty())
        assertFalse("Result should not be just whitespace", result.trim().isEmpty())
    }

    /**
     * Test that output contains the expected field from YAML fixture.
     */
    fun testCollectContainsExpectedYamlField() {
        val result = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")

        assertTrue("Should contain field with 'name':\n$result", result.contains("name"))
        assertTrue("Should contain 'string' type:\n$result", result.contains("string"))
    }

    /**
     * Test header format.
     */
    fun testCollectHeaderFormat() {
        val result = DoctrineEntityFieldsCollector(project).collect("FooBundle\\Entity\\Yaml")

        assertTrue("Should have complete header", result.contains("name,column,type,relation,relationType,enumType,propertyType"))
    }
}
