package fr.adrienbrault.idea.symfony2plugin.installer;

import com.intellij.ide.util.projectWizard.WebProjectTemplate;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.ProjectGeneratorPeer;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.apache.commons.lang.SystemUtils;
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
        File symfonyInProject = null;
        String binaryPath = SystemUtils.IS_OS_WINDOWS ? "symfony.exe" : "symfony";

        if (!checkBinaryValidity(binaryPath) && settings.isDownloadInstallerSelected()) {
            String projectBinaryPath = null;
            try {
                projectBinaryPath = ProgressManager.getInstance().run(new Task.WithResult<String, Exception>(null, "Downloading Symfony CLI", false) {
                    @Override
                    protected String compute(@NotNull ProgressIndicator indicator) {
                        String releaseUrl = SymfonyInstallerUtil.getReleaseUrl();
                        if (releaseUrl == null) {
                            return null;
                        }

                        return SymfonyInstallerUtil.extractTarGZ(releaseUrl, baseDir.getPath());
                    }
                });
            } catch (Exception ignored) {
            }

            if (projectBinaryPath == null) {
                showErrorNotification(project, "Cannot download or find a matching Symfony CLI binary architecture");
                Symfony2ProjectComponent.getLogger().warn("Cannot download or find a matching Symfony CLI binary architecture");
                return;
            }

            symfonyInProject = new File(projectBinaryPath);
            binaryPath = projectBinaryPath;
        }

        if (!checkBinaryValidity(binaryPath)) {
            showErrorNotification(project, "Symfony CLI could not be executed: " + binaryPath);
            Symfony2ProjectComponent.getLogger().warn("Symfony CLI could not be executed: " + binaryPath);
            return;
        }

        String[] commands = SymfonyInstallerUtil.getCreateProjectCommand(settings.getVersion(), binaryPath, baseDir.getPath() + "/" + SymfonyInstallerUtil.PROJECT_SUB_FOLDER, settings.getProjectType());

        final File finalSymfonyInProject = symfonyInProject;
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

    private boolean checkBinaryValidity(@NotNull String binary) {
        Boolean installerExists = false;
        try {
            installerExists = ProgressManager.getInstance().run(new Task.WithResult<Boolean, Exception>(null, "Checking Symfony CLI Validity", false) {
                @Override
                protected Boolean compute(@NotNull ProgressIndicator indicator) {
                    return SymfonyInstallerUtil.isValidSymfonyCliToolsCommand(binary);
                }
            });
        } catch (Exception ignored) {
        }

        return installerExists;
    }
}
