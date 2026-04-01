package fr.adrienbrault.idea.symfony2plugin.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.refactoring.PhpNameUtil;
import com.jetbrains.php.roots.PhpNamespaceCompositeProvider;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
abstract public class AbstractNewPhpClassAction extends AbstractProjectDumbAwareAction {

    public AbstractNewPhpClassAction(String text, String description) {
        super(text, description, Symfony2Icons.SYMFONY);
    }

    /**
     * Returns a suffix to auto-append to the class name (e.g. "Command", "Test").
     * The suffix is appended only when the class name does not already end with it (case-insensitive).
     * Return null to skip suffix handling.
     */
    @Nullable
    protected String getClassNameSuffix() {
        return null;
    }

    /**
     * Returns the file template name to use for code generation.
     *
     * @param project   the current project
     * @param namespace the resolved namespace for the target directory
     * @param directory the target directory
     */
    @NotNull
    protected abstract String getTemplateName(@NotNull Project project, @NotNull String namespace, @NotNull PsiDirectory directory);

    /**
     * Returns additional template variables beyond the default "class" and "namespace" entries.
     *
     * @param className the final (possibly suffix-appended) class name
     * @param namespace the resolved namespace
     * @param directory the target directory
     */
    @NotNull
    protected Map<String, String> getExtraVariables(@NotNull String className, @NotNull String namespace, @NotNull PsiDirectory directory) {
        return Collections.emptyMap();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        this.setStatus(event, false);
        Project project = getEventProject(event);
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        if (NewFileActionUtil.getSelectedDirectoryFromAction(event) != null) {
            this.setStatus(event, true);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        PsiDirectory directory = NewFileActionUtil.getSelectedDirectoryFromAction(event);
        if (directory == null) {
            return;
        }

        Project project = getEventProject(event);
        String className = Messages.showInputDialog(project, "New class name:", "New File", Symfony2Icons.SYMFONY);
        if (StringUtils.isBlank(className)) {
            return;
        }

        String suffix = getClassNameSuffix();
        if (suffix != null && !className.toLowerCase().endsWith(suffix.toLowerCase())) {
            className += suffix;
        }

        if (!PhpNameUtil.isValidClassName(className)) {
            JOptionPane.showMessageDialog(null, "Invalid class name");
            return;
        }

        List<String> namespaces = PhpNamespaceCompositeProvider.INSTANCE.suggestNamespaces(directory);
        if (namespaces.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No namespace found");
            return;
        }

        String finalClassName = className;
        String namespace = namespaces.get(0);
        String templateName = getTemplateName(project, namespace, directory);
        Map<String, String> extraVariables = getExtraVariables(finalClassName, namespace, directory);

        ApplicationManager.getApplication().runWriteAction(() -> {
            HashMap<String, String> vars = new HashMap<>();
            vars.put("class", finalClassName);
            vars.put("namespace", namespace);
            vars.putAll(extraVariables);

            PsiElement element = PhpBundleFileFactory.createFile(
                project,
                directory.getVirtualFile(),
                templateName,
                finalClassName,
                vars
            );

            new OpenFileDescriptor(project, element.getContainingFile().getVirtualFile(), 0).navigate(true);
        });
    }
}
