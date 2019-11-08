package fr.adrienbrault.idea.symfonyplugin.webDeployment.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.jetbrains.plugins.webDeployment.actions.WebDeploymentDataKeys;
import com.jetbrains.plugins.webDeployment.config.Deployable;
import com.jetbrains.plugins.webDeployment.config.PublishConfig;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.webDeployment.utils.RemoteWebServerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyWebDeploymentDownloadAction extends DumbAwareAction {

    public SymfonyWebDeploymentDownloadAction() {
        super("Download dev files", "Download Symfony files from dev folder", Symfony2Icons.SYMFONY);
    }

    public void update(AnActionEvent e) {
        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (project == null || project.isDisposed() || project.isDefault()) {
            e.getPresentation().setVisible(false);
            return;
        }

        Deployable server = WebDeploymentDataKeys.DEPLOYABLE.getData(e.getDataContext());
        if(server == null || !PublishConfig.getInstance(project).isDefault(server) || !server.needsTransfer() || server.validateFast() != null) {
            e.getPresentation().setVisible(false);
            return;
        }

        e.getPresentation().setVisible(RemoteWebServerUtil.hasConfiguredRemoteFile(project));
    }

    public void actionPerformed(AnActionEvent e) {
        final Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (project == null) {
            return;
        }

        Deployable server = WebDeploymentDataKeys.DEPLOYABLE.getData(e.getDataContext());
        if(server == null || !PublishConfig.getInstance(project).isDefault(server)) {
            return;
        }

        new Task.Backgroundable(project, "Symfony: Downloading Files", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                Symfony2ProjectComponent.getLogger().info("Running webDeployment dev download");
                RemoteWebServerUtil.collectRemoteFiles(project);
            }
        }.queue();
    }
}
