package fr.adrienbrault.idea.symfonyplugin.webDeployment.storage;

import com.intellij.openapi.project.Project;
import org.apache.commons.vfs2.FileObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface RemoteFileStorageInterface<V> {
    @NotNull
    Collection<String> files(@NotNull Project project);

    void build(@NotNull Project project, @NotNull Collection<FileObject> fileObjects);

    @NotNull
    V getState();

    void clear();
}