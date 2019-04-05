package fr.adrienbrault.idea.symfony2plugin.library;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class DoctrineMetaLibraryRootProvider extends PluginLibraryRootProvider {
    @NotNull
    @Override
    public Stream<VirtualFile> getLibraryRoots(@NotNull Project project) {
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return Stream.empty();
        }
        return this.getPluginPathAsStream("/doctrine-meta/");
    }
}
