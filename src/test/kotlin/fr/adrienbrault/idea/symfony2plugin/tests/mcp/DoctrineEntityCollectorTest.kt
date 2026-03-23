package fr.adrienbrault.idea.symfony2plugin.tests.mcp

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityCollector

class DoctrineEntityCollectorTest : McpCollectorTestCase() {

    /**
     * Test that collect returns valid CSV format with header.
     */
    fun testCollectReturnsValidCsvFormat() {
        val result = DoctrineEntityCollector(project).collect()

        assertTrue("CSV should contain header:\n$result", result.contains("className,filePath"))
        assertUsesRealLineBreaks(result)
    }

    /**
     * Test that annotated entities from entity_helper.php are collected.
     * Bar, Bar2, Bar3 have @ORM\Entity annotations.
     */
    fun testCollectReturnsAnnotatedEntities() {
        val result = DoctrineEntityCollector(project).collect()

        assertTrue("Should contain \\FooBundle\\Entity\\Bar (annotated entity):\n$result", result.contains("\\FooBundle\\Entity\\Bar"))
        assertTrue("Should contain \\FooBundle\\Entity\\Bar2 (annotated with ::class):\n$result", result.contains("\\FooBundle\\Entity\\Bar2"))
        assertTrue("Should contain \\FooBundle\\Entity\\Bar3 (annotated with alias):\n$result", result.contains("\\FooBundle\\Entity\\Bar3"))
    }

    /**
     * Test that YAML-mapped entity is collected.
     * FooBundle\Entity\Yaml is mapped via doctrine.orm.yml.
     */
    fun testCollectReturnsYamlMappedEntity() {
        val result = DoctrineEntityCollector(project).collect()

        assertTrue("Should contain \\FooBundle\\Entity\\Yaml (YAML-mapped entity):\n$result", result.contains("\\FooBundle\\Entity\\Yaml"))
    }

    /**
     * Test that file paths are correctly resolved.
     */
    fun testCollectIncludesFilePaths() {
        val result = DoctrineEntityCollector(project).collect()

        assertTrue("Should contain entity_helper.php in file path:\n$result", result.contains("entity_helper.php"))
    }

    /**
     * Test that interfaces are excluded from entity list.
     * BarInterface should not be collected.
     */
    fun testCollectExcludesInterfaces() {
        val result = DoctrineEntityCollector(project).collect()

        assertFalse("Should not contain BarInterface:\n$result", result.contains("BarInterface"))
    }

    /**
     * Test that repository classes are excluded.
     * BarRepository should not be collected as an entity.
     */
    fun testCollectExcludesRepositories() {
        val result = DoctrineEntityCollector(project).collect()

        assertFalse("Should not contain BarRepository as standalone entity:\n$result", result.contains("\\FooBundle\\Entity\\BarRepository,"))
    }

    /**
     * Test that CSV contains class names with backslashes.
     */
    fun testCollectContainsBackslashesInClassNames() {
        val result = DoctrineEntityCollector(project).collect()

        assertTrue("Class names should contain backslashes:\n$result", result.contains("\\FooBundle"))
    }

    /**
     * Test that header is correctly formatted.
     */
    fun testCollectHeaderFormat() {
        val result = DoctrineEntityCollector(project).collect()

        assertTrue("Should have className in header", result.contains("className"))
        assertTrue("Should have filePath in header", result.contains("filePath"))
    }

    /**
     * Test that multiple entities are collected.
     */
    fun testCollectMultipleEntities() {
        val result = DoctrineEntityCollector(project).collect()

        var entityCount = 0
        for (entity in arrayOf("\\FooBundle\\Entity\\Bar", "\\FooBundle\\Entity\\Bar2", "\\FooBundle\\Entity\\Bar3", "\\FooBundle\\Entity\\Yaml")) {
            if (result.contains(entity)) {
                entityCount++
            }
        }

        assertTrue("Should have at least 4 entities, got $entityCount:\n$result", entityCount >= 4)
    }

    /**
     * Test that output is not empty.
     */
    fun testCollectOutputNotEmpty() {
        val result = DoctrineEntityCollector(project).collect()

        assertFalse("Result should not be empty", result.isEmpty())
        assertFalse("Result should not be just whitespace", result.trim().isEmpty())
    }
}
