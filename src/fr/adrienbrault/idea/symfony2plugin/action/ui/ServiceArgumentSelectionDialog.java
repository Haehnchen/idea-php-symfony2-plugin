package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class ServiceArgumentSelectionDialog extends JDialog {

    private final Project project;
    private final Map<String, Set<String>> arguments;
    private final Callback callback;
    private JPanel panel1;
    private JButton generateButton;
    private JButton closeButton;
    private JPanel mainPanel;

    private TableView<ServiceParameter> tableView;
    private ListTableModel<ServiceParameter> modelList;

    public ServiceArgumentSelectionDialog(Project project, Map<String, Set<String>> arguments, Callback callback) {
        this.project = project;
        this.arguments = arguments;
        this.callback = callback;
    }

    public void init() {

        setContentPane(panel1);
        setModal(true);

        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnabled(false);

                java.util.List<String> items = new ArrayList<String>();

                for (ServiceParameter serviceParameter : modelList.getItems()) {
                    items.add(serviceParameter.getCurrentService());
                }

                callback.onOk(items);

                dispose();
            }
        });

        this.closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnabled(false);
                dispose();
            }
        });

    }

    private void createUIComponents() {
        mainPanel = new JPanel();

        mainPanel.setLayout(new GridLayout(0, 1));

        this.modelList = new ListTableModel<ServiceParameter>(
            new IconColumn(),
            new NamespaceColumn(),
            new ServiceColumn()
        );

        for (Map.Entry<String, Set<String>> entry : this.arguments.entrySet()) {
            this.modelList.addRow(new ServiceParameter(entry.getKey(), entry.getValue()));
        }

        this.tableView = new TableView<ServiceParameter>();
        this.tableView.setModelAndUpdateColumns(this.modelList);

        mainPanel.add(ToolbarDecorator.createDecorator(this.tableView)
                .disableAddAction()
                .disableDownAction()
                .disableRemoveAction()
                .disableUpDownActions()
                .createPanel()
        );

    }

    private static class ServiceParameter {

        private final String classFqn;
        private final Set<String> possibleServices;

        private String currentService;

        public ServiceParameter(String classFqn, Set<String> possibleServices) {
            this.classFqn = classFqn;
            this.possibleServices = possibleServices;

            if(possibleServices.size() > 0) {
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


    private class NamespaceColumn extends ColumnInfo<ServiceParameter, String> {

        public NamespaceColumn() {
            super("Class");
        }

        @Nullable
        @Override
        public String valueOf(ServiceParameter modelParameter) {
            return modelParameter.getClassFqn();
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

        public void setValue(ServiceParameter modelParameter, String value){
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
            ComboBox comboBox = new ComboBox(sorted.toArray(new String[sorted.size()] ), 200);
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
            return classInterface != null ? classInterface.getIcon(0) : null;
        }

        public java.lang.Class getColumnClass() {
            return ImageIcon.class;
        }

        @Override
        public int getWidth(JTable table) {
            return 32;
        }

    }

    public static interface Callback {
        public void onOk(java.util.List<String> items);
    }

    public static ServiceArgumentSelectionDialog createDialog(Project project, Map<String, Set<String>> arguments, Callback callback) {

        ServiceArgumentSelectionDialog dialog = new ServiceArgumentSelectionDialog(project, arguments, callback);
        dialog.init();

        dialog.setTitle("Add Argument");
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return dialog;
    }

}
