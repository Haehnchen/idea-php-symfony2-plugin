package fr.adrienbrault.idea.symfony2plugin.webDeployment;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            DumbService.getInstance(project).smartInvokeLater(new Runnable() {
                @Override
                public void run() {
                    new Timer().schedule(new MyTimerTask(), 10000, 300000);
                }
            });
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

            DumbService.getInstance(project).smartInvokeLater(new Runnable() {
                @Override
                public void run() {
                    ApplicationManager.getApplication().runReadAction(new Runnable() {
                        @Override
                        public void run() {
                            RemoteWebServerUtil.collectRemoteFiles(project);
                        }
                    });
                }
            });

        }
    }
}
