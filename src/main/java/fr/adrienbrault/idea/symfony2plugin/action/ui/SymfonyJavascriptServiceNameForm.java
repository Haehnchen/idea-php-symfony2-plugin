package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.JavascriptServiceNameStrategy;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.script.ScriptException;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyJavascriptServiceNameForm extends JDialog {
    @NotNull
    private final Project project;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textClass;
    private JTextArea textJavascript;
    private JButton buttonTest;
    private JButton buttonExample;
    private JTextField textResult;

    public SymfonyJavascriptServiceNameForm(@NotNull Project project, String className) {
        this(project);
        textClass.setText(className);
    }

    public SymfonyJavascriptServiceNameForm(@NotNull final Project project) {
        this.project = project;

        createUIComponents();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onSave());

        buttonCancel.addActionListener(e -> onCancel());


        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        buttonTest.addActionListener(e -> {
            try {
                Object eval = JavascriptServiceNameStrategy.run(project, textClass.getText(), textJavascript.getText());
                if (!(eval instanceof String)) {
                    textResult.setText("Error: invalid string response");
                    return;
                }

                textResult.setText((String) eval);
            } catch (ScriptException e1) {
                textResult.setText("Error: " + e1.getMessage());
            }

        });

        buttonExample.addActionListener(e -> textJavascript.setText("" +
            "var className = args.className;\n" +
            "var projectName = args.projectName;\n" +
            "var projectBasePath = args.projectBasePath;\n" +
            "var defaultNaming = args.defaultNaming;\n" +
            "\n" +
            "// nullable\n" +
            "var relativePath = args.relativePath;\n" +
            "var absolutePath = args.absolutePath;\n" +
            "\n" +
            "return className.replace(/\\\\/g, '.').toLowerCase();"));

        String jsText = Settings.getInstance(project).serviceJsNameStrategy;
        if (StringUtils.isNotBlank(jsText)) {
            textJavascript.setText(jsText);
        }

    }

    private void createUIComponents() {
        textClass = new JTextField(30);
        textJavascript = new JTextArea();
        textJavascript.setFont(new Font("Courier New", Font.PLAIN, textJavascript.getFont().getSize()));
        textResult = new JTextField();
        textResult.setEditable(false);
        textResult.setEnabled(false);
        buttonOK = new JButton("Save");
        buttonCancel = new JButton("Cancel");
        buttonTest = new JButton("Test");
        buttonExample = new JButton("Example");

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        fieldsPanel.add(new JLabel("PhpClass Name:"), gbc);
        gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        fieldsPanel.add(textClass, gbc);
        gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        fieldsPanel.add(new JLabel("Javascript:"), gbc);
        gbc.gridy = 3; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        fieldsPanel.add(new JScrollPane(textJavascript), gbc);
        gbc.gridy = 4; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.weighty = 0;
        fieldsPanel.add(textResult, gbc);

        JLabel hintLabel = new JLabel("Provide value by 'return'; empty textfield will clear name strategy to project default");
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonsPanel.add(buttonTest);
        buttonsPanel.add(buttonExample);
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightButtons.add(buttonOK);
        rightButtons.add(buttonCancel);
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.add(buttonsPanel, BorderLayout.WEST);
        bottomBar.add(rightButtons, BorderLayout.EAST);

        JPanel southSection = new JPanel(new BorderLayout(0, 2));
        southSection.add(hintLabel, BorderLayout.NORTH);
        southSection.add(bottomBar, BorderLayout.SOUTH);

        contentPane = new JPanel(new BorderLayout(0, 5));
        contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPane.add(fieldsPanel, BorderLayout.CENTER);
        contentPane.add(southSection, BorderLayout.SOUTH);
    }

    private void onSave() {

        String text = textJavascript.getText();
        if (StringUtils.isBlank(text)) {
            text = null;
        }

        Settings.getInstance(project).serviceJsNameStrategy = text;

        dispose();
    }

    private void onCancel() {
        dispose();
    }

    public static void create(@NotNull Component c, @NotNull Project project, @NotNull String className) {
        SymfonyJavascriptServiceNameForm dialog = new SymfonyJavascriptServiceNameForm(project, className);
        dialog.setMinimumSize(new Dimension(500, 400));
        dialog.setTitle("Symfony: Service Generator - JavaScript Naming");
        dialog.pack();
        dialog.setIconImage(Symfony2Icons.getImage(Symfony2Icons.SYMFONY));
        dialog.setLocationRelativeTo(c);
        dialog.setVisible(true);
    }
}
