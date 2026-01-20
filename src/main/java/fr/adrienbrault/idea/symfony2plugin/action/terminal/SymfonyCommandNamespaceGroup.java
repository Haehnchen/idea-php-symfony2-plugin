package fr.adrienbrault.idea.symfony2plugin.action.terminal;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Action group for a Symfony command namespace (e.g., "cache:", "doctrine:").
 * Creates a submenu with all commands belonging to a specific namespace.
 */
public class SymfonyCommandNamespaceGroup extends ActionGroup implements DumbAware {

    @NotNull
    private final List<SymfonyCommand> commands;

    public SymfonyCommandNamespaceGroup(@NotNull String namespace, @NotNull List<SymfonyCommand> commands) {
        super(formatNamespaceTitle(namespace), true);
        setPopup(true);
        this.commands = commands;
    }

    @Override
    @NotNull
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        for (SymfonyCommand command : commands) {
            actions.add(new SymfonyCommandInjectAction(command));
        }
        return actions.toArray(new AnAction[0]);
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @NotNull
    private static String formatNamespaceTitle(@NotNull String namespace) {
        // Remove trailing colon for display
        String title = namespace;
        if (title.endsWith(":")) {
            title = title.substring(0, title.length() - 1);
        }
        // Ensure we never return an empty string to prevent "Empty menu item text" error
        return title.isEmpty() ? "Commands" : title;
    }
}
