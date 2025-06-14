package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceArgumentSelectionDialog extends JDialog {

    private final Project project;
    private final Map<String, Set<String>> arguments = new HashMap<>();
    private final Callback callback;
    private JPanel panel1;
    private JButton generateButton;
    private JButton closeButton;
    private JPanel mainPanel;

    private TableView<ServiceParameter> tableView;
    private ListTableModel<ServiceParameter> modelList;

    public ServiceArgumentSelectionDialog(Project project, Map<String, Set<String>> arguments, Callback callback) {
        this.project = project;
        this.arguments.putAll(arguments);
        this.callback = callback;
    }

    public void init() {

        setContentPane(panel1);
        setModal(true);

        generateButton.addActionListener(e -> {
            setEnabled(false);

            java.util.List<String> items = new ArrayList<>();

            for (ServiceParameter serviceParameter : modelList.getItems()) {
                items.add(serviceParameter.getCurrentService());
            }

            ApplicationManager.getApplication().runWriteAction(() -> callback.onOk(items));

            dispose();
        });

        generateButton.requestFocusInWindow();
        this.getRootPane().setDefaultButton(generateButton);

        this.closeButton.addActionListener(e -> onCancel());

        this.getRootPane().registerKeyboardAction(e ->
                onCancel(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
    }

    private void onCancel() {
        setEnabled(false);
        dispose();
    }

    private void createUIComponents() {
        mainPanel = new JPanel();

        mainPanel.setLayout(new GridLayout(0, 1));

        this.modelList = new ListTableModel<>(
            new IconColumn(),
            new NamespaceColumn(),
            new ServiceColumn()
        );

        for (Map.Entry<String, Set<String>> entry : this.arguments.entrySet()) {
            this.modelList.addRow(new ServiceParameter(entry.getKey(), entry.getValue()));
        }

        this.tableView = new TableView<>();
        this.tableView.setModelAndUpdateColumns(this.modelList);

        mainPanel.add(ToolbarDecorator.createDecorator(this.tableView)
            .disableAddAction()
            .disableDownAction()
            .disableRemoveAction()
            .disableUpDownActions()
            .createPanel()
        );

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
        createUIComponents();
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 3, new Insets(5, 5, 5, 5), -1, -1));
        generateButton = new JButton();
        generateButton.setText("Generate");
        panel1.add(generateButton, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        closeButton = new JButton();
        closeButton.setText("Close");
        panel1.add(closeButton, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        panel1.add(mainPanel, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

    private static class ServiceParameter {

        private final String classFqn;
        private final Set<String> possibleServices;

        private String currentService;

        public ServiceParameter(String classFqn, Set<String> possibleServices) {
            this.classFqn = classFqn;
            this.possibleServices = possibleServices;

            if (!possibleServices.isEmpty()) {
                currentService = possibleServices.iterator().next();
            }

        }

        public String getClassFqn() {
            return classFqn;
        }

        public Set<String> getPossibleServices() {
            return possibleServices;
        }

        public String getCurrentService() {
            return currentService;
        }

        public void setCurrentService(String currentService) {

            if (StringUtils.isBlank(currentService)) {
                this.currentService = null;
                return;
            }

            this.currentService = currentService;
        }

    }


    private static class NamespaceColumn extends ColumnInfo<ServiceParameter, String> {

        public NamespaceColumn() {
            super("Class");
        }

        @Nullable
        @Override
        public String valueOf(ServiceParameter modelParameter) {

            String classFqn = modelParameter.getClassFqn();
            if (StringUtils.isBlank(classFqn)) {
                return "unknown";
            }

            if (classFqn.startsWith("\\")) {
                classFqn = classFqn.substring(1);
            }

            int i = classFqn.lastIndexOf("\\");
            if (i > 0) {
                String ns = classFqn.substring(0, i);
                String clazz = classFqn.substring(i + 1);
                classFqn = String.format("%s [%s]", clazz, ns);
            }

            return classFqn;
        }
    }


    private class ServiceColumn extends ColumnInfo<ServiceParameter, String> {

        public ServiceColumn() {
            super("Service");
        }

        @Nullable
        @Override
        public String valueOf(ServiceParameter modelParameter) {
            return modelParameter.getCurrentService();
        }

        public void setValue(ServiceParameter modelParameter, String value) {
            modelParameter.setCurrentService(value);
            tableView.getListTableModel().fireTableDataChanged();
        }

        @Override
        public boolean isCellEditable(ServiceParameter modelParameter) {
            return true;
        }

        @Nullable
        @Override
        public TableCellEditor getEditor(ServiceParameter modelParameter) {

            Set<String> sorted = modelParameter.getPossibleServices();
            ComboBox comboBox = new ComboBox(sorted.toArray(new String[0]), 200);
            comboBox.setEditable(true);

            return new DefaultCellEditor(comboBox);
        }
    }

    private class IconColumn extends ColumnInfo<ServiceParameter, Icon> {
        public IconColumn() {
            super("");
        }

        @Nullable
        @Override
        public Icon valueOf(ServiceParameter modelParameter) {
            PhpClass classInterface = PhpElementsUtil.getClassInterface(project, modelParameter.getClassFqn());
            return classInterface != null ? classInterface.getIcon() : null;
        }

        public Class getColumnClass() {
            return ImageIcon.class;
        }

        @Override
        public int getWidth(JTable table) {
            return 32;
        }

    }

    public interface Callback {
        void onOk(java.util.List<String> items);
    }

    public static ServiceArgumentSelectionDialog createDialog(Project project, Map<String, Set<String>> arguments, Callback callback) {

        ServiceArgumentSelectionDialog dialog = new ServiceArgumentSelectionDialog(project, arguments, callback);
        dialog.pack();
        dialog.init();

        dialog.setMinimumSize(new Dimension(620, 300));
        dialog.setTitle("Symfony: Add Argument");
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return dialog;
    }

}
