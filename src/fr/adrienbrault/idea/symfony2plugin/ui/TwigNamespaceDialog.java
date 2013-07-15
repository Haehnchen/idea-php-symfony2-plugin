package fr.adrienbrault.idea.symfony2plugin.ui;

import com.google.common.base.Enums;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.table.TableView;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;

import javax.swing.*;
import java.awt.event.*;
import java.util.Collections;

public class TwigNamespaceDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JComboBox namespaceType;
    private TextFieldWithBrowseButton namespacePath;
    private JCheckBox chkboxEnabled;
    private JTextField name;
    private TableView<TwigPath> tableView;
    private Project project;
    private TwigPath twigPath;

    public TwigNamespaceDialog(Project project, TableView<TwigPath> tableView, TwigPath twigPath) {
        this(project, tableView);
        this.name.setText(twigPath.getNamespace());
        this.namespacePath.getTextField().setText(twigPath.getPath());
        this.namespaceType.getModel().setSelectedItem(twigPath.getNamespaceType().toString());
        this.twigPath = twigPath;
    }

    public TwigNamespaceDialog(Project project, TableView<TwigPath> tableView) {
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

        this.tableView = tableView;
        this.project = project;

        namespacePath.getButton().addMouseListener(createPathButtonMouseListener(namespacePath.getTextField(), FileChooserDescriptorFactory.createSingleFolderDescriptor()));

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
    }

    private void onOK() {

        TwigPath twigPath = new TwigPath(this.namespacePath.getText(), this.name.getText(), TwigPathIndex.NamespaceType.valueOf((String) this.namespaceType.getSelectedItem()), true);

        // re-add old item to not use public setter wor twigpaths
        // update ?
        if(this.twigPath != null) {
            int row = this.tableView.getSelectedRows()[0];
            this.tableView.getListTableModel().removeRow(row);
            this.tableView.getListTableModel().insertRow(row, twigPath);
            this.tableView.setRowSelectionInterval(row, row);
        } else {
            int row = this.tableView.getRowCount();
            this.tableView.getListTableModel().addRow(twigPath);
            this.tableView.setRowSelectionInterval(row, row);
        }

        twigPath.setEnabled(this.chkboxEnabled.isSelected());
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private MouseListener createPathButtonMouseListener(final JTextField textField, final FileChooserDescriptor fileChooserDescriptor) {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
            }

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                VirtualFile projectDirectory = project.getBaseDir();
                VirtualFile selectedFile = FileChooser.chooseFile(
                    fileChooserDescriptor,
                    project,
                    VfsUtil.findRelativeFile(textField.getText(), projectDirectory)
                );

                if (null == selectedFile) {
                    return; // Ignore but keep the previous path
                }

                String path = VfsUtil.getRelativePath(selectedFile, projectDirectory, '/');
                if (null == path) {
                    path = selectedFile.getPath();
                }

                textField.setText(path);
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
            }

            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
            }
        };
    }

}
