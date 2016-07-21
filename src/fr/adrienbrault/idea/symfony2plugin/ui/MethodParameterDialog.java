package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.table.TableView;
import fr.adrienbrault.idea.symfony2plugin.assistant.AssistantReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.AssistantReferenceUtil;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.*;

public class MethodParameterDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox comboProvider;
    private JComboBox comboContributor;
    private JTextField textCallTo;
    private JTextField textMethodName;
    private JTextField textIndex;
    private JTextField textContributorData;
    private MethodParameterSetting methodParameterSetting;
    private TableView<MethodParameterSetting> tableView;

    private Project project;

    class ComboBoxRenderer extends ColoredListCellRenderer {
        @Override
        protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
            append((String) value);
        }
    }

    public MethodParameterDialog(Project project, TableView<MethodParameterSetting> tableView) {
        this.tableView = tableView;
        this.project = project;

        comboContributor.setRenderer(new ComboBoxRenderer());
        comboProvider.setRenderer(new ComboBoxRenderer());

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        this.textCallTo.getDocument().addDocumentListener(new ChangeDocumentListener());
        this.textMethodName.getDocument().addDocumentListener(new ChangeDocumentListener());
        this.textIndex.getDocument().addDocumentListener(new ChangeDocumentListener());


        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        comboContributor.addItemListener(e -> {
            AssistantReferenceContributor assistantReferenceContributor = AssistantReferenceUtil.getContributorProviderByName(MethodParameterDialog.this.project, (String) e.getItem());
            if(assistantReferenceContributor != null) {
                MethodParameterDialog.this.textContributorData.setEnabled(assistantReferenceContributor.supportData());
            }
        });

        // allow only number values to indexes
        // simple?
        this.textIndex.addKeyListener(new KeyAdapter() {
            private String allowedRegex = "[^0-9]";

            @Override
            public void keyReleased(KeyEvent e) {
                String curText = ((JTextComponent) e.getSource()).getText();
                curText = curText.replaceAll(allowedRegex, "");

                ((JTextComponent) e.getSource()).setText(curText);
            }
        });

        this.attachComboBoxValues(project);
    }

    private void attachComboBoxValues(Project project) {
        for(String provider: AssistantReferenceUtil.getReferenceProvider(project)) {
            comboProvider.addItem(provider);
        }

        for(String provider: AssistantReferenceUtil.getContributorProvider(project)) {
            comboContributor.addItem(provider);
        }
    }

    public MethodParameterDialog(Project project, TableView<MethodParameterSetting> tableView, MethodParameterSetting methodParameterSetting) {
        this(project, tableView);

        this.textCallTo.setText(methodParameterSetting.getCallTo());
        this.textMethodName.setText(methodParameterSetting.getMethodName());
        this.textIndex.setText(String.valueOf(methodParameterSetting.getIndexParameter()));
        this.textContributorData.setText(methodParameterSetting.getContributorData());
        this.methodParameterSetting = methodParameterSetting;

        if(methodParameterSetting.getReferenceProviderName() != null) {
            this.comboProvider.setSelectedItem(methodParameterSetting.getReferenceProviderName());
        }

        if(methodParameterSetting.getContributorName() != null) {
            this.comboContributor.setSelectedItem(methodParameterSetting.getContributorName());
        }

    }

    private void onOK() {

        int index;
        try {
            index = Integer.parseInt(this.textIndex.getText());
        } catch (NumberFormatException e) {
            index = 0;
        }

        MethodParameterSetting twigPath = new MethodParameterSetting(this.textCallTo.getText(), this.textMethodName.getText(), index, (String) this.comboProvider.getSelectedItem());
        twigPath.setContributorName((String) comboContributor.getSelectedItem());

        if(textContributorData.isEnabled()) {
            twigPath.setContributorData(textContributorData.getText());
        }

        twigPath.setReferenceProviderName((String) comboProvider.getSelectedItem());

        // re-add old item to not use public setter wor twigpaths
        // update ?
        if(this.methodParameterSetting != null) {
            int row = this.tableView.getSelectedRows()[0];
            this.tableView.getListTableModel().removeRow(row);
            this.tableView.getListTableModel().insertRow(row, twigPath);
            this.tableView.setRowSelectionInterval(row, row);
        } else {
            int row = this.tableView.getRowCount();
            this.tableView.getListTableModel().addRow(twigPath);
            this.tableView.setRowSelectionInterval(row, row);
        }

        dispose();
    }

    private void setOkState() {
        this.buttonOK.setEnabled(
            this.textCallTo.getText().length() > 0 &&
            this.textMethodName.getText().length() > 0 &&
            this.textIndex.getText().length() > 0
        );
    }

    private class ChangeDocumentListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            setOkState();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            setOkState();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            setOkState();
        }
    }

    private void onCancel() {
        dispose();
    }

}
