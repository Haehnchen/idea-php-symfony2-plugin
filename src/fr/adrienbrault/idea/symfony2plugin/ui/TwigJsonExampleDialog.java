package fr.adrienbrault.idea.symfony2plugin.ui;

import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TwigJsonExampleDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextArea pathIsRelativeToTextArea;

    private TwigJsonExampleDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
    }

    private void onOK() {
        dispose();
    }

    public static void open(@NotNull Component relativeTo) {
        TwigJsonExampleDialog twigJsonExampleForm = new TwigJsonExampleDialog();
        twigJsonExampleForm.setTitle("Example: ide-twig.json");
        twigJsonExampleForm.setIconImage(Symfony2Icons.getImage(Symfony2Icons.SYMFONY));
        twigJsonExampleForm.pack();
        twigJsonExampleForm.setLocationRelativeTo(relativeTo);
        twigJsonExampleForm.setVisible(true);
    }
}
