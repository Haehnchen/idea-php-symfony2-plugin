package fr.adrienbrault.idea.symfony2plugin.ui;

import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
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
        createUIComponents();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());
    }

    private void createUIComponents() {
        contentPane = new JPanel(new BorderLayout(0, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        pathIsRelativeToTextArea = new JTextArea(
            "// \"path\" is relative to file (ide-twig.json)\n" +
            "{\n" +
            "  \"namespaces\": [\n" +
            "    {\n" +
            "      \"namespace\": \"foo\", // @foo/bar/foo.htm.twig\n" +
            "      \"path\": \"res\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"path\": \"res\" // bar/foo.htm.twig\n" +
            "    },\n" +
            "    {\n" +
            "      \"path\": \"res\", // FooBundle:bar:foo.htm.twig\n" +
            "      \"type\": \"Bundle\",\n" +
            "      \"namespace\": \"FooBundle\"\n" +
            "    }\n" +
            "  ]\n" +
            "}"
        );
        pathIsRelativeToTextArea.setEditable(false);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 2));
        centerPanel.add(new JLabel("Example \"ide-twig.json\"; supports multiple locations"), BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(pathIsRelativeToTextArea), BorderLayout.CENTER);

        buttonOK = new JButton("OK");
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonsPanel.add(buttonOK);

        contentPane.add(centerPanel, BorderLayout.CENTER);
        contentPane.add(buttonsPanel, BorderLayout.SOUTH);
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
