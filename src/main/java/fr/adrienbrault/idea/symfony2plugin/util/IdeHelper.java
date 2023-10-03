package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.preview.IntentionPreviewUtils;
import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.pom.Navigatable;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.ui.UIUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class IdeHelper {

    private static final String DIRECTORY_EXCLUDE_MESSAGE = "Directory '%s' marked as excluded for indexing";

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
        Notification notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Symfony Notifications")
            .createNotification("Detected a Symfony project structure", NotificationType.INFORMATION);

        notification.addAction(new NotificationAction("Enable plugin") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                Project project1 = e.getProject();
                if (project1 == null) {
                    return;
                }

                Collection<String> messages = enablePluginAndConfigure(project1);

                String message = "Plugin enabled for project";

                if (!messages.isEmpty()) {
                    List<String> collect = messages.stream().map(s -> "<br> - " + s).collect(Collectors.toList());
                    message += StringUtils.join(collect, "");
                }

                Notification notification2 = NotificationGroupManager.getInstance()
                    .getNotificationGroup("Symfony Notifications")
                    .createNotification(message, NotificationType.INFORMATION);

                notification2.setTitle(createNotificationTitle(project1));
                notification2.setIcon(Symfony2Icons.SYMFONY);
                notification2.notify(project1);

                notification.expire();
            }
        });

        notification.addAction(new NotificationAction("Don't show again") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                // user doesnt want to show notification again
                Settings.getInstance(project).dismissEnableNotification = true;

                notification.expire();
            }
        });

        notification.setTitle(createNotificationTitle(project));
        notification.setIcon(Symfony2Icons.SYMFONY);

        notification.notify(project);
    }

    private static String createNotificationTitle(@NotNull final Project project) {
        String title = "Symfony Support";

        title += " (Full Version)";

        return title;
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

        // mark "var" as excluded directory to prevent indexing
        Module moduleForFile = ModuleUtilCore.findModuleForFile(ProjectUtil.getProjectDir(project), project);
        if (moduleForFile != null) {
            ModifiableRootModel modifiableRootModel = ModuleRootManager.getInstance(moduleForFile).getModifiableModel();
            boolean needsCommit = false;

            for (ContentEntry contentEntry : modifiableRootModel.getContentEntries()) {
                VirtualFile projectDir = ProjectUtil.getProjectDir(modifiableRootModel.getProject());

                VirtualFile var = VfsUtil.findRelativeFile(projectDir, "var");

                // second check for valid exclude
                if (var != null && (VfsUtil.findRelativeFile(projectDir, "var", "cache") != null || VfsUtil.findRelativeFile(projectDir, "var", "log") != null) && !hasAlreadyAnExclude(contentEntry, projectDir, "var")) {
                    contentEntry.addExcludeFolder(var);
                    needsCommit = true;

                    if (!messages.contains(String.format(DIRECTORY_EXCLUDE_MESSAGE, "var"))) {
                        messages.add(String.format(DIRECTORY_EXCLUDE_MESSAGE, "var"));
                    }
                }

                // encore build path + "var" are Symfony structure root
                VirtualFile publicBuild = VfsUtil.findRelativeFile(projectDir, "public", "build");
                if (publicBuild != null && var != null && !hasAlreadyAnExclude(contentEntry, projectDir, "public/build")) {
                    contentEntry.addExcludeFolder(publicBuild);
                    needsCommit = true;

                    if (!messages.contains(String.format(DIRECTORY_EXCLUDE_MESSAGE, "public/build"))) {
                        messages.add(String.format(DIRECTORY_EXCLUDE_MESSAGE, "public/build"));
                    }
                }

                // assetic bundle + "var" are Symfony structure root
                VirtualFile publicBundles = VfsUtil.findRelativeFile(projectDir, "public", "bundles");
                if (publicBundles != null && var != null && !hasAlreadyAnExclude(contentEntry, projectDir, "public/bundles")) {
                    contentEntry.addExcludeFolder(publicBundles);
                    needsCommit = true;

                    if (!messages.contains(String.format(DIRECTORY_EXCLUDE_MESSAGE, "public/bundles"))) {
                        messages.add(String.format(DIRECTORY_EXCLUDE_MESSAGE, "public/bundles"));
                    }
                }
            }

            if (needsCommit) {
                ApplicationManager.getApplication().runWriteAction(modifiableRootModel::commit);
            }
        }

        return messages;
    }

    private static boolean hasAlreadyAnExclude(@NotNull ContentEntry contentEntry, @NotNull VirtualFile projectDir, @NotNull String path) {
        return Arrays.stream(contentEntry.getExcludeFolders()).anyMatch(excludeFolder -> {
            VirtualFile file = excludeFolder.getFile();
            if (file == null) {
                return false;
            }

            return path.equals(VfsUtil.getRelativePath(file, projectDir, '/'));
        });
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

    public static void showErrorHintIfAvailable(@NotNull Editor editor, @NotNull String text) {
        if (ApplicationManager.getApplication().isHeadlessEnvironment() || IntentionPreviewUtils.isIntentionPreviewActive()) {
            return;
        }

        Runnable runnable = () -> HintManager.getInstance().showErrorHint(editor, text);

        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(runnable);
        } else {
            runnable.run();
        }
    }
}
