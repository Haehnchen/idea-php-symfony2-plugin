package fr.adrienbrault.idea.symfony2plugin.webDeployment;

import com.intellij.openapi.Disposable;
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
public class WebDeploymentProjectComponent {
    public static class ProjectService implements Disposable {
        private final Project project;
        private Timer timer1;

        public ProjectService(@NotNull Project project) {
            this.project = project;
        }

        public void start() {
            // remote file downloader
            if(Settings.getInstance(project).remoteDevFileScheduler) {
                Symfony2ProjectComponent.getLogger().info("Starting Symfony webDeployment background scheduler");

                this.timer1 = new Timer();
                DumbService.getInstance(this.project).smartInvokeLater(() -> timer1.schedule(new MyTimerTask(project), 1000, 300000));
            }
        }

        @Override
        public void dispose() {
            RemoteWebServerUtil.STORAGE_INSTANCES.remove(project);

            if (this.timer1 != null) {
                this.timer1.cancel();
                this.timer1.purge();
                this.timer1 = null;
            }
        }
    }

    private static class MyTimerTask extends TimerTask {
        @NotNull
        private final Project project;

        public MyTimerTask(@NotNull Project project) {
            this.project = project;
        }

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
