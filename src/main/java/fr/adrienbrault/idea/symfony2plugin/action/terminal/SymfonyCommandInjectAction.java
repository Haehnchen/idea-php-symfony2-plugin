package fr.adrienbrault.idea.symfony2plugin.action.terminal;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.ui.TerminalWidget;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Action that injects a Symfony console command into the active terminal.
 */
public class SymfonyCommandInjectAction extends AnAction implements DumbAware {

    @NotNull
    private final SymfonyCommand command;

    public SymfonyCommandInjectAction(@NotNull SymfonyCommand command) {
        super(command.getName(), command.getFqn(), Symfony2Icons.SYMFONY);
        this.command = command;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        TerminalToolWindowManager manager = TerminalToolWindowManager.getInstance(project);
        if (manager == null) {
            return;
        }

        ContentManager contentManager = manager.getToolWindow().getContentManager();
        if (contentManager == null) {
            return;
        }

        Content content = contentManager.getSelectedContent();
        if (content == null) {
            return;
        }

        TerminalWidget widget = TerminalWidgetFinder.findWidgetByContent(content);
        if (widget == null) {
            return;
        }

        // Build the command to inject: bin/console <command>
        String commandToInject = "bin/console " + command.getName();

        // Try modern terminal first (JBTerminalWidget with TtyConnector)
        if (widget instanceof JBTerminalWidget jbTerminalWidget) {
            try {
                var ttyConnector = jbTerminalWidget.getTtyConnector();
                if (ttyConnector != null) {
                    ttyConnector.write(commandToInject + "\n");
                    return;
                }
            } catch (IOException ignored) {
                // Fall through to reflection-based approach
            }
        }

        // Fallback: use reflection to support both old and new terminal implementations
        try {
            injectCommandViaReflection(widget, commandToInject);
        } catch (Exception ex) {
            Messages.showErrorDialog(
                project,
                "Failed to inject command into terminal: " + ex.getMessage(),
                "Symfony Command Injection Error"
            );
        }
    }

    /**
     * Injects a command into the terminal using reflection to maintain compatibility
     * with different terminal implementations.
     */
    private void injectCommandViaReflection(@NotNull TerminalWidget widget, @NotNull String command) throws Exception {
        // Split command into lines for multi-line injection support
        String[] lines = command.split("\n");
        List<String> nonEmptyLines = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) {
                nonEmptyLines.add(line);
            }
        }

        // Try to find sendText method (for new terminal)
        Method sendTextMethod = findMethod(widget.getClass(), "sendText");
        if (sendTextMethod != null) {
            Object builder = sendTextMethod.invoke(widget);
            if (builder != null) {
                // Find the send(String) method on the builder
                Method sendMethod = findMethod(builder.getClass(), "send", String.class);
                if (sendMethod != null) {
                    for (String line : nonEmptyLines) {
                        sendMethod.invoke(builder, line);
                    }
                    return;
                }
            }
        }

        // Fallback to sendCommandToExecute (for older terminal)
        Method sendCommandMethod = widget.getClass().getMethod("sendCommandToExecute", String.class);
        for (String line : nonEmptyLines) {
            sendCommandMethod.invoke(widget, line);
        }
    }

    @Nullable
    private Method findMethod(@NotNull Class<?> clazz, @NotNull String methodName, @Nullable Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    @Override
    @NotNull
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
