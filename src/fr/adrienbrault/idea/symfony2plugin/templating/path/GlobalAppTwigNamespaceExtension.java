package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * app/Resources/views/foo.html.twig => :foo.html.twig
 * app/Resources/views/foo.html.twig => foo.html.twig
 *
 * /templates/foo.html.twig => foo.html.twig
 * /templates/foo.html.twig => :foo.html.twig
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class GlobalAppTwigNamespaceExtension implements TwigNamespaceExtension {
    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter) {
        VirtualFile baseDir = parameter.getProject().getBaseDir();

        Collection<TwigPath> paths = new ArrayList<>();

        List<String[]> templatePaths = Arrays.asList(
            new String[]{Settings.getInstance(parameter.getProject()).directoryToApp, "Resources", "views"},
            new String[]{"templates"}
        );

        for (String[] strings1 : templatePaths) {
            VirtualFile globalDirectory = VfsUtil.findRelativeFile(baseDir, strings1);

            if(globalDirectory == null) {
                continue;
            }

            String path = globalDirectory.getPath();

            paths.add(new TwigPath(path, TwigPathIndex.MAIN, TwigPathIndex.NamespaceType.BUNDLE));
            paths.add(new TwigPath(path, TwigPathIndex.MAIN, TwigPathIndex.NamespaceType.ADD_PATH));
        }

        return paths;
    }
}
