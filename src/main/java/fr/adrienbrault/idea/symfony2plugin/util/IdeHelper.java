package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.ui.UIUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.SettingsForm;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IdeHelper {

    public static void openUrl(String url) {
        if(java.awt.Desktop.isDesktopSupported() ) {
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

            if(desktop.isSupported(java.awt.Desktop.Action.BROWSE) ) {
                try {
                    java.net.URI uri = new java.net.URI(url);
                    desktop.browse(uri);
                } catch (URISyntaxException | IOException ignored) {
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

    public static void notifyEnableMessage(final Project project) {

        Notification notification = new Notification("Symfony Support", "Symfony", "Enable the Symfony Plugin <a href=\"enable\">with auto configuration now</a>, open <a href=\"config\">Project Settings</a> or <a href=\"dismiss\">dismiss</a> further messages", NotificationType.INFORMATION, (notification1, event) -> {

            // handle html click events
            if("config".equals(event.getDescription())) {

                // open settings dialog and show panel
                SettingsForm.show(project);
            } else if("enable".equals(event.getDescription())) {
                Collection<String> messages = enablePluginAndConfigure(project);

                String message = "Plugin enabled";

                if (!messages.isEmpty()) {
                    List<String> collect = messages.stream().map(s -> "<br> - " + s).collect(Collectors.toList());
                    message += StringUtils.join(collect, "");
                }

                Notifications.Bus.notify(new Notification("Symfony Support", "Symfony", message, NotificationType.INFORMATION), project);
            } else if("dismiss".equals(event.getDescription())) {

                // user doesnt want to show notification again
                Settings.getInstance(project).dismissEnableNotification = true;
            }

            notification1.expire();
        });

        Notifications.Bus.notify(notification, project);
    }

    public static Collection<String> enablePluginAndConfigure(@NotNull Project project) {
        Settings.getInstance(project).pluginEnabled = true;

        Collection<String> messages = new ArrayList<>();

        /* Remove version info; prevent index issues
        Set<String> versions = SymfonyUtil.getVersions(project);
        if (!versions.isEmpty()) {
            messages.add("Symfony Version: " + versions.iterator().next());
        }
        */

        // Symfony 3.0 structure
        if (VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "var", "cache") != null) {
            Settings.getInstance(project).pathToTranslation = "var/cache/dev/translations";
            messages.add("Translations: var/cache/dev/translations");
        }

        // Symfony 4.0 structure
        if (VfsUtil.findRelativeFile(ProjectUtil.getProjectDir(project), "public") != null) {
            Settings.getInstance(project).directoryToWeb = "public";
            messages.add("Web Directory: public");
        }

        // There no clean version when "FooBar:Foo:foo.html.twig" was dropped or deprecated
        // So we disable it in the 4 branch by default; following with a default switch to "false" soon
        /* Remove version info; prevent index issues
        if (SymfonyUtil.isVersionGreaterThenEquals(project, "4.0")) {
            Settings.getInstance(project).twigBundleNamespaceSupport = false;
            messages.add("Twig: Bundle names disabled");
        }
        */

        return messages;
    }

    public static void navigateToPsiElement(@NotNull PsiElement psiElement) {
        final Navigatable descriptor = PsiNavigationSupport.getInstance().getDescriptor(psiElement);
        if (descriptor != null) {
            descriptor.navigate(true);
        }
    }

    /**
     * Find a window manager
     *
     * @see com.intellij.ui.popup.AbstractPopup
     */
    @Nullable
    private static WindowManagerEx getWindowManager() {
        return ApplicationManagerEx.getApplicationEx() != null ? WindowManagerEx.getInstanceEx() : null;
    }

    /**
     * Find current window element of given project.
     * Use this to find a component for new dialogs without using JBPopupFactory
     *
     * @see com.intellij.ui.popup.AbstractPopup#showCenteredInCurrentWindow
     */
    @Nullable
    public static Window getWindowComponentFromProject(@NotNull Project project) {
        WindowManagerEx windowManager = getWindowManager();
        if(windowManager == null) {
            return null;
        }

        Window window = null;

        Component focusedComponent = windowManager.getFocusedComponent(project);
        if (focusedComponent != null) {
            Component parent = UIUtil.findUltimateParent(focusedComponent);
            if (parent instanceof Window) {
                window = (Window)parent;
            }
        }

        if (window == null) {
            window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        }

        if (window != null && window.isShowing()) {
            return window;
        }

        return window;
    }
}
