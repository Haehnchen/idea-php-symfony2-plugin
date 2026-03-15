package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import com.intellij.openapi.vfs.VfsUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.File;
import java.io.FileInputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathServiceParserTest extends SymfonyTempCodeInsightFixtureTestCase {

    public void testParse() throws Exception {
        createFile("vendor/symfony/symfony/src/Symfony/Bundle/FrameworkBundle/Resources/views/.keep");
        createFile("vendor/foo/bar/FooBundle/Resources/views/.keep");
        createFile("vendor/symfony/symfony/src/Symfony/Bridge/Twig/Resources/views/Form/.keep");
        createFile("app/Resources/views/.keep");

        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/path/appDevDebugProjectContainer.xml");
        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());

        assertEquals("vendor\\symfony\\symfony\\src\\Symfony\\Bundle\\FrameworkBundle/Resources/views", parser.getTwigPathIndex().getNamespacePaths("Framework").get(0).getPath());
        assertEquals("vendor\\foo\\bar\\FooBundle/Resources/views", parser.getTwigPathIndex().getNamespacePaths("Framework").get(1).getPath());

        assertEquals("vendor\\symfony\\symfony\\src\\Symfony\\Bridge\\Twig/Resources/views/Form", parser.getTwigPathIndex().getNamespacePaths(TwigUtil.MAIN).get(0).getPath());
        assertEquals("app/Resources/views", parser.getTwigPathIndex().getNamespacePaths(TwigUtil.MAIN).get(1).getPath());

        // absolute path without kernel.project_dir cannot be relativized — must be skipped
        assertEquals(0, parser.getTwigPathIndex().getNamespacePaths("Twig2").size());
        assertEquals(0, parser.getTwigPathIndex().getNamespacePaths("!Twig2").size());
    }

    public void testParseWithKernelProjectDir() throws Exception {
        createFile("templates/.keep");
        createFile("vendor/symfony/twig-bridge/Resources/views/.keep");
        createFile("relative/path/views/.keep");
        createFile("src/Report/Resources/views/.keep");

        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/path/fixtures/container_with_project_dir.xml");
        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());

        // /app/templates with kernel.project_dir=/app → "templates"
        assertEquals("templates", parser.getTwigPathIndex().getNamespacePaths(TwigUtil.MAIN).get(0).getPath());

        // /app/vendor/symfony/twig-bridge/Resources/views with kernel.project_dir=/app → relative
        assertEquals("vendor/symfony/twig-bridge/Resources/views", parser.getTwigPathIndex().getNamespacePaths("SymfonyBridge").get(0).getPath());

        // already relative path passes through
        assertEquals("relative/path/views", parser.getTwigPathIndex().getNamespacePaths("AlreadyRelative").get(0).getPath());

        // relative path with namespace passes through
        assertEquals("src/Report/Resources/views", parser.getTwigPathIndex().getNamespacePaths("Report").get(0).getPath());
    }
}
