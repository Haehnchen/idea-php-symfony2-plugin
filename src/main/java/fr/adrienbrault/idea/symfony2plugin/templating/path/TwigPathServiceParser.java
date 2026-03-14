package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigPathServiceParser extends AbstractServiceParser {
    @NotNull
    private final TwigPathIndex twigPathIndex = new TwigPathIndex();

    @Override
    public String getXPathFilter() {
        return "/container/services/service[@id[starts-with(.,'twig.loader')]]//call[@method='addPath']";
    }

    @Override
    public synchronized void parser(InputStream file, @Nullable VirtualFile sourceFile, @Nullable Project project) {
        Document document = parseDocument(file);
        if (document == null) {
            return;
        }

        String kernelProjectDir = extractKernelProjectDir(document);
        String intellijProjectRoot = findProjectRootFromContainer(project, sourceFile);

        NodeList nodeList = queryTwigPaths(document);
        if (nodeList == null) {
            return;
        }

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            NodeList arguments = node.getElementsByTagName("argument");

            String path = null;
            String namespace = TwigUtil.MAIN;

            if (arguments.getLength() == 1) {
                path = arguments.item(0).getTextContent();
            } else if (arguments.getLength() == 2) {
                path = arguments.item(0).getTextContent();
                namespace = arguments.item(1).getTextContent();

                // we ignore overwrites; they are added also without "!", so just skip it
                if (namespace.startsWith("!")) {
                    continue;
                }
            }

            if (path != null) {
                String resolvedPath = resolvePath(path, kernelProjectDir, intellijProjectRoot);
                twigPathIndex.addPath(new TwigPath(resolvedPath, namespace));
            }
        }
    }

    /**
     * Resolve a Twig path to IntelliJ project path.
     *
     * 1. Normalize: absolute path + kernel.project_dir → relative path
     * 2. Resolve: relative path + IntelliJ project root → absolute IntelliJ path
     */
    @NotNull
    private String resolvePath(@NotNull String path, @Nullable String kernelProjectDir, @Nullable String intellijProjectRoot) {
        String relativePath = normalizePath(path, kernelProjectDir);

        if (!FileUtil.isAbsolute(relativePath) && intellijProjectRoot != null) {
            return intellijProjectRoot + "/" + relativePath;
        }

        return relativePath;
    }

    /**
     * Normalize absolute path to relative using kernel.project_dir.
     */
    @NotNull
    private String normalizePath(@NotNull String path, @Nullable String kernelProjectDir) {
        if (!FileUtil.isAbsolute(path)) {
            return path;
        }

        if (kernelProjectDir == null) {
            return path;
        }

        String normalizedPath = path.replace('\\', '/');
        String normalizedDir = kernelProjectDir.replace('\\', '/').replaceAll("/+$", "");

        if (normalizedPath.startsWith(normalizedDir + "/")) {
            return normalizedPath.substring(normalizedDir.length() + 1);
        } else if (normalizedPath.equals(normalizedDir)) {
            return "";
        }

        return path;
    }


    /**
     * Find IntelliJ project root by locating "var" directory in container file path.
     * Container is typically at: project_root/var/cache/dev/srcDevDebugProjectContainer.xml
     *
     * Traversal stops at the IntelliJ project boundary — never walks above it.
     */
    @Nullable
    private String findProjectRootFromContainer(@Nullable Project project, @Nullable VirtualFile containerFile) {
        if (containerFile == null || project == null) {
            return null;
        }

        VirtualFile intellijProjectDir = ProjectUtil.getProjectDir(project);
        if (intellijProjectDir == null) {
            return null;
        }
        String intellijProjectPath = intellijProjectDir.getPath();

        VirtualFile current = containerFile.getParent();
        while (current != null && current.getPath().startsWith(intellijProjectPath)) {
            if ("var".equals(current.getName())) {
                VirtualFile projectRoot = current.getParent();
                if (projectRoot != null) {
                    return projectRoot.getPath();
                }
            }
            current = current.getParent();
        }

        return null;
    }

    @Nullable
    private String extractKernelProjectDir(@NotNull Document document) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(
                "/container/parameters/parameter[@key='kernel.project_dir']"
            );
            Node node = (Node) expr.evaluate(document, XPathConstants.NODE);
            return node != null ? StringUtils.stripToNull(node.getTextContent()) : null;
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    @Nullable
    private NodeList queryTwigPaths(@NotNull Document document) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(
                "/container/services/service[@id[starts-with(.,'twig.loader')]]//call[@method='addPath']"
            );
            return (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    @Nullable
    private Document parseDocument(@NotNull InputStream inputStream) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder documentBuilder = dbFactory.newDocumentBuilder();
            return documentBuilder.parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return null;
        }
    }

    @NotNull
    public TwigPathIndex getTwigPathIndex() {
        return twigPathIndex;
    }
}
