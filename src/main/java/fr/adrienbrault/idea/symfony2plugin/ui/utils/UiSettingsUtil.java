package fr.adrienbrault.idea.symfony2plugin.ui.utils;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathInterface;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathPresentable;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UiSettingsUtil {

    @Nullable
    public static String getPathDialog(@NotNull Project project, @NotNull FileType fileType) {
        return getPathDialog(project, fileType, null);
    }

    @Nullable
    public static String getPathDialog(@NotNull Project project, @NotNull FileType fileType, @Nullable String current) {
        VirtualFile projectDirectory = ProjectUtil.getProjectDir(project);

        VirtualFile selectedFileBefore = null;
        if(current != null) {
            selectedFileBefore = VfsUtil.findRelativeFile(current, projectDirectory);
        }

        VirtualFile selectedFile = FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFileDescriptor(fileType),
                project,
                selectedFileBefore
        );

        if (null == selectedFile) {
            return null;
        }

        String path = VfsUtil.getRelativePath(selectedFile, projectDirectory, '/');
        if (null == path) {
            path = selectedFile.getPath();
        }

        return path;
    }

    public static UiFilePathPresentable getPresentableFilePath(@NotNull Project project, @NotNull UiFilePathInterface uiFilePath) {
        String info = uiFilePath.exists(project) ? "EXISTS" : "NOT FOUND";
        return new UiFilePathPresentable(uiFilePath.getPath(), info);
    }
}
