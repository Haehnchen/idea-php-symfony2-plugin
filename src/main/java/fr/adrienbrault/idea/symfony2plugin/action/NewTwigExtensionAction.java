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
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PhpBundleFileFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class NewTwigExtensionAction extends AbstractProjectDumbAwareAction {
    public NewTwigExtensionAction() {
        super("TwigExtension", "Create TwigExtension Class", Symfony2Icons.SYMFONY);
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

        // Detect available Twig attributes in the project
        String templateName = detectTemplate(project);

        ApplicationManager.getApplication().runWriteAction(() -> {
            Map<String, String> hashMap = new HashMap<>() {{
                put("class", className);
                put("namespace", strings.get(0));
            }};

            PsiElement commandAttributes = PhpBundleFileFactory.createFile(
                project,
                directory.getVirtualFile(),
                templateName,
                className,
                hashMap
            );

            new OpenFileDescriptor(project, commandAttributes.getContainingFile().getVirtualFile(), 0).navigate(true);
        });
    }

    @NotNull
    private static String detectTemplate(@NotNull Project project) {
        // If attributes are available, use the new attribute-based template
        if (PhpElementsUtil.getClassInterface(project, "\\Twig\\Attribute\\AsTwigFunction") != null) {
            return "twig_extension_function_attribute";
        }

        // Default to traditional template
        return "twig_extension";
    }

    public static class Shortcut extends NewTwigExtensionAction {
        @Override
        public void update(@NotNull AnActionEvent event) {
            Project project = getEventProject(event);
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return;
            }

            this.setStatus(event, NewFileActionUtil.isInGivenDirectoryScope(event, "Twig", "Extension"));
        }
    }
}
