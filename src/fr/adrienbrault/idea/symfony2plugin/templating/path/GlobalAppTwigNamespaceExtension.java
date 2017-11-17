package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.util.FilesystemUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

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

        // "app" folder
        Collection<VirtualFile> directories = FilesystemUtil.getAppDirectories(parameter.getProject()).stream()
            .map(path -> VfsUtil.findRelativeFile(path, "Resources", "views"))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        // flex "templates" in root
        VirtualFile templates = VfsUtil.findRelativeFile(baseDir, "templates");
        if(templates != null) {
            directories.add(templates);
        }

        Collection<TwigPath> paths = new ArrayList<>();

        directories.stream().map(VirtualFile::getPath).forEach(path -> {
            paths.add(new TwigPath(path, TwigPathIndex.MAIN, TwigPathIndex.NamespaceType.BUNDLE));
            paths.add(new TwigPath(path, TwigPathIndex.MAIN, TwigPathIndex.NamespaceType.ADD_PATH));
        });

        return paths;
    }
}
