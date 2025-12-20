package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;
import com.jetbrains.php.lang.psi.elements.Method;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Generates HTTP Client request files for Symfony routes
 */
public class RouteHttpClientGenerator {

    private static final String HTTP_REQUEST_FILE_EXTENSION = ".http";

    /**
     * Generates an HTTP request file for the given route method
     */
    public static void generateHttpRequest(@NotNull Project project, 
                                          @NotNull Method method, 
                                          @NotNull String requestContent) {
        ApplicationManager.getApplication().invokeLater(() -> {
            WriteCommandAction.runWriteCommandAction(project, "Generate HTTP Request", null, () -> {
                try {
                    // Create or find the HTTP requests directory
                    VirtualFile projectDir = project.getBaseDir();
                    if (projectDir == null) {
                        showError(project, "Could not find project directory");
                        return;
                    }

                    // Create 'http-requests' directory if it doesn't exist
                    VirtualFile httpRequestsDir = projectDir.findChild("http-requests");
                    if (httpRequestsDir == null || !httpRequestsDir.isDirectory()) {
                        httpRequestsDir = projectDir.createChildDirectory(null, "http-requests");
                    }

                    // Generate filename based on method name
                    String fileName = generateFileName(method);
                    
                    // Create the HTTP request file
                    VirtualFile existingFile = httpRequestsDir.findChild(fileName);
                    VirtualFile httpRequestFile;
                    
                    if (existingFile != null) {
                        // Append to existing file
                        appendToFile(project, existingFile, requestContent);
                        httpRequestFile = existingFile;
                    } else {
                        // Create new file
                        httpRequestFile = httpRequestsDir.createChildData(null, fileName);
                        httpRequestFile.setBinaryContent(requestContent.getBytes());
                    }

                    // Open the file in the editor
                    FileEditorManager.getInstance(project).openFile(httpRequestFile, true);
                    
                    // Show success notification
                    com.intellij.notification.Notifications.Bus.notify(
                            new com.intellij.notification.Notification(
                                    "Symfony Plugin",
                                    "HTTP Request Generated",
                                    "Created HTTP request in: " + httpRequestFile.getPath(),
                                    com.intellij.notification.NotificationType.INFORMATION
                            ),
                            project
                    );
                    
                } catch (IOException e) {
                    showError(project, "Failed to create HTTP request file: " + e.getMessage());
                }
            });
        });
    }

    @NotNull
    private static String generateFileName(@NotNull Method method) {
        String className = method.getContainingClass() != null ? 
                method.getContainingClass().getName() : "Controller";
        String methodName = method.getName();
        
        // Generate a readable filename
        return String.format("%s_%s%s", className, methodName, HTTP_REQUEST_FILE_EXTENSION);
    }

    private static void appendToFile(@NotNull Project project, 
                                     @NotNull VirtualFile file, 
                                     @NotNull String content) throws IOException {
        String existingContent = new String(file.contentsToByteArray());
        String newContent = existingContent + "\n" + content;
        file.setBinaryContent(newContent.getBytes());
    }

    private static void showError(@NotNull Project project, @NotNull String message) {
        com.intellij.notification.Notifications.Bus.notify(
                new com.intellij.notification.Notification(
                        "Symfony Plugin",
                        "HTTP Request Generation Failed",
                        message,
                        com.intellij.notification.NotificationType.ERROR
                ),
                project
        );
    }
}
