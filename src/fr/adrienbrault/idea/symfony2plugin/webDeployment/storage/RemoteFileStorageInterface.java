package fr.adrienbrault.idea.symfony2plugin.webDeployment.storage;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface RemoteFileStorageInterface<V> {
    Collection<String> files(@NotNull Project project);
    void build(@NotNull Project project, @NotNull Collection<String> content);
    V getState();
    void clear();
}