package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyTempCodeInsightFixtureTestCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tests for TwigPathServiceParser path resolution using a real IntelliJ VirtualFile and Project.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathServiceParser
 */
public class TwigPathServiceParserTempTest extends SymfonyTempCodeInsightFixtureTestCase {

    private static List<TwigPath> pathsFor(TwigPathServiceParser parser, String namespace) {
        return parser.getTwigPaths().stream()
            .filter(p -> p.getNamespace().equals(namespace))
            .collect(Collectors.toList());
    }

    /**
     * When the container is at {project}/var/cache/dev/container.xml, the Symfony root equals the
     * IntelliJ project root. Relative paths in the container should pass through unchanged.
     */
    public void testRelativePathWithSymfonyRootEqualsProjectRoot() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<container><services>" +
            "<service id=\"twig.loader.native_filesystem\">" +
            "<call method=\"addPath\">" +
            "<argument>src/Report/Resources/views</argument>" +
            "<argument>Report</argument>" +
            "</call>" +
            "</service>" +
            "</services></container>";

        createFile("src/Report/Resources/views/.keep");
        VirtualFile containerFile = createFile("var/cache/dev/container.xml", xml);

        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), containerFile, getProject());

        assertEquals("src/Report/Resources/views", pathsFor(parser,"Report").get(0).getPath());
    }

    /**
     * When the container is at {project}/symfony-app/var/cache/dev/container.xml, the Symfony root
     * is a subdirectory of the IntelliJ project. Relative paths must be prefixed with "symfony-app/".
     */
    public void testRelativePathWithNestedSymfonyRoot() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<container><services>" +
            "<service id=\"twig.loader.native_filesystem\">" +
            "<call method=\"addPath\">" +
            "<argument>src/Report/Resources/views</argument>" +
            "<argument>Report</argument>" +
            "</call>" +
            "<call method=\"addPath\">" +
            "<argument>templates</argument>" +
            "</call>" +
            "</service>" +
            "</services></container>";

        createFile("symfony-app/src/Report/Resources/views/.keep");
        createFile("symfony-app/templates/.keep");
        VirtualFile containerFile = createFile("symfony-app/var/cache/dev/container.xml", xml);

        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), containerFile, getProject());

        assertEquals("symfony-app/src/Report/Resources/views", pathsFor(parser,"Report").get(0).getPath());
        assertEquals("symfony-app/templates", pathsFor(parser,TwigUtil.MAIN).get(0).getPath());
    }

    /**
     * When var/ is not in the container path but the relative path exists under the IntelliJ project root,
     * the path is accepted as-is (direct fallback).
     */
    public void testRelativePathFallbackToProjectRootWhenVarNotFound() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<container><services>" +
            "<service id=\"twig.loader.native_filesystem\">" +
            "<call method=\"addPath\">" +
            "<argument>src/Report/Resources/views</argument>" +
            "<argument>Report</argument>" +
            "</call>" +
            "</service>" +
            "</services></container>";

        // container is NOT under a var/ directory — findSymfonyRootPrefix returns null
        VirtualFile containerFile = createFile("custom-cache/container.xml", xml);
        // but the path exists in the project root
        createFile("src/Report/Resources/views/dummy.html.twig");

        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), containerFile, getProject());

        assertEquals("src/Report/Resources/views", pathsFor(parser,"Report").get(0).getPath());
    }

    /**
     * When var/ is not found and the relative path does NOT exist under the IntelliJ project root, skip it.
     */
    public void testRelativePathFallbackSkipsNonExistentPath() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<container><services>" +
            "<service id=\"twig.loader.native_filesystem\">" +
            "<call method=\"addPath\">" +
            "<argument>does/not/exist/views</argument>" +
            "<argument>Ghost</argument>" +
            "</call>" +
            "</service>" +
            "</services></container>";

        VirtualFile containerFile = createFile("custom-cache/container.xml", xml);
        // path "does/not/exist/views" is NOT created in the project

        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), containerFile, getProject());

        assertEquals(0, pathsFor(parser,"Ghost").size());
    }

    /**
     * Absolute path without kernel.project_dir must be skipped entirely.
     */
    public void testAbsolutePathWithoutKernelProjectDirIsSkipped() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
            "<container><services>" +
            "<service id=\"twig.loader.native_filesystem\">" +
            "<call method=\"addPath\">" +
            "<argument>/var/www/project/templates</argument>" +
            "<argument>AbsoluteOnly</argument>" +
            "</call>" +
            "</service>" +
            "</services></container>";

        VirtualFile containerFile = createFile("var/cache/dev/container.xml", xml);

        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), containerFile, getProject());

        assertEquals(0, pathsFor(parser,"AbsoluteOnly").size());
    }
}
