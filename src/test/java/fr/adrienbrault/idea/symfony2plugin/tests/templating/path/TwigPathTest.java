package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigPath
 */
public class TwigPathTest extends SymfonyLightCodeInsightFixtureTestCase {
    public void testRelativePathResolving() {
        myFixture.addFileToProject("app/views", "");

        TwigPath twigPath = new TwigPath("app", "namespace");
        assertEquals("app", twigPath.getDirectory(getProject()).getName());

        assertEquals("app", twigPath.getPath());
        assertEquals("namespace", twigPath.getNamespace());

        assertEquals("app", twigPath.getRelativePath(getProject()));
    }
}
