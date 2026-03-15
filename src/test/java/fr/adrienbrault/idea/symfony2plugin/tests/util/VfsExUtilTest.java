package fr.adrienbrault.idea.symfony2plugin.tests.util;

import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see VfsExUtil
 */
public class VfsExUtilTest extends Assert {
    /**
     * @see VfsExUtil#isAbsolutePath
     */
    @Test
    public void testIsAbsolutePath() {
        // Unix absolute
        assertTrue(VfsExUtil.isAbsolutePath("/mnt/ssd/symfony8/templates"));
        assertTrue(VfsExUtil.isAbsolutePath("/var/cache/dev"));
        assertTrue(VfsExUtil.isAbsolutePath("/"));

        // Windows drive-letter with forward slash
        assertTrue(VfsExUtil.isAbsolutePath("C:/foo/bar"));
        assertTrue(VfsExUtil.isAbsolutePath("Z:/mnt/ssd/symfony8"));
        assertTrue(VfsExUtil.isAbsolutePath("c:/lowercase"));

        // Windows drive-letter with backslash
        assertTrue(VfsExUtil.isAbsolutePath("C:\\foo\\bar"));
        assertTrue(VfsExUtil.isAbsolutePath("Z:\\mnt\\ssd\\symfony8"));

        // relative paths
        assertFalse(VfsExUtil.isAbsolutePath("vendor/symfony/twig-bridge/Resources/views"));
        assertFalse(VfsExUtil.isAbsolutePath("templates"));
        assertFalse(VfsExUtil.isAbsolutePath("src/Report/Resources/views"));
        assertFalse(VfsExUtil.isAbsolutePath(""));

        // edge cases that look like drive letters but are not
        assertFalse(VfsExUtil.isAbsolutePath("C:foo"));   // no separator after colon
        assertFalse(VfsExUtil.isAbsolutePath("C:"));      // too short
        assertFalse(VfsExUtil.isAbsolutePath("1:/foo"));  // digit, not letter
    }
}
