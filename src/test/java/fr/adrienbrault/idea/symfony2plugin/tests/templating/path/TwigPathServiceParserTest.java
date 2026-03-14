package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathServiceParserTest extends Assert {
    @Test
    public void testParse() throws Exception {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/path/appDevDebugProjectContainer.xml");

        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new FileInputStream(testFile), null, null);

        assertEquals("vendor\\symfony\\symfony\\src\\Symfony\\Bundle\\FrameworkBundle/Resources/views", parser.getTwigPathIndex().getNamespacePaths("Framework").get(0).getPath());
        assertEquals("vendor\\foo\\bar\\FooBundle/Resources/views", parser.getTwigPathIndex().getNamespacePaths("Framework").get(1).getPath());

        assertEquals("vendor\\symfony\\symfony\\src\\Symfony\\Bridge\\Twig/Resources/views/Form", parser.getTwigPathIndex().getNamespacePaths(TwigUtil.MAIN).get(0).getPath());
        assertEquals("app/Resources/views", parser.getTwigPathIndex().getNamespacePaths(TwigUtil.MAIN).get(1).getPath());

        assertEquals("/app/vendor/symfony/twig-bundle/Resources/views2", parser.getTwigPathIndex().getNamespacePaths("Twig2").get(0).getPath());
        assertEquals(0, parser.getTwigPathIndex().getNamespacePaths("!Twig2").size());
    }

    @Test
    public void testParseWithKernelProjectDir() throws Exception {
        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/path/fixtures/container_with_project_dir.xml");

        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new FileInputStream(testFile), null, null);

        // /app/templates with kernel.project_dir=/app → "templates" (no IntelliJ root without VirtualFile)
        assertEquals("templates", parser.getTwigPathIndex().getNamespacePaths(TwigUtil.MAIN).get(0).getPath());

        // /app/vendor/symfony/twig-bridge/Resources/views with kernel.project_dir=/app → relative
        assertEquals("vendor/symfony/twig-bridge/Resources/views", parser.getTwigPathIndex().getNamespacePaths("SymfonyBridge").get(0).getPath());

        // already relative path stays unchanged
        assertEquals("relative/path/views", parser.getTwigPathIndex().getNamespacePaths("AlreadyRelative").get(0).getPath());
    }
}
