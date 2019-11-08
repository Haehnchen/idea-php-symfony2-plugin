package fr.adrienbrault.idea.symfonyplugin.templating.path;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfonyplugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfonyplugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfonyplugin.util.FilesystemUtil;
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
            paths.add(new TwigPath(path, TwigUtil.MAIN, TwigUtil.NamespaceType.BUNDLE));
            paths.add(new TwigPath(path, TwigUtil.MAIN, TwigUtil.NamespaceType.ADD_PATH));
        });

        return paths;
    }
}
