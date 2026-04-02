package fr.adrienbrault.idea.symfony2plugin;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.extension.PluginConfigurationExtension;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class Symfony2ProjectComponent {
    public static class PostStartupActivity implements ProjectActivity {
        @Nullable
        @Override
        public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
            if (isEnabled(project)) {
                SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(project);
            }

            if (!ApplicationManager.getApplication().isUnitTestMode() && !ApplicationManager.getApplication().isHeadlessEnvironment()) {
                checkProject(project);
            }

            return Unit.INSTANCE;
        }
    }

    final private static Logger LOG = Logger.getInstance("Symfony-Plugin");
    public static final ExtensionPointName<PluginConfigurationExtension> PLUGIN_CONFIGURATION_EXTENSION = new ExtensionPointName<>("fr.adrienbrault.idea.symfony2plugin.extension.PluginConfigurationExtension");

    public static Logger getLogger() {
        return LOG;
    }

    public static Collection<File> getContainerFiles(@NotNull Project project) {
        Collection<ContainerFile> containerFiles = new ArrayList<>();

        List<ContainerFile> settingsContainerFiles = Settings.getInstance(project).containerFiles;
        if (settingsContainerFiles != null) {
            containerFiles.addAll(settingsContainerFiles.stream()
                .filter(containerFile -> containerFile.getPath() != null)
                .toList());
        }

        if(containerFiles.isEmpty()) {
            for (String s : ServiceContainerUtil.getContainerFiles(project)) {
                containerFiles.add(new ContainerFile(s));
            }
        }

        Collection<File> validFiles = new ArrayList<>();
        for(ContainerFile containerFile : containerFiles) {
            if(containerFile.exists(project)) {
                validFiles.add(containerFile.getFile(project));
            }
        }

        return validFiles;
    }

    private static void checkProject(@NotNull Project project) {
        if(!isEnabled(project)
            && !Settings.getInstance(project).dismissEnableNotification
            && VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "vendor", "symfony") != null
            ) {

            IdeHelper.notifyEnableMessage(project);
        }
    }

    public static boolean isEnabled(@Nullable Project project) {
        return project != null && Settings.getInstance(project).pluginEnabled;
    }

    /**
     * If plugin is not enabled on first project start/indexing we will never get a filled
     * index until a forced cache rebuild, we check also for vendor path
     */
    public static boolean isEnabledForIndex(Project project) {

        if(Settings.getInstance(project).pluginEnabled) {
            return true;
        }

        if(VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "vendor", "symfony") != null) {
            return true;
        }

        // drupal8; this should not really here
        if(VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "core", "vendor", "symfony") != null) {
            return true;
        }

        return false;
    }

    public static boolean isEnabled(@Nullable PsiElement psiElement) {
        return psiElement != null && isEnabled(psiElement.getProject());
    }
}
