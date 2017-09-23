package fr.adrienbrault.idea.symfony2plugin.templating.path;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import org.jetbrains.annotations.NotNull;
;
import java.util.Collection;
import java.util.Collections;

/**
 * https://symfony.com/doc/current/setup/flex.html#upgrading-existing-applications-to-flex
 *
 * /templates
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FlexTwigNamespaceExtension implements TwigNamespaceExtension {
    @NotNull
    @Override
    public Collection<TwigPath> getNamespaces(@NotNull TwigNamespaceExtensionParameter parameter) {
        VirtualFile templatesDirectory = VfsUtil.findRelativeFile(parameter.getProject().getBaseDir(), "templates");
        if(templatesDirectory == null) {
            return Collections.emptyList();
        }

        String path = templatesDirectory.getPath();
        return Collections.singletonList(
            new TwigPath(path, TwigPathIndex.MAIN, TwigPathIndex.NamespaceType.ADD_PATH)
        );
    }
}
