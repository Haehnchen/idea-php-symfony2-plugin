package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodSignatureTypeSettingsForm implements Configurable {
    private JPanel panel1;
    private JPanel panelConfigTableView;
    private JCheckBox enableCustomSignatureTypesCheckBox;
    private JButton buttonHelp;

    private final TableView<MethodSignatureSetting> tableView;
    private final Project project;
    private boolean changed = false;
    private final ListTableModel<MethodSignatureSetting> modelList;

    public MethodSignatureTypeSettingsForm(Project project) {
        this.project = project;

        this.tableView = new TableView<>();
        this.modelList = new ListTableModel<>(
            new CallToColumn(),
            new MethodColumn(),
            new IndexColumn(),
            new ProviderColumn()
        );

        this.attachItems();

        this.tableView.setModelAndUpdateColumns(this.modelList);
        this.tableView.getModel().addTableModelListener(e -> MethodSignatureTypeSettingsForm.this.changed = true);

        buttonHelp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                IdeHelper.openUrl("https://www.jetbrains.com/help/phpstorm/symfony-creating-helper-functions.html#signature-types");
            }
        });

        enableCustomSignatureTypesCheckBox.setSelected(getSettings().objectSignatureTypeProvider);

        enableCustomSignatureTypesCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                MethodSignatureTypeSettingsForm.this.changed = true;
            }
        });
    }

    private void attachItems() {

        if (this.getSettings().methodSignatureSettings == null) {
            return;
        }

        for (MethodSignatureSetting methodParameterSetting : this.getSettings().methodSignatureSettings) {
            this.modelList.addRow(methodParameterSetting);
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return null;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ToolbarDecorator tablePanel = ToolbarDecorator.createDecorator(this.tableView, new ElementProducer<>() {
            @Override
            public MethodSignatureSetting createElement() {
                //IdeFocusManager.getInstance(TwigSettingsForm.this.project).requestFocus(TwigNamespaceDialog.getWindows(), true);
                return null;
            }

            @Override
            public boolean canCreateElement() {
                return true;
            }
        });

        tablePanel.setEditAction(anActionButton ->
            MethodSignatureTypeSettingsForm.this.openTwigPathDialog(MethodSignatureTypeSettingsForm.this.tableView.getSelectedObject())
        );

        tablePanel.setAddAction(anActionButton ->
            MethodSignatureTypeSettingsForm.this.openTwigPathDialog(null)
        );

        tablePanel.disableUpAction();
        tablePanel.disableDownAction();

        this.panelConfigTableView.add(tablePanel.createPanel());

        return this.panel1;
    }

    @Override
    public boolean isModified() {
        return this.changed;
    }

    @Override
    public void apply() throws ConfigurationException {
        getSettings().methodSignatureSettings = new ArrayList<>(this.tableView.getListTableModel().getItems());
        getSettings().objectSignatureTypeProvider = enableCustomSignatureTypesCheckBox.isSelected();

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
        this.changed = false;
    }

    @Override
    public void disposeUIResources() {

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
        panelConfigTableView = new JPanel();
        panelConfigTableView.setLayout(new BorderLayout(0, 0));
        panel1.add(panelConfigTableView, BorderLayout.CENTER);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, BorderLayout.NORTH);
        enableCustomSignatureTypesCheckBox = new JCheckBox();
        enableCustomSignatureTypesCheckBox.setSelected(false);
        enableCustomSignatureTypesCheckBox.setText("Enable Custom Signature Types");
        panel2.add(enableCustomSignatureTypesCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonHelp = new JButton();
        buttonHelp.setText("Help");
        panel2.add(buttonHelp, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private static class CallToColumn extends ColumnInfo<MethodSignatureSetting, String> {

        public CallToColumn() {
            super("CallTo");
        }

        @Nullable
        @Override
        public String valueOf(MethodSignatureSetting methodParameterSetting) {
            return methodParameterSetting.getCallTo();
        }
    }

    private static class MethodColumn extends ColumnInfo<MethodSignatureSetting, String> {

        public MethodColumn() {
            super("Method");
        }

        @Nullable
        @Override
        public String valueOf(MethodSignatureSetting methodSignatureSetting) {
            return methodSignatureSetting.getMethodName();
        }
    }

    private static class IndexColumn extends ColumnInfo<MethodSignatureSetting, Integer> {

        public IndexColumn() {
            super("Index");
        }

        @Nullable
        @Override
        public Integer valueOf(MethodSignatureSetting methodParameterSetting) {
            return methodParameterSetting.getIndexParameter();
        }
    }

    private static class ProviderColumn extends ColumnInfo<MethodSignatureSetting, String> {

        public ProviderColumn() {
            super("Provider");
        }

        @Nullable
        @Override
        public String valueOf(MethodSignatureSetting methodParameterSetting) {
            return methodParameterSetting.getReferenceProviderName();
        }
    }

    private void openTwigPathDialog(@Nullable MethodSignatureSetting methodParameterSetting) {
        MethodSignatureTypeDialog twigNamespaceDialog;
        if (methodParameterSetting == null) {
            twigNamespaceDialog = new MethodSignatureTypeDialog(project, this.tableView);
        } else {
            twigNamespaceDialog = new MethodSignatureTypeDialog(project, this.tableView, methodParameterSetting);
        }

        Dimension dim = new Dimension();
        dim.setSize(500, 190);
        twigNamespaceDialog.setTitle("MethodSignatureSetting");
        twigNamespaceDialog.setMinimumSize(dim);
        twigNamespaceDialog.pack();
        twigNamespaceDialog.setLocationRelativeTo(this.panel1);

        twigNamespaceDialog.setVisible(true);
    }
}
