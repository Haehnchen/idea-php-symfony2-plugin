package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.table.TableView;
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting;
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.PhpTypeSignatureInterface;
import fr.adrienbrault.idea.symfony2plugin.assistant.signature.PhpTypeSignatureTypes;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class MethodSignatureTypeDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox comboProvider;
    private JTextField textCallTo;
    private JTextField textMethodName;
    private JTextField textIndex;
    private MethodSignatureSetting methodParameterSetting;
    private final TableView<MethodSignatureSetting> tableView;

    private final Project project;

    static class ComboBoxRenderer extends ColoredListCellRenderer {
        @Override
        protected void customizeCellRenderer(JList list, Object value, int index, boolean selected, boolean hasFocus) {
            append((String) value);
        }
    }

    public MethodSignatureTypeDialog(Project project, TableView<MethodSignatureSetting> tableView) {
        this.tableView = tableView;
        this.project = project;

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


        // allow only number values to indexes
        // simple?
        this.textIndex.addKeyListener(new KeyAdapter() {
            private final String allowedRegex = "[^0-9]";

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
        for (PhpTypeSignatureInterface provider : PhpTypeSignatureTypes.DEFAULT_PROVIDER) {
            comboProvider.addItem(provider.getName());
        }
    }

    public MethodSignatureTypeDialog(Project project, TableView<MethodSignatureSetting> tableView, MethodSignatureSetting methodParameterSetting) {
        this(project, tableView);

        this.textCallTo.setText(methodParameterSetting.getCallTo());
        this.textMethodName.setText(methodParameterSetting.getMethodName());
        this.textIndex.setText(String.valueOf(methodParameterSetting.getIndexParameter()));
        this.methodParameterSetting = methodParameterSetting;

        if (methodParameterSetting.getReferenceProviderName() != null) {
            this.comboProvider.setSelectedItem(methodParameterSetting.getReferenceProviderName());
        }

    }

    private void onOK() {

        int index;
        try {
            index = Integer.parseInt(this.textIndex.getText());
        } catch (NumberFormatException e) {
            index = 0;
        }

        MethodSignatureSetting twigPath = new MethodSignatureSetting(this.textCallTo.getText(), this.textMethodName.getText(), index, (String) this.comboProvider.getSelectedItem());

        // re-add old item to not use public setter wor twigpaths
        // update ?
        if (this.methodParameterSetting != null) {
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
            !this.textCallTo.getText().isEmpty() &&
                !this.textMethodName.getText().isEmpty() &&
                !this.textIndex.getText().isEmpty()
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
