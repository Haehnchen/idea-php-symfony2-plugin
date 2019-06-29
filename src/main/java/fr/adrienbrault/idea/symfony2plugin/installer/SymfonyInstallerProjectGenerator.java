package fr.adrienbrault.idea.symfony2plugin.installer;

import com.intellij.ide.util.projectWizard.WebProjectTemplate;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PlatformUtils;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
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
        return "Create a new Symfony project by using the external \"Symfony Installer\". A local PHP interpreter is required.";
    }

    @Override
    public void generateProject(@NotNull final Project project, final @NotNull VirtualFile baseDir, final @NotNull SymfonyInstallerSettings settings, @NotNull Module module) {
        String version = settings.getVersion().getVersion();

        if (version.equals("latest") || version.equals("demo") || version.equals("website")) {
            generateViaComposer(project, baseDir, settings, module);
            return;
        }

        int b = 0;
        try {
            b = Integer.valueOf(version.substring(0, 1));
        } catch (NumberFormatException ignored) {
        }

        if (b > 0 && b < 3) {
            generateViaInstaller(project, baseDir, settings, module);
        } else {
            generateViaComposer(project, baseDir, settings, module);
        }
    }

    private void generateViaComposer(@NotNull final Project project, final @NotNull VirtualFile baseDir, final @NotNull SymfonyInstallerSettings settings, @NotNull Module module) {
        final File baseDirFile = new File(baseDir.getPath());
        final File tempFile = FileUtil.findSequentNonexistentFile(baseDirFile, "symfony", "");

        String composerPath;
        File symfonyInProject = null;
        if (settings.isDownload()) {

            VirtualFile file = SymfonyInstallerUtil.downloadComposer(project, null, tempFile.getPath());
            if (file == null)  {
                showErrorNotification(project, "Cannot download composer.phar file");
                Symfony2ProjectComponent.getLogger().warn("Cannot download composer.phar file");
                return;
            }

            composerPath = file.getPath();
            symfonyInProject = tempFile;
        } else {
            composerPath = settings.getExistingPath();
        }

        String[] commands = SymfonyInstallerUtil.getCreateComposerSymfonyProjectCommand(settings.getVersion(), composerPath, baseDir.getPath(), settings.getPhpInterpreter());

        new SymfonyInstallerCommandExecutor(settings, symfonyInProject, project, baseDir, commands).execute();
    }

    private void generateViaInstaller(@NotNull final Project project, final @NotNull VirtualFile baseDir, final @NotNull SymfonyInstallerSettings settings, @NotNull Module module) {
        final File baseDirFile = new File(baseDir.getPath());
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

        String[] commands = SymfonyInstallerUtil.getCreateProjectCommand(settings.getVersion(), composerPath, baseDir.getPath(), settings.getPhpInterpreter(), null);

        new SymfonyInstallerCommandExecutor(settings, symfonyInProject, project, baseDir, commands).execute();
    }

    static void showErrorNotification(@NotNull Project project, @NotNull String content)
    {
        Notifications.Bus.notify(new Notification(SymfonyInstallerUtil.INSTALLER_GROUP_DISPLAY_ID, "Symfony-Installer", content, NotificationType.ERROR, null), project);
    }

    static void showInfoNotification(@NotNull Project project, @NotNull String content)
    {
        Notifications.Bus.notify(new Notification(SymfonyInstallerUtil.INSTALLER_GROUP_DISPLAY_ID, "Symfony-Installer", content, NotificationType.INFORMATION, null), project);
    }

    @NotNull
    @Override
    public GeneratorPeer<SymfonyInstallerSettings> createPeer() {
        return new SymfonyInstallerGeneratorPeer();
    }

    public boolean isPrimaryGenerator()
    {
        return PlatformUtils.isPhpStorm();
    }

    @Override
    public Icon getLogo() {
        return Symfony2Icons.SYMFONY;
    }
}
