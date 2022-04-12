package fr.adrienbrault.idea.symfony2plugin.ui.utils;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.plugins.webDeployment.config.Deployable;import com.jetbrains.plugins.webDeployment.config.FileTransferConfig;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import com.jetbrains.plugins.webDeployment.ui.ServerBrowserDialog;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathInterface;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.UiFilePathPresentable;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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

    public static void openFileDialogForDefaultWebServerConnection(@NotNull Project project, @NotNull WebServerFileDialogCallback callback) {
        WebServerConfig server = RemoteWebServerUtil.findDefaultServer(project);
        if(server == null) {
            callback.noDefaultServer();
            return;
        }

        String rootPath = server.getFileTransferConfig().getRootFolder();
        ServerBrowserDialog d = new ServerBrowserDialog(project, Deployable.create(server, project), String.format("Remote file: %s", server.getName()), false, FileTransferConfig.Origin.Default, new WebServerConfig.RemotePath(rootPath));
        d.show();
        if (!d.isOK()) {
            return;
        }

        WebServerConfig.RemotePath path = d.getPath();
        if (path != null && callback.validate(server, path)) {
            callback.success(server, path);
        } else {
            JOptionPane.showMessageDialog(null, "Invalid file selected", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public interface WebServerFileDialogCallback {
        void noDefaultServer();
        boolean validate(@NotNull WebServerConfig server, @NotNull WebServerConfig.RemotePath remotePath);
        void success(@NotNull WebServerConfig server, @NotNull WebServerConfig.RemotePath remotePath);
    }

    public static UiFilePathPresentable getPresentableFilePath(@NotNull Project project, @NotNull UiFilePathInterface uiFilePath) {
        String info;
        if(uiFilePath.isRemote()) {
            info = "REMOTE";
        } else {
            info = uiFilePath.exists(project) ? "EXISTS" : "NOT FOUND";
        }

        return new UiFilePathPresentable(uiFilePath.getPath(), info);
    }
}
