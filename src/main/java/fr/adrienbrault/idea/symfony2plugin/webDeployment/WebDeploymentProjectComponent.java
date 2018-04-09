package fr.adrienbrault.idea.symfony2plugin.webDeployment;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class WebDeploymentProjectComponent implements ProjectComponent {

    private Project project;

    public WebDeploymentProjectComponent(Project project) {
        this.project = project;
    }

    public void initComponent() {
    }

    public void disposeComponent() {
    }

    @NotNull
    public String getComponentName() {
        return "WebDeploymentProjectComponent";
    }

    public void projectOpened() {
        if(!WebDeploymentUtil.isEnabled(project)) {
            return;
        }

        // remote file downloader
        if(Settings.getInstance(project).remoteDevFileScheduler) {
            Symfony2ProjectComponent.getLogger().info("Starting Symfony webDeployment background scheduler");

            DumbService.getInstance(project).smartInvokeLater(() -> new Timer().schedule(new MyTimerTask(), 1000, 300000));
        }
    }

    public void projectClosed() {
        if(RemoteWebServerUtil.STORAGE_INSTANCES.containsKey(project)) {
            RemoteWebServerUtil.STORAGE_INSTANCES.remove(project);
        }
    }

    private class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            if(!RemoteWebServerUtil.hasConfiguredRemoteFile(project)) {
                return;
            }

            DumbService.getInstance(project).smartInvokeLater(() -> new Task.Backgroundable(project, "Symfony: Remote File Download", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    Symfony2ProjectComponent.getLogger().info("Running background webDeployment dev download");
                    RemoteWebServerUtil.collectRemoteFiles(project);
                }
            }.queue());
        }
    }
}
