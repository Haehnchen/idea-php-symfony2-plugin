package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * app/Resources/views/foo.html.twig => :foo.html.twig
 * app/Resources/views/foo.html.twig => foo.html.twig
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GlobalAppTwigNamespaceExtension implements TwigNamespaceExtension {
    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter) {
        String appDirectoryName = Settings.getInstance(parameter.getProject()).directoryToApp + "/Resources/views";
        VirtualFile baseDir = parameter.getProject().getBaseDir();

        VirtualFile globalDirectory = VfsUtil.findRelativeFile(baseDir, appDirectoryName.split("/"));
        if(globalDirectory == null) {
            return Collections.emptyList();
        }

        String path = globalDirectory.getPath();

        return Arrays.asList(
            new TwigPath(path, TwigPathIndex.MAIN, TwigPathIndex.NamespaceType.BUNDLE),
            new TwigPath(path, TwigPathIndex.MAIN, TwigPathIndex.NamespaceType.ADD_PATH)
        );
    }
}
