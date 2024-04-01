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

import javax.swing.*;
import java.util.HashMap;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewFormTypeAction extends AbstractProjectDumbAwareAction {
    public NewFormTypeAction() {
        super("Form", "Create FormType Class", Symfony2Icons.SYMFONY);
    }

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

        if (!PhpNameUtil.isValidClassName(className)) {
            JOptionPane.showMessageDialog(null, "Invalid class name");
            return;
        }

        List<String> strings = PhpNamespaceCompositeProvider.INSTANCE.suggestNamespaces(directory);
        if (strings.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No namespace found");
            return;
        }

        ApplicationManager.getApplication().runWriteAction(() -> {
            HashMap<String, String> hashMap = new HashMap<>() {{
                put("class", className);
                put("namespace", strings.get(0));
            }};

            PsiElement commandAttributes = PhpBundleFileFactory.createFile(
                project,
                directory.getVirtualFile(),
                "form_type",
                className,
                hashMap
            );

            new OpenFileDescriptor(project, commandAttributes.getContainingFile().getVirtualFile(), 0).navigate(true);
        });
    }

    public static class Shortcut extends NewFormTypeAction {
        @Override
        public void update(@NotNull AnActionEvent event) {
            Project project = getEventProject(event);
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            this.setStatus(event, NewFileActionUtil.isInGivenDirectoryScope(event, "Form", "Type"));
        }
    }
}
