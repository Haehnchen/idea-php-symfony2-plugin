package fr.adrienbrault.idea.symfony2plugin.ui.dict;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface UiFilePathInterface {
    boolean exists(@NotNull Project project);
    void setPath(String path);
    String getPath();
}
