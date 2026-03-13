package fr.adrienbrault.idea.symfony2plugin.templating.path;

import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Collects path on compiled container: appDevDebugProjectContainer.xml
 *
 * <call method="addPath">
 *  <argument>... ymfony\Bundle\FrameworkBundle/Resources/views</argument>
 *  <argument>Framework</argument>
 * </call>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerTwigNamespaceExtension implements TwigNamespaceExtension {
    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter) {
        TwigPathServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(parameter.getProject(), TwigPathServiceParser.class);
        Collection<TwigPath> twigPaths = twigPathServiceParser.getTwigPathIndex().getTwigPaths();

        // Get kernel.project_dir for Docker/VM path mapping support
        String kernelProjectDir = getKernelProjectDir(parameter);

        // If no kernel.project_dir found, return paths as-is
        if (kernelProjectDir == null) {
            return new ArrayList<>(twigPaths);
        }

        // Convert absolute container paths to relative paths
        Collection<TwigPath> convertedPaths = new ArrayList<>();
        for (TwigPath twigPath : twigPaths) {
            String path = twigPath.getPath();
            String convertedPath = convertContainerPathToRelative(path, kernelProjectDir);

            // Create new TwigPath with converted path
            if (!convertedPath.equals(path)) {
                convertedPaths.add(TwigPath.createTwigPath(
                    convertedPath,
                    twigPath.getNamespace(),
                    twigPath.getNamespaceType(),
                    twigPath.isCustomPath(),
                    twigPath.isEnabled()
                ));
            } else {
                convertedPaths.add(twigPath);
            }
        }

        return convertedPaths;
    }

    /**
     * Gets kernel.project_dir from cached ParameterCollector.
     */
    @Nullable
    private String getKernelProjectDir(@NotNull TwigNamespaceExtensionParameter parameter) {
        ContainerParameter containerParameter = ContainerCollectionResolver
            .getParameters(parameter.getProject())
            .get("kernel.project_dir");

        if (containerParameter != null) {
            String value = containerParameter.getValue();
            return StringUtils.isNotBlank(value) ? value.trim() : null;
        }

        return null;
    }

    /**
     * Converts container/VM absolute paths to relative paths.
     * Uses kernel.project_dir to determine what should be stripped from paths.
     */
    @NotNull
    private String convertContainerPathToRelative(@NotNull String path, @NotNull String kernelProjectDir) {
        if (path.startsWith(kernelProjectDir)) {
            // Remove kernel.project_dir prefix and leading slash
            String relativePath = StringUtils.stripStart(path.substring(kernelProjectDir.length()), "/");
            if (StringUtils.isNotBlank(relativePath)) {
                return relativePath;
            }
        }

        // If path doesn't start with kernel.project_dir, return as-is
        return path;
    }
}
