package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpSettings;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpToolSetting;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Configuration form for MCP (Model Context Protocol) tools
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class McpSettingsForm implements Configurable {

    private JCheckBox mcpEnabledCheckBox;
    private TableView<McpToolSetting> tableView;
    private final Project project;
    private boolean changed = false;
    private ListTableModel<McpToolSetting> modelList;

    // Define all available MCP tools with their metadata
    private static final List<McpToolSetting> DEFAULT_MCP_TOOLS = Arrays.asList(
        new McpToolSetting(
            "list_symfony_routes_controllers",
            "Symfony Routes with Controllers",
            "Lists all routes with their controller mappings and file paths (supports filtering by route name or controller)"
        ),
        new McpToolSetting(
            "match_symfony_url_to_route",
            "Match Symfony URL to Route",
            "Matches a plain request URL to Symfony routes using reverse pattern matching"
        ),
        new McpToolSetting(
            "list_symfony_commands",
            "Symfony Commands",
            "Lists all Symfony console commands available in the project"
        ),
        new McpToolSetting(
            "list_doctrine_entities",
            "Doctrine Entities",
            "Lists all Doctrine ORM entities in the project"
        ),
        new McpToolSetting(
            "list_doctrine_entity_fields",
            "Doctrine Entity Fields",
            "Lists all fields of a specific Doctrine entity including relations"
        ),
        new McpToolSetting(
            "list_twig_extensions",
            "Twig Extensions",
            "Lists all Twig extensions: filters, functions, tests, and tags (supports search and type filtering)"
        ),
        new McpToolSetting(
            "list_profiler_requests",
            "Profiler Requests",
            "Lists the last 10 Symfony profiler requests (supports filtering by URL, hash, controller, route)"
        ),
        new McpToolSetting(
            "list_symfony_forms",
            "Symfony Forms",
            "Lists all Symfony form types in the project"
        ),
        new McpToolSetting(
            "list_symfony_form_options",
            "Symfony Form Options",
            "Lists all options for a specific Symfony form type"
        ),
        new McpToolSetting(
            "generate_symfony_service_definition",
            "Generate Service Definition",
            "Generates YAML or XML service definitions for a given class name with autowired dependencies"
        ),
        new McpToolSetting(
            "list_twig_template_usages",
            "Twig Template Usages",
            "Lists usages of Twig templates across the project (controllers, includes, embeds, extends, imports)"
        )
    );

    public McpSettingsForm(@NotNull Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "MCP Tools";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        // Initialize main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setMinimumSize(new Dimension(200, 200));
        mainPanel.setPreferredSize(new Dimension(600, 400));

        // Create top panel with checkbox and description
        JPanel topPanel = new JPanel(new BorderLayout());

        // Master enable/disable checkbox (on top left)
        mcpEnabledCheckBox = new JCheckBox("Enable MCP Tools");
        mcpEnabledCheckBox.addActionListener(e -> {
            changed = true;
            updateTableEnabledState();
        });
        topPanel.add(mcpEnabledCheckBox, BorderLayout.NORTH);

        // Description text (below checkbox)
        JTextArea descriptionTextArea = new JTextArea(
            "MCP (Model Context Protocol) tools provide programmatic access to Symfony project information " +
                "for AI assistants and other tools. Enable or disable individual tools below.",
            3, 40
        );
        descriptionTextArea.setEditable(false);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        descriptionTextArea.setBackground(mainPanel.getBackground());
        descriptionTextArea.setFont(UIManager.getFont("Label.font"));
        descriptionTextArea.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        descriptionTextArea.setMinimumSize(new Dimension(0, 0));
        topPanel.add(descriptionTextArea, BorderLayout.CENTER);

        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Create table
        this.tableView = new TableView<>();
        this.tableView.setMinimumSize(new Dimension(0, 0));

        this.modelList = new ListTableModel<>(
            new ToolNameColumn(),
            new DescriptionColumn(),
            new EnabledColumn()
        );

        this.attachItems();
        this.tableView.setModelAndUpdateColumns(this.modelList);
        this.modelList.addTableModelListener(e -> McpSettingsForm.this.changed = true);

        // Create toolbar decorator without add/remove actions (read-only list)
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(this.tableView);
        decorator.disableAddAction();
        decorator.disableRemoveAction();
        decorator.disableUpDownActions();

        JPanel decoratorPanel = decorator.createPanel();
        decoratorPanel.setMinimumSize(new Dimension(0, 0));
        mainPanel.add(decoratorPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    private void attachItems() {
        McpSettings settings = getSettings();

        // Get existing settings or initialize with defaults
        List<McpToolSetting> existingSettings = settings.mcpToolSettings;

        if (existingSettings == null || existingSettings.isEmpty()) {
            // Initialize with all default tools
            for (McpToolSetting defaultTool : DEFAULT_MCP_TOOLS) {
                this.modelList.addRow(new McpToolSetting(
                    defaultTool.getToolId(),
                    defaultTool.getToolName(),
                    defaultTool.getDescription(),
                    true
                ));
            }
        } else {
            // Load existing settings, but ensure all default tools are present
            List<String> existingToolIds = new ArrayList<>();
            for (McpToolSetting setting : existingSettings) {
                existingToolIds.add(setting.getToolId());
                this.modelList.addRow(new McpToolSetting(
                    setting.getToolId(),
                    setting.getToolName(),
                    setting.getDescription(),
                    setting.isEnabled()
                ));
            }

            // Add any new tools that weren't in the saved settings
            for (McpToolSetting defaultTool : DEFAULT_MCP_TOOLS) {
                if (!existingToolIds.contains(defaultTool.getToolId())) {
                    this.modelList.addRow(new McpToolSetting(
                        defaultTool.getToolId(),
                        defaultTool.getToolName(),
                        defaultTool.getDescription(),
                        true
                    ));
                }
            }
        }
    }

    private void updateTableEnabledState() {
        boolean enabled = mcpEnabledCheckBox.isSelected();
        tableView.setEnabled(enabled);
    }

    @Override
    public boolean isModified() {
        return this.changed || getSettings().mcpEnabled != mcpEnabledCheckBox.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        List<McpToolSetting> toolSettings = new ArrayList<>();

        for (McpToolSetting tool : this.tableView.getListTableModel().getItems()) {
            toolSettings.add(new McpToolSetting(
                tool.getToolId(),
                tool.getToolName(),
                tool.getDescription(),
                tool.isEnabled()
            ));
        }

        getSettings().mcpEnabled = mcpEnabledCheckBox.isSelected();
        getSettings().mcpToolSettings = toolSettings;
        this.changed = false;
    }

    @Override
    public void reset() {
        this.resetList();
        this.attachItems();
        this.updateUIFromSettings();
        this.changed = false;
    }

    private void updateUIFromSettings() {
        this.mcpEnabledCheckBox.setSelected(getSettings().mcpEnabled);
        updateTableEnabledState();
    }

    private void resetList() {
        while (this.modelList.getRowCount() > 0) {
            this.modelList.removeRow(0);
        }
    }

    @Override
    public void disposeUIResources() {
        this.resetList();
    }

    private McpSettings getSettings() {
        return McpSettings.getInstance(this.project);
    }

    // Column definitions
    private static class ToolNameColumn extends ColumnInfo<McpToolSetting, String> {
        public ToolNameColumn() {
            super("Tool Name");
        }

        @Nullable
        @Override
        public String valueOf(McpToolSetting setting) {
            return setting.getToolName();
        }

        @Override
        public int getWidth(JTable table) {
            return 200;
        }
    }

    private static class DescriptionColumn extends ColumnInfo<McpToolSetting, String> {
        public DescriptionColumn() {
            super("Description");
        }

        @Nullable
        @Override
        public String valueOf(McpToolSetting setting) {
            return setting.getDescription();
        }
    }

    private static class EnabledColumn extends ColumnInfo<McpToolSetting, Boolean> {
        public EnabledColumn() {
            super("Enabled");
        }

        @Override
        public Boolean valueOf(McpToolSetting setting) {
            return setting.isEnabled();
        }

        @Override
        public void setValue(McpToolSetting setting, Boolean value) {
            setting.setEnabled(value);
        }

        @Override
        public boolean isCellEditable(McpToolSetting setting) {
            return true;
        }

        @Override
        public Class<?> getColumnClass() {
            return Boolean.class;
        }

        @Override
        public int getWidth(JTable table) {
            return 80;
        }

        @Nullable
        @Override
        public TableCellRenderer getRenderer(McpToolSetting setting) {
            return new BooleanTableCellRenderer();
        }
    }
}
