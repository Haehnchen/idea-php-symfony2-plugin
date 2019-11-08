package fr.adrienbrault.idea.symfonyplugin.templating.path;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfonyplugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfonyplugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfonyplugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfonyplugin.util.SymfonyBundleUtil;
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
                twigPaths.add(new TwigPath(views.getPath(), key, TwigUtil.NamespaceType.BUNDLE));
            }
        });

        return twigPaths;
    }
}
