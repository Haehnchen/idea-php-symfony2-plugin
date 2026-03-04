package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.ObjectUtils;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandTestRunLineMarkerProvider extends RunLineMarkerContributor {
    @Override
    public @Nullable Info getInfo(@NotNull PsiElement leaf) {
        PhpClass phpClass = getCommandContext(leaf);
        if (phpClass == null) {
            return null;
        }

        List<String> commandNames = SymfonyCommandUtil.getCommandNameFromClass(phpClass);
        if (commandNames.isEmpty()) {
            return null;
        }

        String commandName = commandNames.get(0);
        List<AnAction> actions = new ArrayList<>();

        Collections.addAll(actions, ExecutorAction.getActions());

        Project project = leaf.getProject();
        VirtualFile projectDir = ProjectUtil.getProjectDir(project);

        if (projectDir.findFileByRelativePath("bin/console") != null) {
            actions.add(new SymfonyCommandRunAction(
                SymfonyCommandRunConfiguration.ExecutionMode.PHP_INTERPRETER,
                "Run '" + commandName + "' with bin/console",
                AllIcons.RunConfigurations.TestState.Run
            ));
        }

        if (isSymfonyCliAvailable()) {
            actions.add(new SymfonyCommandRunAction(
                SymfonyCommandRunConfiguration.ExecutionMode.SYMFONY_CLI,
                "Run '" + commandName + "' with Symfony CLI",
                AllIcons.RunConfigurations.TestState.Run
            ));
        }

        return new Info(
            AllIcons.RunConfigurations.TestState.Run,
            actions.toArray(AnAction.EMPTY_ARRAY),
            psiElement -> "Run Symfony Command"
        );
    }

    @Nullable
    public static PhpClass getCommandContext(@NotNull PsiElement leaf) {
        if (PhpPsiUtil.isOfType(leaf, PhpTokenTypes.IDENTIFIER)) {
            PhpNamedElement element = ObjectUtils.tryCast(leaf.getParent(), PhpNamedElement.class);
            if (element != null && element.getNameIdentifier() == leaf) {
                if (element instanceof PhpClass) {
                    return (PhpClass) element;
                }
            }
        }

        return null;
    }

    public static boolean isSymfonyCliAvailable() {
        String path = EnvironmentUtil.getValue("PATH");
        if (path == null) {
            return false;
        }

        for (String dir : path.split(File.pathSeparator)) {
            File file = new File(dir, "symfony");
            if (file.isFile() && file.canExecute()) {
                return true;
            }
        }

        return false;
    }
}
