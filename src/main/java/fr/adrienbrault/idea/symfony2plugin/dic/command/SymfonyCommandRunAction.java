package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandRunAction extends AnAction {

    private final SymfonyCommandRunConfiguration.ExecutionMode myMode;

    public SymfonyCommandRunAction(@NotNull SymfonyCommandRunConfiguration.ExecutionMode mode, @NotNull String text, @NotNull Icon icon) {
        super(text, null, icon);
        this.myMode = mode;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (psiElement == null) {
            PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
            if (psiFile != null) {
                psiElement = psiFile.findElementAt(e.getData(CommonDataKeys.CARET) != null ? e.getData(CommonDataKeys.CARET).getOffset() : 0);
            }
        }

        if (psiElement == null) {
            return;
        }

        PhpClass phpClass;
        if (psiElement instanceof PhpClass) {
            phpClass = (PhpClass) psiElement;
        } else {
            phpClass = SymfonyCommandTestRunLineMarkerProvider.getCommandContext(psiElement);
            if (phpClass == null && psiElement.getParent() instanceof PhpClass) {
                phpClass = (PhpClass) psiElement.getParent();
            }
        }

        if (phpClass == null) {
            return;
        }

        List<String> commandNames = SymfonyCommandUtil.getCommandNameFromClass(phpClass);
        if (commandNames.isEmpty()) {
            return;
        }

        String commandName = commandNames.getFirst();

        RunManager runManager = RunManager.getInstance(project);
        SymfonyCommandRunConfigurationType type = SymfonyCommandRunConfigurationType.getInstance();

        RunnerAndConfigurationSettings settings = runManager.createConfiguration(commandName, type.getFactory());
        SymfonyCommandRunConfiguration configuration = (SymfonyCommandRunConfiguration) settings.getConfiguration();
        configuration.setCommandName(commandName);
        configuration.setExecutionMode(myMode);
        configuration.setName(commandName);

        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);

        try {
            ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings)
                .buildAndExecute();
        } catch (ExecutionException ignored) {
        }
    }
}
