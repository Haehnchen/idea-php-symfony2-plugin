package fr.adrienbrault.idea.symfonyplugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import fr.adrienbrault.idea.symfonyplugin.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FilesystemUtil {

    @Nullable
    public static PsiDirectory findParentBundleFolder(@NotNull PsiDirectory directory) {

        // self click
        if(directory.isDirectory() && directory.getName().endsWith("Bundle")) {
            return directory;
        }

        for (PsiDirectory parent = directory.getParent(); parent != null; parent = parent.getParent()) {
            if(parent.isDirectory() && parent.getName().endsWith("Bundle")) {
                return parent;
            }
        }

        return null;
    }

    /**
     * Try to find an "app" directory on configuration or on project directory in root
     * We also support absolute path in configuration
     */
    @NotNull
    public static Collection<VirtualFile> getAppDirectories(@NotNull Project project) {
        Collection<VirtualFile> virtualFiles = new HashSet<>();

        // find "app" folder on user settings
        String directoryToApp = Settings.getInstance(project).directoryToApp;

        if(FileUtil.isAbsolute(directoryToApp)) {
            // absolute dir given
            VirtualFile fileByIoFile = VfsUtil.findFileByIoFile(new File(directoryToApp), true);
            if(fileByIoFile != null) {
                virtualFiles.add(fileByIoFile);
            }
        } else {
            // relative path resolve
            VirtualFile globalDirectory = VfsUtil.findRelativeFile(
                project.getBaseDir(),
                directoryToApp.replace("\\", "/").split("/")
            );

            if(globalDirectory != null) {
                virtualFiles.add(globalDirectory);
            }
        }

        // global "app" in root
        VirtualFile templates = VfsUtil.findRelativeFile(project.getBaseDir(), "app");
        if(templates != null) {
            virtualFiles.add(templates);
        }

        return virtualFiles;
    }
}
