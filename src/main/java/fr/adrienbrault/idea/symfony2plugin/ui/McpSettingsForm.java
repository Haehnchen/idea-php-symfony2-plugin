package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpApplicationSettings;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpSettings;
import fr.adrienbrault.idea.symfony2plugin.mcp.McpToolSetting;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration form for MCP (Model Context Protocol) tools.
 * Combines both global visibility (application-level) and project-level settings.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class McpSettingsForm implements Configurable {

    private JCheckBox mcpEnabledCheckBox;
    private TableView<ToolSettingItem> tableView;
    private final Project project;
    private boolean changed = false;
    private ListTableModel<ToolSettingItem> modelList;

    // All available MCP tools with their metadata
    private static final List<ToolSettingItem> DEFAULT_MCP_TOOLS = List.of(
        new ToolSettingItem(
            "list_symfony_routes_url_controllers",
            "Symfony Routes, URLs, and Controllers",
            "Lists Symfony routes with URLs, controllers, file paths, Twig templates, and optional URL/path filtering"
        ),
        new ToolSettingItem(
            "list_symfony_commands",
            "Symfony Commands",
            "Lists all Symfony console commands available in the project"
        ),
        new ToolSettingItem(
            "list_doctrine_entities",
            "Doctrine Entities",
            "Lists all Doctrine ORM entities in the project"
        ),
        new ToolSettingItem(
            "list_doctrine_entity_fields",
            "Doctrine Entity Fields",
            "Lists all fields of a specific Doctrine entity including relations"
        ),
        new ToolSettingItem(
            "list_twig_extensions",
            "Twig Extensions",
            "Lists all Twig extensions: filters, functions, tests, and tags"
        ),
        new ToolSettingItem(
            "list_profiler_requests",
            "Profiler Requests",
            "Lists the last 10 Symfony profiler requests"
        ),
        new ToolSettingItem(
            "list_symfony_forms",
            "Symfony Forms",
            "Lists all Symfony form types in the project"
        ),
        new ToolSettingItem(
            "list_symfony_form_options",
            "Symfony Form Options",
            "Lists all options for a specific Symfony form type"
        ),
        new ToolSettingItem(
            "generate_symfony_service_definition",
            "Generate Service Definition",
            "Generates YAML or XML service definitions for a given class name"
        ),
        new ToolSettingItem(
            "list_twig_template_usages",
            "Twig Template Usages",
            "Lists usages of Twig templates across the project"
        ),
        new ToolSettingItem(
            "list_twig_components",
            "Twig Components",
            "Lists Symfony UX Twig components"
        ),
        new ToolSettingItem(
            "list_twig_template_variables",
            "Twig Template Variables",
            "Lists variables available in Twig templates"
        ),
        new ToolSettingItem(
            "locate_symfony_service",
            "Locate Symfony Service",
            "Locates a Symfony service by ID or class name"
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
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setMinimumSize(new Dimension(200, 200));
        mainPanel.setPreferredSize(new Dimension(700, 400));

        // Create top panel with checkbox and description
        JPanel topPanel = new JPanel(new BorderLayout());

        // Master enable/disable checkbox (on top left) - now global setting
        mcpEnabledCheckBox = new JCheckBox("Enable MCP Tools globally");
        mcpEnabledCheckBox.addActionListener(e -> {
            changed = true;
            updateTableEnabledState();
        });
        topPanel.add(mcpEnabledCheckBox, BorderLayout.NORTH);

        // Description text with hints
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.Y_AXIS));
        descriptionPanel.setOpaque(false);
        descriptionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Hint styling
        Font smallFont = UIManager.getFont("Label.font").deriveFont(Math.max(UIManager.getFont("Label.font").getSize() - 2f, 10f));
        Color hintColor = UIManager.getColor("Label.disabledForeground");
        if (hintColor == null) {
            hintColor = JBColor.GRAY;
        }

        // Description lines
        descriptionPanel.add(createHintLabel("MCP tools provide programmatic access to Symfony project information.", smallFont, hintColor));

        JLabel projectLabel = createHintLabel("Project: tool state for this project (does not change visibility).", smallFont, hintColor);
        projectLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        descriptionPanel.add(projectLabel);

        JLabel globalLabel = createHintLabel("Global: tool visibility to the MCP server (application-wide).", smallFont, hintColor);
        globalLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        descriptionPanel.add(globalLabel);

        // Restart hint with clickable link to MCP server settings
        JPanel restartHintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        restartHintPanel.setOpaque(false);
        restartHintPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        restartHintPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JLabel restartHintPrefix = createHintLabel("⚠ Changing global settings requires restarting the IDE or ", smallFont, hintColor);
        restartHintPanel.add(restartHintPrefix);

        JLabel mcpSettingsLink = new JLabel("re-enabling the MCP server");
        mcpSettingsLink.setFont(smallFont);
        mcpSettingsLink.setForeground(UIManager.getColor("link.foreground") != null ? UIManager.getColor("link.foreground") : new JBColor(new Color(0x2470B3), new Color(0x589DF6)));
        mcpSettingsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        mcpSettingsLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Navigate within the currently open Settings dialog instead of opening a new one
                DataContext dataContext = com.intellij.ide.DataManager.getInstance().getDataContext(mcpSettingsLink);
                Settings settings = Settings.KEY.getData(dataContext);
                if (settings != null) {
                    Configurable configurable = settings.find("com.intellij.mcpserver.settings");
                    if (configurable != null) {
                        settings.select(configurable);
                    }
                }
            }
        });
        restartHintPanel.add(mcpSettingsLink);

        JLabel restartHintSuffix = createHintLabel(" to take effect.", smallFont, hintColor);
        restartHintPanel.add(restartHintSuffix);

        descriptionPanel.add(restartHintPanel);

        topPanel.add(descriptionPanel, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Create table
        this.tableView = new TableView<>();
        this.tableView.setMinimumSize(new Dimension(0, 0));

        // Columns: Tool Name, Description, Project, Global (Global is last)
        this.modelList = new ListTableModel<>(
            new ToolNameColumn(),
            new DescriptionColumn(),
            new ProjectEnabledColumn(),
            new GlobalVisibleColumn()
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
        McpSettings projectSettings = getProjectSettings();
        McpApplicationSettings appSettings = getApplicationSettings();

        for (ToolSettingItem defaultTool : DEFAULT_MCP_TOOLS) {
            // Get application-level visibility (global)
            boolean globallyVisible = appSettings.isToolGloballyVisible(defaultTool.toolId);

            // Get project-level enabled status
            boolean projectEnabled = true;
            if (projectSettings.mcpToolSettings != null) {
                for (McpToolSetting setting : projectSettings.mcpToolSettings) {
                    if (setting.getToolId().equals(defaultTool.toolId)) {
                        projectEnabled = setting.isEnabled();
                        break;
                    }
                }
            }

            this.modelList.addRow(new ToolSettingItem(
                defaultTool.toolId,
                defaultTool.toolName,
                defaultTool.description,
                globallyVisible,
                projectEnabled
            ));
        }
    }

    private void updateTableEnabledState() {
        boolean enabled = mcpEnabledCheckBox.isSelected();
        tableView.setEnabled(enabled);
    }

    @Override
    public boolean isModified() {
        McpApplicationSettings appSettings = getApplicationSettings();
        return this.changed || appSettings.mcpEnabled != mcpEnabledCheckBox.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        McpSettings projectSettings = getProjectSettings();
        McpApplicationSettings appSettings = getApplicationSettings();

        // Save application-level master enable setting
        appSettings.mcpEnabled = mcpEnabledCheckBox.isSelected();

        // Save project-level settings and global visibility for each tool
        List<McpToolSetting> toolSettings = new ArrayList<>();
        for (ToolSettingItem item : this.tableView.getListTableModel().getItems()) {
            toolSettings.add(new McpToolSetting(
                item.toolId,
                item.toolName,
                item.description,
                item.projectEnabled
            ));

            // Save application-level visibility
            appSettings.setToolGloballyVisible(item.toolId, item.globallyVisible);
        }

        projectSettings.mcpToolSettings = toolSettings;
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
        McpApplicationSettings appSettings = getApplicationSettings();
        this.mcpEnabledCheckBox.setSelected(appSettings.mcpEnabled);
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

    private McpSettings getProjectSettings() {
        return McpSettings.getInstance(this.project);
    }

    private McpApplicationSettings getApplicationSettings() {
        return McpApplicationSettings.getInstance();
    }

    /**
     * Creates a hint label with smaller, greyish text
     */
    private static JLabel createHintLabel(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    // Data class for table items
    private static class ToolSettingItem {
        final String toolId;
        final String toolName;
        final String description;
        boolean globallyVisible;
        boolean projectEnabled;

        ToolSettingItem(String toolId, String toolName, String description) {
            this(toolId, toolName, description, true, true);
        }

        ToolSettingItem(String toolId, String toolName, String description, boolean globallyVisible, boolean projectEnabled) {
            this.toolId = toolId;
            this.toolName = toolName;
            this.description = description;
            this.globallyVisible = globallyVisible;
            this.projectEnabled = projectEnabled;
        }
    }

    // Column definitions
    private static class ToolNameColumn extends ColumnInfo<ToolSettingItem, String> {
        public ToolNameColumn() {
            super("Tool Name");
        }

        @Nullable
        @Override
        public String valueOf(ToolSettingItem item) {
            return item.toolName;
        }

        @Override
        public int getWidth(JTable table) {
            return 200;
        }
    }

    private static class DescriptionColumn extends ColumnInfo<ToolSettingItem, String> {
        public DescriptionColumn() {
            super("Description");
        }

        @Nullable
        @Override
        public String valueOf(ToolSettingItem item) {
            return item.description;
        }
    }

    private static class ProjectEnabledColumn extends ColumnInfo<ToolSettingItem, Boolean> {
        public ProjectEnabledColumn() {
            super("Project");
        }

        @Override
        public Boolean valueOf(ToolSettingItem item) {
            return item.projectEnabled;
        }

        @Override
        public void setValue(ToolSettingItem item, Boolean value) {
            item.projectEnabled = value;
        }

        @Override
        public boolean isCellEditable(ToolSettingItem item) {
            return true;
        }

        @Override
        public Class<?> getColumnClass() {
            return Boolean.class;
        }

        @Override
        public int getWidth(JTable table) {
            return 60;
        }

        @Nullable
        @Override
        public TableCellRenderer getRenderer(ToolSettingItem item) {
            return new BooleanTableCellRenderer();
        }
    }

    private static class GlobalVisibleColumn extends ColumnInfo<ToolSettingItem, Boolean> {
        public GlobalVisibleColumn() {
            super("Global");
        }

        @Override
        public Boolean valueOf(ToolSettingItem item) {
            return item.globallyVisible;
        }

        @Override
        public void setValue(ToolSettingItem item, Boolean value) {
            item.globallyVisible = value;
        }

        @Override
        public boolean isCellEditable(ToolSettingItem item) {
            return true;
        }

        @Override
        public Class<?> getColumnClass() {
            return Boolean.class;
        }

        @Override
        public int getWidth(JTable table) {
            return 60;
        }

        @Nullable
        @Override
        public TableCellRenderer getRenderer(ToolSettingItem item) {
            return new BooleanTableCellRenderer();
        }
    }
}
