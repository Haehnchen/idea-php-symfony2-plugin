package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * "app/Resources/ParentBundle/Resources/views"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class BundleOverwriteNamespaceExtensions implements TwigNamespaceExtension {
    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter) {
        Collection<TwigPath> twigPaths = new ArrayList<>();

        new SymfonyBundleUtil(parameter.getProject()).getParentBundles().forEach((key, virtualFile) -> {
            VirtualFile views = virtualFile.getRelative("Resources/views");
            if (views != null) {
                twigPaths.add(new TwigPath(views.getPath(), key, TwigPathIndex.NamespaceType.BUNDLE));
            }
        });

        return twigPaths;
    }
}
