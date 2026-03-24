package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import com.intellij.openapi.vfs.VfsUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathServiceParserTest extends SymfonyTempCodeInsightFixtureTestCase {

    private static List<TwigPath> pathsFor(TwigPathServiceParser parser, String namespace) {
        return parser.getTwigPaths().stream()
            .filter(p -> p.getNamespace().equals(namespace))
            .collect(Collectors.toList());
    }

    public void testParse() throws Exception {
        createFile("vendor/symfony/symfony/src/Symfony/Bundle/FrameworkBundle/Resources/views/.keep");
        createFile("vendor/foo/bar/FooBundle/Resources/views/.keep");
        createFile("vendor/symfony/symfony/src/Symfony/Bridge/Twig/Resources/views/Form/.keep");
        createFile("app/Resources/views/.keep");

        File testFile = new File("src/test/java/fr/adrienbrault/idea/symfony2plugin/tests/templating/path/appDevDebugProjectContainer.xml");
        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new FileInputStream(testFile), VfsUtil.findFileByIoFile(testFile, true), getProject());

        List<TwigPath> frameworkPaths = pathsFor(parser, "Framework");
        assertEquals("vendor\\symfony\\symfony\\src\\Symfony\\Bundle\\FrameworkBundle/Resources/views", frameworkPaths.get(0).getPath());
        assertEquals("vendor\\foo\\bar\\FooBundle/Resources/views", frameworkPaths.get(1).getPath());

        List<TwigPath> mainPaths = pathsFor(parser, TwigUtil.MAIN);
        assertEquals("vendor\\symfony\\symfony\\src\\Symfony\\Bridge\\Twig/Resources/views/Form", mainPaths.get(0).getPath());
        assertEquals("app/Resources/views", mainPaths.get(1).getPath());

        // absolute path without kernel.project_dir cannot be relativized — must be skipped
        assertEquals(0, pathsFor(parser, "Twig2").size());
        assertEquals(0, pathsFor(parser, "!Twig2").size());
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
        assertEquals("templates", pathsFor(parser, TwigUtil.MAIN).get(0).getPath());

        // /app/vendor/symfony/twig-bridge/Resources/views with kernel.project_dir=/app → relative
        assertEquals("vendor/symfony/twig-bridge/Resources/views", pathsFor(parser, "SymfonyBridge").get(0).getPath());

        // already relative path passes through
        assertEquals("relative/path/views", pathsFor(parser, "AlreadyRelative").get(0).getPath());

        // relative path with namespace passes through
        assertEquals("src/Report/Resources/views", pathsFor(parser, "Report").get(0).getPath());
    }
}
