package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
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
    public synchronized void parser(@NotNull InputStream file, @NotNull VirtualFile sourceFile, @NotNull Project project) {
        Document document = parseDocument(file);
        if (document == null) {
            return;
        }

        String kernelProjectDir = extractKernelProjectDir(document);
        String symfonyRootPrefix = findSymfonyRootPrefix(project, sourceFile);

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

            if (path == null) {
                continue;
            }

            // Step 1: normalize absolute paths to relative using kernel.project_dir.
            // VfsExUtil.isAbsolutePath catches both Windows ("C:/") and Linux ("/") style paths
            // so Docker-generated Linux paths are handled correctly on Windows too.
            String relativePath;
            if (VfsExUtil.isAbsolutePath(path)) {
                if (kernelProjectDir == null) {
                    continue;
                }
                relativePath = normalizeAbsolutePath(path, kernelProjectDir);
                if (relativePath == null) {
                    continue;
                }
            } else {
                relativePath = path;
            }

            // Step 2: apply Symfony root prefix, then verify the resolved path exists in the project
            if (symfonyRootPrefix != null && !symfonyRootPrefix.isEmpty()) {
                relativePath = symfonyRootPrefix + "/" + relativePath;
            }

            if (existsInProjectRoot(project, relativePath)) {
                twigPathIndex.addPath(new TwigPath(relativePath, namespace));
            }
        }
    }

    /**
     * Strip kernel.project_dir prefix from an absolute path to produce a relative path.
     * Returns null if the path does not start with kernelProjectDir.
     */
    @Nullable
    static String normalizeAbsolutePath(@NotNull String path, @NotNull String kernelProjectDir) {
        String normalizedPath = path.replace('\\', '/');
        String normalizedDir = kernelProjectDir.replace('\\', '/').replaceAll("/+$", "");

        if (normalizedPath.startsWith(normalizedDir + "/")) {
            return normalizedPath.substring(normalizedDir.length() + 1);
        } else if (normalizedPath.equals(normalizedDir)) {
            return "";
        }

        return null;
    }

    /**
     * Find the Symfony project root (parent of the "var" directory in the container file path)
     * and return its path relative to the IntelliJ project root.
     *
     * Returns "" when the Symfony root equals the IntelliJ project root.
     * Returns null when the Symfony root cannot be determined or is not inside the IntelliJ project.
     */
    @Nullable
    static String findSymfonyRootPrefix(@NotNull Project project, @Nullable VirtualFile containerFile) {
        if (containerFile == null) {
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
                VirtualFile symfonyRoot = current.getParent();
                if (symfonyRoot == null) {
                    return null;
                }

                String symfonyRootPath = symfonyRoot.getPath();
                if (symfonyRootPath.equals(intellijProjectPath)) {
                    return "";
                } else if (symfonyRootPath.startsWith(intellijProjectPath + "/")) {
                    return symfonyRootPath.substring(intellijProjectPath.length() + 1);
                }

                return null;
            }
            current = current.getParent();
        }

        return null;
    }

    /**
     * Returns true if the given relative path exists as a directory or file under the IntelliJ project root.
     */
    static boolean existsInProjectRoot(@NotNull Project project, @NotNull String relativePath) {
        VirtualFile intellijProjectDir = ProjectUtil.getProjectDir(project);
        if (intellijProjectDir == null) {
            return false;
        }

        return intellijProjectDir.findFileByRelativePath(relativePath.replace('\\', '/')) != null;
    }

    @Nullable
    static String extractKernelProjectDir(@NotNull Document document) {
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
