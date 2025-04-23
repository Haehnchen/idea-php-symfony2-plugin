package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigSettingsForm implements Configurable {

    private JPanel panel1;
    private JPanel panelTableView;
    private JButton resetToDefault;
    private JButton buttonJsonExample;
    private JCheckBox chkTwigBundleNamespaceSupport;
    private TableView<TwigPath> tableView;
    private final Project project;
    private boolean changed = false;
    private ListTableModel<TwigPath> modelList;

    public TwigSettingsForm(@NotNull Project project) {
        this.project = project;
    }

    private void attachItems() {
        if (DumbService.getInstance(project).isDumb()) {
            this.tableView.getEmptyText().setText("Not available while indexing. Please re-open this screen when ready.");

            return;
        }

        List<TwigPath> sortableLookupItems = new ArrayList<>(TwigUtil.getTwigNamespaces(this.project, true));
        sortableLookupItems.sort(new TwigUtil.TwigPathNamespaceComparator());

        for (TwigPath twigPath : sortableLookupItems) {
            // dont use managed class here
            this.modelList.addRow(TwigPath.createClone(twigPath));
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Twig";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {

        this.tableView = new TableView<>();
        this.modelList = new ListTableModel<>(
            new NamespaceColumn(),
            new PathColumn(project),
            new TypeColumn(),
            new CustomColumn(),
            new DisableColumn()
        );

        this.attachItems();

        this.tableView.setModelAndUpdateColumns(this.modelList);

        this.modelList.addTableModelListener(e -> TwigSettingsForm.this.changed = true);

        resetToDefault.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                TwigSettingsForm.this.resetList();

                List<TwigPath> sortableLookupItems = new ArrayList<>(TwigUtil.getTwigNamespaces(TwigSettingsForm.this.project, false));
                sortableLookupItems.sort(new TwigUtil.TwigPathNamespaceComparator());

                for (TwigPath twigPath : sortableLookupItems) {
                    // dont use managed class here
                    // @TODO state to enabled (should not be here)
                    TwigSettingsForm.this.modelList.addRow(TwigPath.createClone(twigPath).setEnabled(true));
                }
            }
        });

        ToolbarDecorator tablePanel = ToolbarDecorator.createDecorator(this.tableView, new ElementProducer<>() {
            @Override
            public TwigPath createElement() {
                //IdeFocusManager.getInstance(TwigSettingsForm.this.project).requestFocus(TwigNamespaceDialog.getWindows(), true);
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean canCreateElement() {
                return true;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        tablePanel.setEditAction(anActionButton -> TwigSettingsForm.this.openTwigPathDialog(TwigSettingsForm.this.tableView.getSelectedObject()));


        tablePanel.setAddAction(anActionButton -> TwigSettingsForm.this.openTwigPathDialog(null));

        tablePanel.setEditActionUpdater(e -> {
            TwigPath twigPath = TwigSettingsForm.this.tableView.getSelectedObject();
            return twigPath != null && twigPath.isCustomPath();
        });

        tablePanel.setRemoveActionUpdater(e -> {
            TwigPath twigPath = TwigSettingsForm.this.tableView.getSelectedObject();
            return twigPath != null && twigPath.isCustomPath();
        });

        tablePanel.disableUpAction();
        tablePanel.disableDownAction();

        this.panelTableView.add(tablePanel.createPanel());

        buttonJsonExample.addActionListener(e -> TwigJsonExampleDialog.open(TwigSettingsForm.this.panel1));

        return this.panel1;
    }

    @Override
    public boolean isModified() {
        return this.changed
            || getSettings().twigBundleNamespaceSupport != chkTwigBundleNamespaceSupport.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        List<TwigNamespaceSetting> twigPaths = new ArrayList<>();

        for (TwigPath twigPath : this.tableView.getListTableModel().getItems()) {
            // only custom and disabled path need to save
            if ((!twigPath.isEnabled() && twigPath.getRelativePath(this.project) != null) || twigPath.isCustomPath()) {
                twigPaths.add(new TwigNamespaceSetting(twigPath.getNamespace(), twigPath.getRelativePath(this.project), twigPath.isEnabled(), twigPath.getNamespaceType(), twigPath.isCustomPath()));
            }
        }

        getSettings().twigBundleNamespaceSupport = chkTwigBundleNamespaceSupport.isSelected();
        getSettings().twigNamespaces = twigPaths;
        this.changed = false;
    }

    private Settings getSettings() {
        return Settings.getInstance(this.project);
    }

    private void resetList() {
        // clear list, easier?
        while (this.modelList.getRowCount() > 0) {
            this.modelList.removeRow(0);
        }

    }

    @Override
    public void reset() {
        this.resetList();
        this.attachItems();
        this.updateUIFromSettings();
        this.changed = false;
    }

    private void updateUIFromSettings() {
        this.chkTwigBundleNamespaceSupport.setSelected(getSettings().twigBundleNamespaceSupport);
    }

    @Override
    public void disposeUIResources() {
        this.resetList();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new BorderLayout(0, 0));
        panelTableView = new JPanel();
        panelTableView.setLayout(new BorderLayout(0, 0));
        panel1.add(panelTableView, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new BorderLayout(0, 0));
        panel1.add(panel2, BorderLayout.NORTH);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setText("Manage Twig Namespaces");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        resetToDefault = new JButton();
        resetToDefault.setText("Reset To Default");
        panel3.add(resetToDefault, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonJsonExample = new JButton();
        buttonJsonExample.setText("JSON Example");
        panel3.add(buttonJsonExample, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel3.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        chkTwigBundleNamespaceSupport = new JCheckBox();
        chkTwigBundleNamespaceSupport.setSelected(true);
        chkTwigBundleNamespaceSupport.setText("Support Bundle Namespaces");
        chkTwigBundleNamespaceSupport.setToolTipText("Example: Foobar:Bar:Foo.html.twig (for older Symfony Versions)");
        panel3.add(chkTwigBundleNamespaceSupport, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private static class NamespaceColumn extends ColumnInfo<TwigPath, String> {

        public NamespaceColumn() {
            super("Namespace");
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.getNamespace();
        }
    }

    private static class PathColumn extends ColumnInfo<TwigPath, String> {

        private final Project project;

        public PathColumn(Project project) {
            super("Path");
            this.project = project;
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.getRelativePath(this.project);
        }
    }

    private static class CustomColumn extends ColumnInfo<TwigPath, String> {

        public CustomColumn() {
            super("Parser");
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.isCustomPath() ? "Custom" : "Internal";
        }
    }

    private static class TypeColumn extends ColumnInfo<TwigPath, String> {

        public TypeColumn() {
            super("Type");
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.getNamespaceType().toString();
        }
    }

    private abstract static class BooleanColumn extends ColumnInfo<TwigPath, Boolean> {
        public BooleanColumn(String name) {
            super(name);
        }

        public boolean isCellEditable(TwigPath groupItem) {
            return true;
        }

        public Class getColumnClass() {
            return Boolean.class;
        }
    }

    private class DisableColumn extends BooleanColumn {

        public DisableColumn() {
            super("on");
        }

        public Boolean valueOf(TwigPath twigPath) {
            return twigPath.isEnabled();
        }

        public void setValue(TwigPath twigPath, Boolean value) {
            twigPath.setEnabled(value);
            TwigSettingsForm.this.tableView.getListTableModel().fireTableDataChanged();
        }

        public int getWidth(JTable table) {
            return 50;
        }

    }

    private void openTwigPathDialog(@Nullable TwigPath twigPath) {
        TwigNamespaceDialog twigNamespaceDialog;
        if (twigPath == null) {
            twigNamespaceDialog = new TwigNamespaceDialog(project, this.tableView);
        } else {
            twigNamespaceDialog = new TwigNamespaceDialog(project, this.tableView, twigPath);
        }

        Dimension dim = new Dimension();
        dim.setSize(500, 190);
        twigNamespaceDialog.setTitle("Twig Namespace");
        twigNamespaceDialog.setMinimumSize(dim);
        twigNamespaceDialog.pack();
        twigNamespaceDialog.setLocationRelativeTo(TwigSettingsForm.this.panel1);

        twigNamespaceDialog.setVisible(true);
    }

}
