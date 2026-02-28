package fr.adrienbrault.idea.symfony2plugin.tests.mcp;

import fr.adrienbrault.idea.symfony2plugin.mcp.collector.DoctrineEntityCollector;

public class DoctrineEntityCollectorTest extends McpCollectorTestCase {

    /**
     * Test that collect returns valid CSV format with header.
     */
    public void testCollectReturnsValidCsvFormat() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        // Check header is present
        assertTrue("CSV should contain header:\n" + result,
            result.contains("className,filePath"));
    }

    /**
     * Test that annotated entities from entity_helper.php are collected.
     * Bar, Bar2, Bar3 have @ORM\Entity annotations.
     */
    public void testCollectReturnsAnnotatedEntities() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        // Check for annotation-based entities from entity_helper.php fixture
        // These have @ORM\Entity annotation
        assertTrue("Should contain \\FooBundle\\Entity\\Bar (annotated entity):\n" + result,
            result.contains("\\FooBundle\\Entity\\Bar"));
        assertTrue("Should contain \\FooBundle\\Entity\\Bar2 (annotated with ::class):\n" + result,
            result.contains("\\FooBundle\\Entity\\Bar2"));
        assertTrue("Should contain \\FooBundle\\Entity\\Bar3 (annotated with alias):\n" + result,
            result.contains("\\FooBundle\\Entity\\Bar3"));
    }

    /**
     * Test that YAML-mapped entity is collected.
     * FooBundle\Entity\Yaml is mapped via doctrine.orm.yml.
     */
    public void testCollectReturnsYamlMappedEntity() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        // Check for YAML-mapped entity
        assertTrue("Should contain \\FooBundle\\Entity\\Yaml (YAML-mapped entity):\n" + result,
            result.contains("\\FooBundle\\Entity\\Yaml"));
    }

    /**
     * Test that file paths are correctly resolved.
     */
    public void testCollectIncludesFilePaths() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        // Check that file paths are included
        assertTrue("Should contain entity_helper.php in file path:\n" + result,
            result.contains("entity_helper.php"));
    }

    /**
     * Test that interfaces are excluded from entity list.
     * BarInterface should not be collected.
     */
    public void testCollectExcludesInterfaces() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        // Interfaces should not be in the entity list
        assertFalse("Should not contain BarInterface:\n" + result,
            result.contains("BarInterface"));
    }

    /**
     * Test that repository classes are excluded.
     * BarRepository should not be collected as an entity.
     */
    public void testCollectExcludesRepositories() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        // Repository classes should not be in the entity list
        assertFalse("Should not contain BarRepository as standalone entity:\n" + result,
            result.contains("\\FooBundle\\Entity\\BarRepository,"));
    }

    /**
     * Test that CSV contains class names with backslashes.
     */
    public void testCollectContainsBackslashesInClassNames() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        // Class names should contain backslashes
        assertTrue("Class names should contain backslashes:\n" + result,
            result.contains("\\FooBundle"));
    }

    /**
     * Test that header is correctly formatted.
     */
    public void testCollectHeaderFormat() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        // Header should be present
        assertTrue("Should have className in header", result.contains("className"));
        assertTrue("Should have filePath in header", result.contains("filePath"));
    }

    /**
     * Test that multiple entities are collected.
     */
    public void testCollectMultipleEntities() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        // Count entities - should have at least Bar, Bar2, Bar3, Yaml
        int entityCount = 0;
        for (String entity : new String[]{"\\FooBundle\\Entity\\Bar", "\\FooBundle\\Entity\\Bar2",
                "\\FooBundle\\Entity\\Bar3", "\\FooBundle\\Entity\\Yaml"}) {
            if (result.contains(entity)) {
                entityCount++;
            }
        }

        assertTrue("Should have at least 4 entities, got " + entityCount + ":\n" + result,
            entityCount >= 4);
    }

    /**
     * Test that output is not empty.
     */
    public void testCollectOutputNotEmpty() {
        String result = new DoctrineEntityCollector(getProject()).collect();

        assertFalse("Result should not be empty", result.isEmpty());
        assertFalse("Result should not be just whitespace", result.trim().isEmpty());
    }
}
