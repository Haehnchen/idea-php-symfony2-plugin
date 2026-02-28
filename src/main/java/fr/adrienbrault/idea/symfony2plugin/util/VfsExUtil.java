package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class VfsExUtil {

    @Nullable
    public static String getRelativeProjectPath(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        VirtualFile projectDir = ProjectUtil.getProjectDir(project);
        if (projectDir == null) {
            return virtualFile.getPath();
        }

        String relativePath = VfsUtil.getRelativePath(virtualFile, projectDir);
        return relativePath != null ? relativePath : virtualFile.getPath();
    }
}
