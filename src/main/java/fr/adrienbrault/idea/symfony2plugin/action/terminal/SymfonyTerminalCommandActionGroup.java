package fr.adrienbrault.idea.symfony2plugin.action.terminal;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Action group that provides Symfony console commands for the terminal toolbar.
 * Displays available Symfony commands in a dropdown menu and injects selected commands into the terminal.
 */
public class SymfonyTerminalCommandActionGroup extends ActionGroup implements DumbAware {

    public SymfonyTerminalCommandActionGroup() {
        super("Symfony Commands", true);
        setPopup(true);
    }

    @Override
    @NotNull
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) {
            return AnAction.EMPTY_ARRAY;
        }

        Project project = e.getProject();
        if (project == null || !Symfony2ProjectComponent.isEnabled(project)) {
            return AnAction.EMPTY_ARRAY;
        }

        Collection<SymfonyCommand> commands = SymfonyCommandUtil.getCommands(project);
        if (commands.isEmpty()) {
            return AnAction.EMPTY_ARRAY;
        }

        // Group commands by namespace (e.g., "cache:", "doctrine:", etc.)
        // Filter out commands with empty names to prevent "Empty menu item text" error
        Map<String, List<SymfonyCommand>> groupedCommands = commands.stream()
            .filter(command -> !command.getName().isEmpty())
            .collect(Collectors.groupingBy(
                command -> {
                    String name = command.getName();
                    int colonIndex = name.indexOf(':');
                    if (colonIndex > 0) {
                        return name.substring(0, colonIndex + 1);
                    }
                    return "_root"; // Commands without namespace
                }
            ));

        List<AnAction> actions = new ArrayList<>();

        // Add namespaced commands as subgroups
        for (Map.Entry<String, List<SymfonyCommand>> entry : groupedCommands.entrySet()) {
            String namespace = entry.getKey();
            List<SymfonyCommand> namespaceCommands = entry.getValue();

            if ("_root".equals(namespace)) {
                // Add root-level commands directly
                for (SymfonyCommand command : namespaceCommands) {
                    actions.add(new SymfonyCommandInjectAction(command));
                }
            } else {
                // Add namespace as a subgroup
                actions.add(new SymfonyCommandNamespaceGroup(namespace, namespaceCommands));
            }
        }

        return actions.toArray(new AnAction[0]);
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
