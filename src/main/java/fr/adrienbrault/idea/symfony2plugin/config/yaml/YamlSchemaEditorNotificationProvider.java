package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import icons.SymfonyIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Shows a notification banner at the top of YAML service configuration files
 * suggesting to add the new Symfony 7.4 JSON schema hint for better autocompletion.

 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlSchemaEditorNotificationProvider implements EditorNotificationProvider, DumbAware {
    private static final String SCHEMA_COMMENT_PREFIX = "# yaml-language-server: $schema=";
    private static final String SCHEMA_COMMENT_MARKER = "yaml-language-server";
    private static final String SCHEMA_FILENAME = "services.schema.json";

    @Override
    public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
        // Only process if Symfony plugin is enabled
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return null;
        }

        // Check if user has dismissed this notification
        if (Settings.getInstance(project).dismissYamlSchemaNotification) {
            return null;
        }

        // Only process YAML files
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (!(psiFile instanceof YAMLFile)) {
            return null;
        }

        // Check if this looks like a Symfony service configuration file
        if (!isServiceConfigurationFile((YAMLFile) psiFile)) {
            return null;
        }

        // Check if schema hint is already present by searching for PsiComment nodes
        if (hasYamlLanguageServerComment(psiFile)) {
            return null;
        }

        // Only show notification if schema file exists in the project
        if (!hasSchemaPath(project)) {
            return null;
        }

        // Return a function that creates the notification panel
        return fileEditor -> createNotificationPanel(project, psiFile);
    }

    /**
     * Checks if the file has a yaml-language-server comment anywhere in the file
     */
    private boolean hasYamlLanguageServerComment(@NotNull PsiFile psiFile) {
        // Use PsiTreeUtil to find all PsiComment elements at any level
        Collection<PsiComment> comments = PsiTreeUtil.findChildrenOfType(psiFile, PsiComment.class);
        for (PsiComment comment : comments) {
            String commentText = comment.getText();
            if (commentText != null && commentText.contains(SCHEMA_COMMENT_MARKER)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given YAML file is a Symfony service configuration file
     * by checking for "services" or "parameters" root keys
     */
    private boolean isServiceConfigurationFile(@NotNull YAMLFile yamlFile) {
        // First check filename patterns for quick filtering
        VirtualFile virtualFile = yamlFile.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }

        String path = virtualFile.getPath();
        String name = virtualFile.getName();

        // docker-compose.yml is also having "services" in root: ignore it
        if (name.contains("-compose")) {
            return false;
        }

        // Quick filename check
        boolean isLikelyServiceFile = name.equals("services.yaml")
            || name.equals("services.yml")
            || path.contains("/config/services/")
            || path.contains("/config/packages/")
            || (path.contains("/config/") && name.contains("service"));

        if (isLikelyServiceFile) {
            return true;
        }

        // Content validation: check for "services" or "parameters" root keys
        List<YAMLDocument> documents = yamlFile.getDocuments();
        if (documents.isEmpty()) {
            return false;
        }

        YAMLDocument first = documents.getFirst();
        if (first != null && first.getTopLevelValue() instanceof YAMLMapping mapping) {
            for (YAMLKeyValue keyValue : mapping.getKeyValues()) {
                String keyText = keyValue.getKeyText();
                if ("services".equals(keyText) || "parameters".equals(keyText) || "imports".equals(keyText)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Creates the notification panel with action buttons
     */
    private @NotNull EditorNotificationPanel createNotificationPanel(@NotNull Project project, @NotNull PsiFile psiFile) {
        EditorNotificationPanel panel = new EditorNotificationPanel(EditorNotificationPanel.Status.Info);
        panel.icon(SymfonyIcons.Symfony);

        panel.setText("Add YAML schema hint for better Symfony service autocompletion?");

        // "Add Schema" button
        panel.createActionLabel("Add Schema Hint", () -> {
            String schemaPath = findSchemaPath(project, psiFile);
            if (schemaPath != null) {
                addSchemaHint(psiFile, schemaPath);
                // Update the editors to remove the notification
                com.intellij.ui.EditorNotifications.getInstance(project).updateAllNotifications();
            }
        });

        // "Ignore" button
        panel.createActionLabel("Don't Show Again", () -> {
            Settings.getInstance(project).dismissYamlSchemaNotification = true;
            // Update the editors to remove the notification
            com.intellij.ui.EditorNotifications.getInstance(project).updateAllNotifications();
        });

        return panel;
    }

    private boolean hasSchemaPath(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            () -> {
                Collection<VirtualFile> schemaFiles = FilenameIndex.getVirtualFilesByName(
                    SCHEMA_FILENAME,
                    GlobalSearchScope.allScope(project)
                );

                return CachedValueProvider.Result.create(!schemaFiles.isEmpty(), VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS);
            }
        );
    }

    /**
     * Finds the schema file and returns the relative path from the current file.
     * Prioritizes vendor directory files.
     */
    private @Nullable String findSchemaPath(@NotNull Project project, @NotNull PsiFile currentVirtualFile) {
        VirtualFile virtualFile = currentVirtualFile.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        Collection<VirtualFile> schemaFiles = FilenameIndex.getVirtualFilesByName(
            SCHEMA_FILENAME,
            GlobalSearchScope.allScope(project)
        );

        if (schemaFiles.isEmpty()) {
            return null;
        }

        // Filter and sort schema files: only use files in Loader/schema/, prioritize vendor directory
        List<VirtualFile> sortedFiles = schemaFiles.stream()
            .filter(file -> file.getPath().contains("/Loader/schema/"))
            .sorted(Comparator.comparing((VirtualFile file) -> {
                String path = file.getPath();
                // Vendor files get priority (lower number = higher priority)
                if (path.contains("/vendor/symfony/dependency-injection/")) {
                    return 0;
                } else if (path.contains("/vendor/")) {
                    return 1;
                }
                return 2;
            }))
            .toList();

        if (sortedFiles.isEmpty()) {
            return null;
        }

        for (VirtualFile sortedFile : sortedFiles) {
            String s = calculateRelativePath(virtualFile, sortedFile);
            if (s != null) {
                return s;
            }
        }

        return null;
    }


    /**
     * Calculates the relative path from the current file to the schema file
     */
    private @Nullable String calculateRelativePath(@NotNull VirtualFile from, @NotNull VirtualFile to) {
        try {
            Path fromPath = Paths.get(from.getParent().getPath());
            Path toPath = Paths.get(to.getPath());
            Path relativePath = fromPath.relativize(toPath);
            return relativePath.toString().replace('\\', '/');
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Adds the schema hint comment to the top of the file
     */
    private void addSchemaHint(@NotNull PsiFile psiFile, @NotNull String schemaPath) {
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(psiFile.getProject(), () -> {
            com.intellij.openapi.editor.Document document =
                com.intellij.psi.PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);

            if (document != null) {
                String schemaComment = SCHEMA_COMMENT_PREFIX + schemaPath;
                String newContent = schemaComment + "\n" + document.getText();
                document.setText(newContent);
            }
        });
    }
}
