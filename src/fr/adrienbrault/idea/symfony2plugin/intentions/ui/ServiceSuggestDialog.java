package fr.adrienbrault.idea.symfony2plugin.intentions.ui;

import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;

public class ServiceSuggestDialog extends JDialog {

    @NotNull
    private final String[] services;

    @NotNull
    private final Callback callback;
    private JPanel contentPane;
    private JButton buttonInsert;
    private JButton buttonCancel;
    private JList listServices;

    public ServiceSuggestDialog(@NotNull final Collection<String> services, @NotNull final Callback callback) {
        this.services = services.toArray(new String[services.size()]);
        this.callback = callback;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonInsert);

        buttonInsert.addActionListener(new ActionListener() {
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

        DefaultListModel listModel = new DefaultListModel();
        for (String path : this.services) {
            listModel.addElement(path);
        }

        listServices.setModel(listModel);
        listServices.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listServices.setSelectedIndex(0);
        listServices.requestFocus();
    }

    private void onOK() {
        int selectedIndex = listServices.getSelectedIndex();
        if(selectedIndex >= 0) {
            this.callback.insert(services[listServices.getSelectedIndex()]);
        }

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static void create(@NotNull Collection<String> services, @NotNull Callback callback) {
        ServiceSuggestDialog templateCreationSelectionDialog = new ServiceSuggestDialog(services, callback);
        Dimension dim = new Dimension();
        dim.setSize(300, 300);
        templateCreationSelectionDialog.setMinimumSize(dim);
        templateCreationSelectionDialog.setTitle("Symfony: Service Suggestions");
        templateCreationSelectionDialog.setIconImage(Symfony2Icons.getImage(Symfony2Icons.SYMFONY));
        templateCreationSelectionDialog.setLocationRelativeTo(null);
        templateCreationSelectionDialog.setVisible(true);
    }

    public interface Callback {
        void insert(@NotNull String selected);
    }
}
