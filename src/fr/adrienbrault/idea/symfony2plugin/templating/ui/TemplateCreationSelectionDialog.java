package fr.adrienbrault.idea.symfony2plugin.templating.ui;

import com.intellij.openapi.editor.Editor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

public class TemplateCreationSelectionDialog extends JDialog {

    @NotNull
    private final Callback callback;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList listPaths;

    final private String[] paths;

    private TemplateCreationSelectionDialog(@NotNull final Collection<String> pathsCollection, @NotNull final Callback callback) {
        this.callback = callback;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        this.paths = pathsCollection.toArray(new String[pathsCollection.size()]);
        Arrays.sort(this.paths, new MyVendorStringComparator());

        DefaultListModel listModel = new DefaultListModel();
        for (String path : this.paths) {
            if (path.length() > 75) {
                path = path.substring(0, 30) + "..." + path.substring(path.length() - 40);
            }

            listModel.addElement(path);
        }

        listPaths.setModel(listModel);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        getRootPane().setDefaultButton(buttonOK);
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

        listPaths.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listPaths.setSelectedIndex(0);
        listPaths.requestFocus();
    }

    private void onOK() {
        int selectedIndex = listPaths.getSelectedIndex();
        if(selectedIndex >= 0) {
            this.callback.ok(paths[listPaths.getSelectedIndex()]);
        }

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public interface Callback {
        void ok(@NotNull String selected);
    }

    private static class MyVendorStringComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {
            if(o1.startsWith("vendor") && !o2.startsWith("vendor")) {
                return 1;
            }

            if(!o1.startsWith("vendor") && o2.startsWith("vendor")) {
                return -1;
            }

            return 0;
        }
    }

    public static void create(@NotNull Editor editor, @NotNull final Collection<String> pathsCollection, @NotNull final Callback callback) {
        TemplateCreationSelectionDialog templateCreationSelectionDialog = new TemplateCreationSelectionDialog(pathsCollection, callback);
        Dimension dim = new Dimension();
        dim.setSize(600, 300);
        templateCreationSelectionDialog.setMinimumSize(dim);
        templateCreationSelectionDialog.setTitle("Twig: Select file to create");
        templateCreationSelectionDialog.setIconImage(Symfony2Icons.getImage(Symfony2Icons.SYMFONY));
        templateCreationSelectionDialog.setLocationRelativeTo(editor.getComponent());
        templateCreationSelectionDialog.setVisible(true);
    }
}
