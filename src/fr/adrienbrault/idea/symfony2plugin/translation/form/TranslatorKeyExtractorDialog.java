package fr.adrienbrault.idea.symfony2plugin.translation.form;

import com.intellij.psi.PsiFile;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.action.dict.TranslationFileModel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class TranslatorKeyExtractorDialog extends JDialog {

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textTranslationKey;
    private JPanel panelTableView;
    private final ListTableModel<TranslationFileModel> listTableModel;
    private final OnOkCallback okCallback;

    public TranslatorKeyExtractorDialog(List<TranslationFileModel> translationFileModels, OnOkCallback okCallback) {

        this.okCallback = okCallback;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);


        listTableModel = new ListTableModel<TranslationFileModel>(
            new PathNameColumn(),
            new FileNameColumn(),
            new BooleanColumn("Create")
        );

        listTableModel.addRows(translationFileModels);

        TableView<TranslationFileModel> tableView = new TableView<TranslationFileModel>();
        tableView.setModelAndUpdateColumns(listTableModel);

        panelTableView.add(ToolbarDecorator.createDecorator(tableView)
            .disableAddAction()
            .disableDownAction()
            .disableRemoveAction()
            .disableUpDownActions()
            .createPanel()
        );

    }

    private void onOK() {
        String text = textTranslationKey.getText();

        if(StringUtils.isNotBlank(text)) {
            List<TranslationFileModel> psiFiles = new ArrayList<TranslationFileModel>();
            for(TranslationFileModel translationFileModel: listTableModel.getItems()) {
                if(translationFileModel.isEnabled()) {
                    psiFiles.add(translationFileModel);
                }
            }

            if(psiFiles.size() > 0) {
                okCallback.onClick(psiFiles, text);
                dispose();
                return;
            }

        }

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private class FileNameColumn extends ColumnInfo<TranslationFileModel, String> {

        public FileNameColumn() {
            super("Name");
        }

        @Nullable
        @Override
        public String valueOf(TranslationFileModel domainModel) {
            return domainModel.getPsiFile().getName();
        }

        public int getWidth(JTable table) {
            return 190;
        }

    }

    private class PathNameColumn extends ColumnInfo<TranslationFileModel, String> {

        public PathNameColumn() {
            super("Path");
        }

        @Nullable
        @Override
        public String valueOf(TranslationFileModel domainModel) {

            if(domainModel.getSymfonyBundle() != null) {
                return domainModel.getSymfonyBundle().getName();
            }

            String relative = domainModel.getRelativePath();
            if(relative != null) {
                return relative;
            }

            return domainModel.getPsiFile().getName();
        }

    }

    private static class BooleanColumn extends ColumnInfo<TranslationFileModel, Boolean>
    {
        public BooleanColumn(String name) {
            super(name);
        }

        @Nullable
        @Override
        public Boolean valueOf(TranslationFileModel domainModel) {
            return domainModel.isEnabled();
        }

        public boolean isCellEditable(TranslationFileModel groupItem)
        {
            return true;
        }

        public void setValue(TranslationFileModel domainModel, Boolean value){
            domainModel.setEnabled(value);
        }

        public Class getColumnClass()
        {
            return Boolean.class;
        }

        public int getWidth(JTable table) {
            return 50;
        }
    }


    public static interface OnOkCallback {
        public void onClick(List<TranslationFileModel> files, String keyName);
    }

}
