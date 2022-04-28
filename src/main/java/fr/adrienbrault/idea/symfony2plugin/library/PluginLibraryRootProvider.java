package fr.adrienbrault.idea.symfony2plugin.library;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.php.config.library.PhpLibraryRootProvider;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.stream.Stream;

public abstract class PluginLibraryRootProvider implements PhpLibraryRootProvider {

    PluginLibraryRootProvider() {
    }

    @NotNull
    @Override
    public abstract Stream<VirtualFile> getLibraryRoots(@NotNull Project project);

    @Override
    public boolean isRuntime() {
        return false;
    }

    Stream<VirtualFile> getPluginPathAsStream(@NotNull String path) {
        URL url = PluginLibraryRootProvider.class.getResource(path);

        if (url == null) {
            return Stream.empty();
        }

        VirtualFile root = VfsUtil.findFileByURL(url);
        if (root == null || !root.isDirectory()) {
            return Stream.empty();
        }

        return Stream.of(root);
    }
}
