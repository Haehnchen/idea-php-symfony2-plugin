package fr.adrienbrault.idea.symfony2plugin.tests.templating.path;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathServiceParser;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see TwigPathServiceParser
 */
public class TwigPathServiceParserTest extends SymfonyLightCodeInsightFixtureTestCase {

    private static List<TwigPath> pathsFor(TwigPathServiceParser parser, String namespace) {
        return parser.getTwigPaths().stream()
            .filter(p -> p.getNamespace().equals(namespace))
            .collect(Collectors.toList());
    }

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

        VirtualFile containerFile = myFixture.addFileToProject("custom-cache/container.xml", xml).getVirtualFile();

        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), containerFile, getProject());

        assertEquals(0, pathsFor(parser, "Ghost").size());
    }

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

        VirtualFile containerFile = myFixture.addFileToProject("var/cache/dev/container.xml", xml).getVirtualFile();

        TwigPathServiceParser parser = new TwigPathServiceParser();
        parser.parser(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), containerFile, getProject());

        assertEquals(0, pathsFor(parser, "AbsoluteOnly").size());
    }
}
