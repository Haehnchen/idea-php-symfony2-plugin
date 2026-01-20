package fr.adrienbrault.idea.symfony2plugin.action.terminal;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.terminal.ui.TerminalWidget;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

/**
 * Action that injects a Symfony console command into a new terminal tab.
 */
public class SymfonyCommandInjectAction extends AnAction implements DumbAware {

    @NotNull
    private final SymfonyCommand command;

    public SymfonyCommandInjectAction(@NotNull SymfonyCommand command) {
        super(command.getName(), "Run: bin/console " + command.getName(), Symfony2Icons.SYMFONY);
        this.command = command;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        String commandToExecute = "bin/console " + command.getName();

        TerminalToolWindowManager manager = TerminalToolWindowManager.getInstance(project);
        if (manager == null) {
            return;
        }

        // Create a new shell widget and execute the command
        TerminalWidget widget = manager.createShellWidget(
            project.getBasePath(),
            "Symfony: " + command.getName(),
            true,
            true
        );

        widget.sendCommandToExecute(commandToExecute);
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
