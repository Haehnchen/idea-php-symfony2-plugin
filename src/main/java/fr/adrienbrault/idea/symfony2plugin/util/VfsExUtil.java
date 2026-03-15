package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
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

    /**
     * Returns true if the given path is absolute on any OS, regardless of the OS the JVM is running on.
     * Detects Unix-style paths ("/foo") and Windows drive-letter paths ("C:/foo", "C:\foo").
     * This is necessary because {@link com.intellij.openapi.util.io.FileUtil#isAbsolute} is
     * OS-sensitive: on Windows it does not recognise Unix paths (e.g. from Docker/WSL containers),
     * and on Linux it does not recognise Windows drive-letter paths.
     */
    public static boolean isAbsolutePath(@NotNull String path) {
        // Unix-style absolute path (e.g. from Docker/WSL containers on Windows)
        if (path.startsWith("/")) {
            return true;
        }
        // Windows drive-letter: "C:/" or "C:\" (cross-OS string check so it works on Linux too)
        if (path.length() >= 3 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
            char sep = path.charAt(2);
            if (sep == '/' || sep == '\\') {
                return true;
            }
        }
        // fallback for any other OS-specific absolute path formats
        return FileUtil.isAbsolute(path);
    }

    /**
     * Returns the path of the given file relative to the IntelliJ project root, or null if it
     * cannot be determined. Falls back to content roots when the project base dir is on a different
     * VirtualFileSystem than the file (e.g. in light tests where base dir is on disk but files
     * are in the in-memory temp:// VFS).
     */
    @Nullable
    public static String getRelativeProjectPathStrict(@NotNull Project project, @NotNull VirtualFile file) {
        VirtualFile baseDir = ProjectUtil.getProjectDir(project);
        if (baseDir != null && baseDir.getFileSystem().equals(file.getFileSystem())) {
            return VfsUtil.getRelativePath(file, baseDir);
        }

        for (VirtualFile root : ProjectRootManager.getInstance(project).getContentRoots()) {
            if (root.getFileSystem().equals(file.getFileSystem()) && VfsUtil.isAncestor(root, file, false)) {
                return VfsUtil.getRelativePath(file, root);
            }
        }

        return null;
    }
}
