package fr.adrienbrault.idea.symfonyplugin.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class VfsExUtil {

    public static String getRelativeProjectPath(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        // hacking around project as temp file
        if(ApplicationManager.getApplication().isUnitTestMode()) {
            return virtualFile.getPath();
        }
        return VfsUtil.getRelativePath(virtualFile, project.getBaseDir());
    }
}
