package fr.adrienbrault.idea.symfonyplugin.ui;

import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigJsonExampleDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextArea pathIsRelativeToTextArea;

    private TwigJsonExampleDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());
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
