package fr.adrienbrault.idea.symfony2plugin.integrations.database.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import fr.adrienbrault.idea.symfony2plugin.integrations.database.DatabaseConnectionConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog for selecting which discovered Symfony database connections to add.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AddFoundDatabaseConnectionDialog extends DialogWrapper {

    private final List<DatabaseConnectionConfig> connections;
    private final Map<DatabaseConnectionConfig, JBCheckBox> checkboxes = new LinkedHashMap<>();

    public AddFoundDatabaseConnectionDialog(@NotNull Project project,
                                            @NotNull List<DatabaseConnectionConfig> connections) {
        super(project);
        this.connections = connections;
        setTitle("Add Symfony Database Connections");
        setOKButtonText("Add Connection(s)");
        init();
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(JBUI.Borders.empty(10));

        JBLabel header = new JBLabel("Select database connections to add from Symfony configuration:");
        header.setBorder(JBUI.Borders.emptyBottom(8));
        panel.add(header, BorderLayout.NORTH);

        JPanel connectionsPanel = new JPanel();
        connectionsPanel.setLayout(new BoxLayout(connectionsPanel, BoxLayout.Y_AXIS));
        connectionsPanel.setBorder(JBUI.Borders.empty(4));

        for (DatabaseConnectionConfig config : connections) {
            JBCheckBox checkBox = new JBCheckBox(formatConnectionLabel(config), true);
            checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
            checkboxes.put(config, checkBox);
            connectionsPanel.add(checkBox);
            connectionsPanel.add(Box.createVerticalStrut(2));
        }

        JBScrollPane scrollPane = new JBScrollPane(connectionsPanel);
        scrollPane.setPreferredSize(new Dimension(520, Math.min(connections.size() * 36 + 20, 240)));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    @NotNull
    private String formatConnectionLabel(@NotNull DatabaseConnectionConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append(config.getName()).append(" — ").append(config.getDriver()).append("://");

        if (config.getHost() != null) {
            sb.append(config.getHost());
            if (config.getPort() != null) {
                sb.append(":").append(config.getPort());
            }
        }

        if (config.getDatabase() != null) {
            sb.append("/").append(config.getDatabase());
        }

        if (config.getUsername() != null) {
            sb.append(" (user: ").append(config.getUsername()).append(")");
        }

        return sb.toString();
    }

    @NotNull
    public List<DatabaseConnectionConfig> getSelectedConnections() {
        List<DatabaseConnectionConfig> selected = new ArrayList<>();
        for (Map.Entry<DatabaseConnectionConfig, JBCheckBox> entry : checkboxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selected.add(entry.getKey());
            }
        }
        return selected;
    }
}
