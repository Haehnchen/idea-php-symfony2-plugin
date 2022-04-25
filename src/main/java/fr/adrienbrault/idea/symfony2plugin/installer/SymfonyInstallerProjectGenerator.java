package fr.adrienbrault.idea.symfony2plugin.installer;

import com.intellij.ide.util.projectWizard.WebProjectTemplate;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.ProjectGeneratorPeer;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerProjectGenerator extends WebProjectTemplate<SymfonyInstallerSettings> {
    @Nls
    @NotNull
    @Override
    public String getName() {
        return "Symfony";
    }

    @Override
    public String getDescription() {
        return "Create a new Symfony project by using the external \"Symfony Symfony CLI\". Download it via <a href=\"https://symfony.com/download\">https://symfony.com/download</a>";
    }

    @Override
    public void generateProject(@NotNull final Project project, final @NotNull VirtualFile baseDir, final @NotNull SymfonyInstallerSettings settings, @NotNull Module module) {
        final File baseDirFile = new File(baseDir.getPath());

        // @TODO: reimplement binary download
        // https://api.github.com/repos/symfony-cli/symfony-cli/releases/64816848/assets
        // https://api.github.com/repos/symfony-cli/symfony-cli/releases
        /*
        final File tempFile = FileUtil.findSequentNonexistentFile(baseDirFile, "symfony", "");

        String composerPath;
        File symfonyInProject = null;
        if (settings.isDownload()) {

            VirtualFile file = SymfonyInstallerUtil.downloadPhar(project, null, tempFile.getPath());
            if (file == null)  {
                showErrorNotification(project, "Cannot download symfony.phar file");
                Symfony2ProjectComponent.getLogger().warn("Cannot download symfony.phar file");
                return;
            }

            composerPath = file.getPath();
            symfonyInProject = tempFile;
        } else {
            composerPath = settings.getExistingPath();
        }
        */

        String[] commands = SymfonyInstallerUtil.getCreateProjectCommand(settings.getVersion(), "symfony", baseDir.getPath(), settings.getPhpInterpreter(), settings.getProjectType());

        final File finalSymfonyInProject = null;
        SymfonyInstallerCommandExecutor executor = new SymfonyInstallerCommandExecutor(project, baseDir, commands) {
            @Override
            protected void onFinish(@Nullable String message) {
                IdeHelper.enablePluginAndConfigure(project);

                if(message != null) {
                    // replace empty lines, provide html output, and remove our temporary path
                    showInfoNotification(project, message
                        .replaceAll("(?m)^\\s*$[\n\r]{1,}", "")
                        .replaceAll("(\r\n|\n)", "<br />")
                        .replace("/" + SymfonyInstallerUtil.PROJECT_SUB_FOLDER, "")
                    );
                }

                // remove temporary symfony installer folder
                if(finalSymfonyInProject != null) {
                    FileUtil.delete(finalSymfonyInProject);
                }

            }

            @Override
            protected void onError(@NotNull String message) {
                showErrorNotification(project, message);
            }

            @Override
            protected String getProgressTitle() {
                return String.format("Installing Symfony %s", settings.getVersion().getPresentableName());
            }
        };

        executor.execute();
    }

    private static void showErrorNotification(@NotNull Project project, @NotNull String content)
    {
        Notifications.Bus.notify(new Notification(SymfonyInstallerUtil.INSTALLER_GROUP_DISPLAY_ID, "Symfony-Installer", content, NotificationType.ERROR), project);
    }

    private static void showInfoNotification(@NotNull Project project, @NotNull String content)
    {
        Notifications.Bus.notify(new Notification(SymfonyInstallerUtil.INSTALLER_GROUP_DISPLAY_ID, "Symfony-Installer", content, NotificationType.INFORMATION), project);
    }

    @NotNull
    @Override
    public ProjectGeneratorPeer<SymfonyInstallerSettings> createPeer() {
        return new SymfonyInstallerGeneratorPeer();
    }

    @Override
    public Icon getIcon() {
        return Symfony2Icons.SYMFONY;
    }
}
