package fr.adrienbrault.idea.symfony2plugin.action.terminal;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import java.awt.Component;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Action that shows Symfony console commands in a scrollable popup with fixed max height.
 */
public class SymfonyTerminalCommandsAction extends AnAction implements DumbAware {

    private static final int MAX_VISIBLE_ROWS = 5;

    public SymfonyTerminalCommandsAction() {
        super("Symfony Commands", "Show Symfony console commands", Symfony2Icons.SYMFONY);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || !Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        ActionGroup actionGroup = createActionGroup(project);
        ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
            null,
            actionGroup,
            e.getDataContext(),
            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
            false,
            null,
            MAX_VISIBLE_ROWS
        );

        Component component = e.getInputEvent() != null ? e.getInputEvent().getComponent() : null;
        if (component != null) {
            popup.showUnderneathOf(component);
        } else {
            popup.showInBestPositionFor(e.getDataContext());
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        boolean enabled = project != null && Symfony2ProjectComponent.isEnabled(project);
        e.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @NotNull
    private ActionGroup createActionGroup(@NotNull Project project) {
        return new DefaultActionGroup() {
            @Override
            public AnAction @NotNull [] getChildren(@org.jetbrains.annotations.Nullable AnActionEvent e) {
                Collection<SymfonyCommand> commands = SymfonyCommandUtil.getCommands(project);
                if (commands.isEmpty()) {
                    return AnAction.EMPTY_ARRAY;
                }

                List<AnAction> actions = new ArrayList<>();
                for (SymfonyCommand command : commands) {
                    if (!command.getName().isEmpty()) {
                        actions.add(new SymfonyCommandInjectAction(command));
                    }
                }

                return actions.toArray(new AnAction[0]);
            }
        };
    }
}
