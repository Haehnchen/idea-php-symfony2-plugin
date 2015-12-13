package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.SettingsForm;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

public class IdeHelper {

    public static void openUrl(String url) {
        if(java.awt.Desktop.isDesktopSupported() ) {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

            if(desktop.isSupported(java.awt.Desktop.Action.BROWSE) ) {
                try {
                    java.net.URI uri = new java.net.URI(url);
                    desktop.browse(uri);
                } catch (URISyntaxException ignored) {
                } catch (IOException ignored) {

                }
            }
        }
    }
    @Nullable
    public static VirtualFile createFile(@NotNull Project project, @NotNull FileType fileType, @Nullable VirtualFile root, @NotNull String fileNameWithPath) {
        return createFile(project, fileType, root, fileNameWithPath, null);
    }

    @Nullable
    public static VirtualFile createFile(@NotNull Project project, @NotNull FileType fileType, @Nullable VirtualFile root, @NotNull String fileNameWithPath, @Nullable String content) {

        if(root == null) {
            return null;
        }

        String[] filenameSplit = fileNameWithPath.split("/");
        String pathString = StringUtils.join(Arrays.copyOf(filenameSplit, filenameSplit.length - 1), "/");

        VirtualFile twigDirectory = VfsUtil.findRelativeFile(root, filenameSplit);
        if(twigDirectory != null) {
            return null;
        }

        VirtualFile targetDir;
        try {
            targetDir = VfsUtil.createDirectoryIfMissing(root, pathString);
        } catch (IOException ignored) {
            return null;
        }

        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        final PsiFile file = factory.createFileFromText(filenameSplit[filenameSplit.length - 1], fileType, content != null ? content : "");
        CodeStyleManager.getInstance(project).reformat(file);
        PsiDirectory directory = PsiManager.getInstance(project).findDirectory(targetDir);
        if(directory == null) {
            return null;
        }

        PsiElement add = directory.add(file);
        if(add instanceof PsiFile) {
            return ((PsiFile) add).getVirtualFile();
        }

        return null;
    }

    public static RunnableCreateAndOpenFile getRunnableCreateAndOpenFile(@NotNull Project project, @NotNull FileType fileType, @NotNull VirtualFile rootVirtualFile, @NotNull String fileName) {
        return new RunnableCreateAndOpenFile(project, fileType, rootVirtualFile, fileName);
    }

    public static class RunnableCreateAndOpenFile implements Runnable {

        @NotNull
        private final FileType fileType;
        private final VirtualFile rootVirtualFile;
        private final String fileName;
        private final Project project;
        private String content;

        public RunnableCreateAndOpenFile(@NotNull Project project, @NotNull FileType fileType, @NotNull VirtualFile rootVirtualFile, @NotNull String fileName) {
            this.project = project;
            this.fileType = fileType;
            this.rootVirtualFile = rootVirtualFile;
            this.fileName = fileName;
        }

        public RunnableCreateAndOpenFile setContent(@Nullable String content) {
            this.content = content;
            return this;
        }

        @Override
        public void run() {
            VirtualFile virtualFile = createFile(project, fileType, rootVirtualFile, fileName, this.content);
            if(virtualFile != null) {
                new OpenFileDescriptor(project, virtualFile, 0).navigate(true);
            }
        }
    }

    /**
     * pre phpstorm 7.1 dont support getStatusBar in this way
     */
    public static boolean supportsStatusBar() {
        try {
            WindowManager.getInstance().getClass().getMethod("getStatusBar", Project.class);
            StatusBar.class.getMethod("getWidget", String.class);
            
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public static void notifyEnableMessage(final Project project) {

        Notification notification = new Notification("Symfony Plugin", "Symfony Plugin", "Enable the Symfony Plugin <a href=\"enable\">with auto configuration now</a>, open <a href=\"config\">Project Settings</a> or <a href=\"dismiss\">dismiss</a> further messages", NotificationType.INFORMATION, new NotificationListener() {
            @Override
            public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {

                // handle html click events
                if("config".equals(event.getDescription())) {

                    // open settings dialog and show panel
                    SettingsForm.show(project);
                } else if("enable".equals(event.getDescription())) {
                    enablePluginAndConfigure(project);
                    Notifications.Bus.notify(new Notification("Symfony Plugin", "Symfony Plugin", "Plugin enabled", NotificationType.INFORMATION), project);
                } else if("dismiss".equals(event.getDescription())) {

                    // use dont want to show notification again
                    Settings.getInstance(project).dismissEnableNotification = true;
                }

                notification.expire();
            }

        });

        Notifications.Bus.notify(notification, project);
    }

    public static void enablePluginAndConfigure(@NotNull Project project) {
        Settings.getInstance(project).pluginEnabled = true;

        // Symfony 3.0 structure
        if(VfsUtil.findRelativeFile(project.getBaseDir(), "var", "cache") == null) {
            Settings.getInstance(project).pathToUrlGenerator = "var/cache/dev/appDevUrlGenerator.php";
            Settings.getInstance(project).pathToTranslation = "var/cache/dev/translations";
        }
    }

    public static void navigateToPsiElement(@NotNull PsiElement psiElement) {
        final Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(psiElement);
        if (descriptor != null) {
            descriptor.navigate(true);
        }

        /*
        Project project = psiElement.getProject();
        PsiElement navElement = psiElement.getNavigationElement();
        navElement = TargetElementUtilBase.getInstance().getGotoDeclarationTarget(psiElement, navElement);
        if (navElement instanceof Navigatable) {
            if (((Navigatable)navElement).canNavigate()) {
                ((Navigatable)navElement).navigate(true);
            }
        }  else if (navElement != null) {
            int navOffset = navElement.getTextOffset();
            VirtualFile virtualFile = PsiUtilCore.getVirtualFile(navElement);
            if (virtualFile != null) {
                new OpenFileDescriptor(project, virtualFile, navOffset).navigate(true);
            }
        }*/
    }

}
