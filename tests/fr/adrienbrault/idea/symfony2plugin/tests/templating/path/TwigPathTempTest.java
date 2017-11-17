package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import com.intellij.util.SystemIndependent;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath#TwigPath
 */
public class TwigPathTempTest extends SymfonyTempCodeInsightFixtureTestCase {
    public void testRelativePathResolving() {
        createFile("app/views");

        TwigPath twigPath = new TwigPath("app", "namespace");
        assertEquals("app", twigPath.getDirectory(getProject()).getName());

        assertEquals("app", twigPath.getPath());
        assertEquals("namespace", twigPath.getNamespace());

        assertEquals("app", twigPath.getRelativePath(getProject()));
    }

    public void testAbsolutePathResolving() {
        createFile("app/views");

        @SystemIndependent String basePath = getProject().getBasePath();
        TwigPath twigPath = new TwigPath(basePath + "/app", "namespace");
        assertEquals("app", twigPath.getDirectory(getProject()).getName());

        assertTrue(twigPath.getPath().endsWith("app"));
        assertEquals("namespace", twigPath.getNamespace());

        assertEquals("app", twigPath.getRelativePath(getProject()));
    }
}
