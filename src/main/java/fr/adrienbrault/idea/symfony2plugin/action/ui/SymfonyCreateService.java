package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.JBColor;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.ClassCompletionPanelWrapper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.ui.EditorTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCreateService extends JDialog {

    private JPanel panel1;
    private JPanel content;
    private JPanel tableViewPanel;
    private EditorTextField editorOutput;
    private JButton generateButton;
    private JButton buttonCopy;
    private JButton closeButton;

    private JRadioButton radioButtonOutXml;
    private JRadioButton radioButtonOutYaml;
    private JRadioButton radioButtonOutFluent;
    private JRadioButton radioButtonOutPhpArray;
    private JTextField textFieldServiceName;
    private JButton buttonSettings;
    private JButton buttonInsert;
    private JPanel panelFoo;
    private JCheckBox checkBoxSymfonyIdClass;
    private JPanel serviceNamePanel;

    private TableView<MethodParameter.MethodModelParameter> tableView;
    private ListTableModel<MethodParameter.MethodModelParameter> modelList;

    private Map<String, ContainerService> serviceClass;

    private final Project project;

    @Nullable
    private final PsiFile psiFile;

    @Nullable
    private final Editor editor;

    @Nullable
    private String classInit;

    private ClassCompletionPanelWrapper classCompletionPanelWrapper;

    public SymfonyCreateService(@NotNull final Project project, @Nullable PsiFile psiFile, @Nullable Editor editor, @NotNull String className) {
        this(project, psiFile, editor);
        this.classInit = className;
    }

    public SymfonyCreateService(@NotNull final Project project, @Nullable PsiFile psiFile, @Nullable Editor editor) {
        this.project = project;
        this.psiFile = psiFile;
        this.editor = editor;
    }

    public void init() {

        createUIComponents();
        setContentPane(panel1);
        setModal(true);

        this.classCompletionPanelWrapper = new ClassCompletionPanelWrapper(project, panelFoo, s -> update());

        this.modelList = new ListTableModel<>(
            new IconColumn(),
            new NamespaceColumn(),
            new ParameterIndexColumn(),
            new ServiceColumn(),
            new IsServiceColumn()
        );

        // set default output language on last user selection
        String lastServiceGeneratorLanguage = Settings.getInstance(project).lastServiceGeneratorLanguage;
        if ("xml".equalsIgnoreCase(lastServiceGeneratorLanguage)) {
            radioButtonOutXml.setSelected(true);
        } else if ("yaml".equalsIgnoreCase(lastServiceGeneratorLanguage)) {
            radioButtonOutYaml.setSelected(true);
        } else if ("fluent".equalsIgnoreCase(lastServiceGeneratorLanguage)) {
            radioButtonOutFluent.setSelected(true);
        } else if ("phparray".equalsIgnoreCase(lastServiceGeneratorLanguage)) {
            radioButtonOutPhpArray.setSelected(true);
        }

        // overwrite language output on direct file context
        if (this.psiFile instanceof YAMLFile) {
            radioButtonOutYaml.setSelected(true);
        } else if (this.psiFile instanceof XmlFile) {
            radioButtonOutXml.setSelected(true);
        }

        // lets use yaml as default
        if (!radioButtonOutYaml.isSelected() && !radioButtonOutXml.isSelected() && !radioButtonOutFluent.isSelected() && !radioButtonOutPhpArray.isSelected()) {
            radioButtonOutYaml.setSelected(true);
        }

        this.tableView = new TableView<>();
        this.tableView.setModelAndUpdateColumns(this.modelList);

        tableViewPanel.add(ToolbarDecorator.createDecorator(this.tableView)
            .disableAddAction()
            .disableDownAction()
            .disableRemoveAction()
            .disableUpDownActions()
            .createPanel()
        );

        this.serviceClass = ContainerCollectionResolver.getServices(project);

        //update();

        this.modelList.addTableModelListener(e -> generateServiceDefinition());

        this.generateButton.addActionListener(e -> update());

        this.checkBoxSymfonyIdClass.setSelected(Settings.getInstance(project).serviceClassAsIdAttribute);
        updateServiceNameVisibility();
        this.checkBoxSymfonyIdClass.addItemListener(e -> {
            Settings.getInstance(project).serviceClassAsIdAttribute = checkBoxSymfonyIdClass.isSelected();
            updateServiceNameVisibility();
            generateServiceDefinition();
        });

        this.closeButton.addActionListener(e -> {
            setEnabled(false);
            dispose();
        });

        this.buttonCopy.addActionListener(e -> {
            if (StringUtils.isBlank(editorOutput.getText())) {
                return;
            }

            StringSelection stringSelection = new StringSelection(editorOutput.getText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });

        this.buttonSettings.addActionListener(e -> SymfonyJavascriptServiceNameForm.create(SymfonyCreateService.this, project, classCompletionPanelWrapper.getClassName()));

        initClassName();

        radioButtonOutXml.addActionListener(e -> generateServiceDefinition());

        radioButtonOutYaml.addActionListener(e -> generateServiceDefinition());

        radioButtonOutFluent.addActionListener(e -> generateServiceDefinition());

        radioButtonOutPhpArray.addActionListener(e -> generateServiceDefinition());

        textFieldServiceName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                generateServiceDefinition();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                generateServiceDefinition();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                generateServiceDefinition();
            }
        });

        // exit on "esc" key
        this.getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // insert
        if (this.psiFile instanceof XmlFile || this.psiFile instanceof YAMLFile) {
            this.buttonInsert.setEnabled(true);
            this.buttonInsert.setVisible(true);

            this.buttonInsert.requestFocusInWindow();
            this.getRootPane().setDefaultButton(buttonInsert);

            this.buttonInsert.addActionListener(e -> {
                if (psiFile instanceof XmlFile) {
                    insertXmlServiceTag();
                } else if (psiFile instanceof YAMLFile) {
                    insertYamlServiceTag();
                }
            });

        } else {
            this.buttonInsert.setEnabled(false);
            this.buttonInsert.setVisible(false);
        }

    }

    private void updateServiceNameVisibility() {
        boolean visible = !checkBoxSymfonyIdClass.isSelected();
        serviceNamePanel.setVisible(visible);
    }

    private void createUIComponents() {
        // Components
        generateButton = new JButton("Generate");
        buttonCopy = new JButton("To Clipboard");
        closeButton = new JButton("Close");
        buttonSettings = new JButton("Settings");
        buttonInsert = new JButton("Insert");
        radioButtonOutXml = new JRadioButton("XML");
        radioButtonOutYaml = new JRadioButton("Yaml");
        radioButtonOutFluent = new JRadioButton("Fluent");
        radioButtonOutPhpArray = new JRadioButton("Array");
        textFieldServiceName = new JTextField(20);
        checkBoxSymfonyIdClass = new JCheckBox("Class as id");

        // Editor with syntax highlighting for output
        editorOutput = new EditorTextField("", project, null);
        editorOutput.setOneLineMode(false);
        editorOutput.setViewer(true);
        editorOutput.setPreferredSize(new Dimension(500, 120));

        panelFoo = new JPanel(new GridBagLayout());
        tableViewPanel = new JPanel(new BorderLayout());

        ButtonGroup outputGroup = new ButtonGroup();
        outputGroup.add(radioButtonOutYaml);
        outputGroup.add(radioButtonOutXml);
        outputGroup.add(radioButtonOutFluent);
        outputGroup.add(radioButtonOutPhpArray);

        // Hint label style (smaller, grayed - IntelliJ style)
        Font hintFont = UIManager.getFont("Label.font").deriveFont(Math.max(UIManager.getFont("Label.font").getSize() - 2f, 10f));
        Color hintColor = UIManager.getColor("Label.disabledForeground");
        if (hintColor == null) {
            hintColor = JBColor.GRAY;
        }

        JBLabel classHint = new JBLabel("Use the class name as the service id (e.g. App\\Service\\MyService)");
        classHint.setFont(hintFont);
        classHint.setForeground(hintColor);


        // North: Class label + panelFoo + Generate button
        JPanel northPanel = new JPanel(new BorderLayout(4, 0));
        northPanel.add(new JLabel("Class:"), BorderLayout.WEST);
        northPanel.add(panelFoo, BorderLayout.CENTER);
        northPanel.add(generateButton, BorderLayout.EAST);

        // Options panel
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;

        // Checkbox + hint
        gbc.gridy = 0;
        JPanel classIdPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        classIdPanel.add(checkBoxSymfonyIdClass);
        optionsPanel.add(classIdPanel, gbc);

        gbc.gridy = 1;
        JPanel classHintPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        classHintPanel.add(classHint);
        optionsPanel.add(classHintPanel, gbc);

        // Service name row (hidden when class as id is checked)
        gbc.gridy = 2; gbc.insets = JBUI.insetsTop(4);
        serviceNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel serviceNameLabel = new JLabel("Service name:");
        serviceNamePanel.add(serviceNameLabel);
        serviceNamePanel.add(textFieldServiceName);
        optionsPanel.add(serviceNamePanel, gbc);

        // Output section with separator
        gbc.gridy = 3; gbc.insets = JBUI.insetsTop(10);
        optionsPanel.add(new TitledSeparator("Output"), gbc);

        // Output format radios
        gbc.gridy = 4; gbc.insets = JBUI.insetsTop(2);
        JPanel outputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        outputRow.add(radioButtonOutYaml);
        outputRow.add(Box.createHorizontalStrut(12));
        outputRow.add(radioButtonOutXml);
        outputRow.add(Box.createHorizontalStrut(12));
        outputRow.add(radioButtonOutFluent);
        outputRow.add(Box.createHorizontalStrut(12));
        outputRow.add(radioButtonOutPhpArray);
        optionsPanel.add(outputRow, gbc);

        // Output editor wrapped in scroll pane so content doesn't push the layout
        JScrollPane editorScrollPane = new JScrollPane(editorOutput);
        editorScrollPane.setPreferredSize(new Dimension(500, 120));
        editorScrollPane.setMinimumSize(new Dimension(200, 80));
        gbc.gridy = 5; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0; gbc.insets = JBUI.insetsTop(4);
        optionsPanel.add(editorScrollPane, gbc);

        // Center section: table + options
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(tableViewPanel, BorderLayout.CENTER);
        centerPanel.add(optionsPanel, BorderLayout.SOUTH);

        // Bottom buttons
        JPanel buttonsPanel = new JPanel(new BorderLayout());
        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 5));
        leftButtons.add(buttonSettings);
        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 5));
        rightButtons.add(closeButton);
        rightButtons.add(buttonCopy);
        rightButtons.add(buttonInsert);
        buttonsPanel.add(leftButtons, BorderLayout.WEST);
        buttonsPanel.add(rightButtons, BorderLayout.EAST);

        content = new JPanel(new BorderLayout());
        content.add(northPanel, BorderLayout.NORTH);
        content.add(centerPanel, BorderLayout.CENTER);
        content.add(buttonsPanel, BorderLayout.SOUTH);

        panel1 = new JPanel(new BorderLayout());
        panel1.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel1.add(content, BorderLayout.CENTER);
    }

    private void initClassName() {
        if (this.classInit != null) {
            classCompletionPanelWrapper.setClassName(StringUtils.stripStart(this.classInit, "\\"));
            return;
        }

        try {
            Object data = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data instanceof String stringData) {
                if (stringData.length() <= 255 && stringData.matches("[_A-Za-z0-9\\\\]+")) {
                    classCompletionPanelWrapper.setClassName(stringData);
                }
            }
        } catch (UnsupportedFlavorException | IOException ignored) {
        }
    }

    private void insertYamlServiceTag() {
        if (!(this.psiFile instanceof YAMLFile)) {
            return;
        }

        String text = createServiceAsText(ServiceBuilder.OutputType.Yaml, this.psiFile);
        YAMLKeyValue fromText = YamlPsiElementFactory.createFromText(project, YAMLKeyValue.class, text);
        if (fromText == null) {
            return;
        }

        PsiElement psiElement = YamlHelper.insertKeyIntoFile((YAMLFile) psiFile, fromText, "services");
        if (psiElement != null) {
            navigateToElement(new TextRange[]{psiElement.getTextRange()});
        }

        dispose();
    }

    private void insertXmlServiceTag() {

        XmlFile psiFile1 = (XmlFile) SymfonyCreateService.this.psiFile;
        if (psiFile1 == null) {
            return;
        }

        final XmlTag rootTag = psiFile1.getRootTag();
        if (rootTag == null) {
            return;
        }

        final TextRange[] textRange = {null};
        WriteCommandAction.runWriteCommandAction(project, "Generate Service", null, () -> {

            XmlTag services = rootTag.findFirstSubTag("services");
            XmlElementFactory instance = XmlElementFactory.getInstance(SymfonyCreateService.this.project);

            if (services == null) {
                services = rootTag.addSubTag(instance.createTagFromText("<services/>", rootTag.getLanguage()), false);
            }

            XmlTag tag = instance.createTagFromText(createServiceAsText(ServiceBuilder.OutputType.XML).replace("\r\n", "\n").replace("\n", " "), services.getLanguage());

            textRange[0] = services.addSubTag(tag, false).getTextRange();
        });

        navigateToElement(textRange);

        dispose();
    }

    private void navigateToElement(TextRange[] textRange) {
        if (editor != null && textRange[0] != null) {
            editor.getCaretModel().moveToOffset(textRange[0].getStartOffset() + 1);
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
    }

    private String createServiceAsText(@NotNull ServiceBuilder.OutputType outputType, @NotNull PsiFile psiFile) {
        String className = StringUtils.stripStart(classCompletionPanelWrapper.getClassName(), "\\");
        String serviceName = textFieldServiceName.getText();

        List<MethodParameter.MethodModelParameter> items = new ArrayList<>(modelList.getItems());
        ServiceBuilder builder = new ServiceBuilder(items, psiFile, this.checkBoxSymfonyIdClass.isSelected());
        return builder.build(outputType, className, serviceName);
    }

    private String createServiceAsText(@NotNull ServiceBuilder.OutputType outputType) {
        String className = StringUtils.stripStart(classCompletionPanelWrapper.getClassName(), "\\");
        String serviceName = textFieldServiceName.getText();

        List<MethodParameter.MethodModelParameter> items = new ArrayList<>(modelList.getItems());
        ServiceBuilder builder = new ServiceBuilder(items, this.project, this.checkBoxSymfonyIdClass.isSelected());
        return builder.build(outputType, className, serviceName);
    }

    private void generateServiceDefinition() {
        String className = classCompletionPanelWrapper.getClassName();
        if (StringUtils.isBlank(className)) {
            return;
        }

        className = StringUtils.stripStart(className, "\\");

        // after cleanup class is empty
        if (StringUtils.isBlank(className)) {
            return;
        }

        ServiceBuilder.OutputType outputType = ServiceBuilder.OutputType.XML;
        if (radioButtonOutYaml.isSelected()) {
            outputType = ServiceBuilder.OutputType.Yaml;
        } else if (radioButtonOutFluent.isSelected()) {
            outputType = ServiceBuilder.OutputType.Fluent;
        } else if (radioButtonOutPhpArray.isSelected()) {
            outputType = ServiceBuilder.OutputType.PhpArray;
        }

        // save last selection
        Settings.getInstance(project).lastServiceGeneratorLanguage = outputType.toString().toLowerCase();

        final ServiceBuilder.OutputType finalOutputType = outputType;
        final FileType fileType = getFileType(outputType);
        ReadAction.nonBlocking(() -> createServiceAsText(finalOutputType))
            .finishOnUiThread(com.intellij.openapi.application.ModalityState.any(), text -> {
                if (text != null) {
                    editorOutput.setFileType(fileType);
                    editorOutput.setText(text);
                }
            })
            .submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService());
    }

    @NotNull
    private FileType getFileType(@NotNull ServiceBuilder.OutputType outputType) {
        return switch (outputType) {
            case XML -> XmlFileType.INSTANCE;
            case Fluent, PhpArray -> PhpFileType.INSTANCE;
            case Yaml -> YAMLFileType.YML;
        };
    }

    private void update() {
        String className = classCompletionPanelWrapper.getClassName();
        if (className.startsWith("\\")) {
            className = className.substring(1);
        }

        if (className.isEmpty()) {
            return;
        }

        final String finalClassName = className;

        ReadAction.nonBlocking(() -> {
            ServiceBuilder.ServiceDefinitionModel model = ServiceBuilder.createModel(project, finalClassName);
            if (model == null) {
                return null;
            }

            return new UpdateResult(model.getServiceName(), model.getModelParameters());
        })
        .finishOnUiThread(com.intellij.openapi.application.ModalityState.any(), result -> {
            if (result == null) {
                textFieldServiceName.setText("");
                return;
            }

            textFieldServiceName.setText(result.serviceName);

            while (this.modelList.getRowCount() > 0) {
                this.modelList.removeRow(0);
            }

            this.modelList.addRows(result.modelParameters);
            generateServiceDefinition();
        })
        .submit(com.intellij.util.concurrency.AppExecutorUtil.getAppExecutorService());
    }

    private record UpdateResult(String serviceName, List<MethodParameter.MethodModelParameter> modelParameters) {}

    private class IsServiceColumn extends ColumnInfo<MethodParameter.MethodModelParameter, Boolean> {

        public IsServiceColumn() {
            super("Act");
        }

        @Nullable
        @Override
        public Boolean valueOf(MethodParameter.MethodModelParameter modelParameter) {
            return modelParameter.isPossibleService();
        }

        public void setValue(MethodParameter.MethodModelParameter modelParameter, Boolean value) {
            if (value && modelParameter.getCurrentService() == null) {
                resolveServiceName(modelParameter);
            }

            modelParameter.setPossibleService(value);
            tableView.getListTableModel().fireTableDataChanged();
        }

        private void resolveServiceName(@NotNull MethodParameter.MethodModelParameter modelParameter) {
            modelParameter.setCurrentService(ServiceBuilder.resolveServiceName(project, modelParameter.getParameter(), serviceClass));
        }

        public Class<?> getColumnClass() {
            return Boolean.class;
        }

        public boolean isCellEditable(MethodParameter.MethodModelParameter modelParameter) {
            return true;
        }

        @Override
        public int getWidth(JTable table) {
            return 36;
        }

    }

    private class ServiceColumn extends ColumnInfo<MethodParameter.MethodModelParameter, String> {

        public ServiceColumn() {
            super("Service");
        }

        @Nullable
        @Override
        public String valueOf(MethodParameter.MethodModelParameter modelParameter) {
            return modelParameter.getCurrentService();
        }

        public void setValue(MethodParameter.MethodModelParameter modelParameter, String value) {
            modelParameter.setCurrentService(value);
            tableView.getListTableModel().fireTableDataChanged();
        }

        @Override
        public boolean isCellEditable(MethodParameter.MethodModelParameter modelParameter) {
            return true;
        }

        @Nullable
        @Override
        public TableCellEditor getEditor(MethodParameter.MethodModelParameter modelParameter) {

            Set<String> sorted = modelParameter.getPossibleServices();
            ComboBox<String> comboBox = new ComboBox<>(sorted.toArray(new String[0]), 200);
            comboBox.setEditable(true);

            return new DefaultCellEditor(comboBox);
        }
    }

    private static class NamespaceColumn extends ColumnInfo<MethodParameter.MethodModelParameter, String> {

        public NamespaceColumn() {
            super("Method");
        }

        @Nullable
        @Override
        public String valueOf(MethodParameter.MethodModelParameter modelParameter) {
            return modelParameter.getName();
        }
    }

    private static class IconColumn extends ColumnInfo<MethodParameter.MethodModelParameter, Icon> {
        public IconColumn() {
            super("");
        }

        @Nullable
        @Override
        public Icon valueOf(MethodParameter.MethodModelParameter modelParameter) {
            return modelParameter.getMethod().getIcon();
        }

        public Class<?> getColumnClass() {
            return ImageIcon.class;
        }

        @Override
        public int getWidth(JTable table) {
            return 32;
        }

    }

    private static class ParameterIndexColumn extends ColumnInfo<MethodParameter.MethodModelParameter, String> {

        public ParameterIndexColumn() {
            super("Parameter");
        }

        @Nullable
        @Override
        public String valueOf(MethodParameter.MethodModelParameter modelParameter) {
            return modelParameter.getParameter().getName();
        }
    }

    public static class ContainerServicePriorityNameComparator implements Comparator<ContainerService> {
        @Override
        public int compare(ContainerService o1, ContainerService o2) {

            if (ServiceContainerUtil.isLowerPriority(o1.getName()) && ServiceContainerUtil.isLowerPriority(o2.getName())) {
                return 0;
            }

            if (ServiceContainerUtil.isLowerPriority(o1.getName())) {
                return 1;
            }

            return -1;
        }
    }

    public static class ContainerServicePriorityWeakComparator implements Comparator<ContainerService> {
        @Override
        public int compare(ContainerService o1, ContainerService o2) {

            if (o1.isWeak() == o2.isWeak()) {
                return 0;
            }

            return (o1.isWeak() ? 1 : -1);
        }
    }

    private static SymfonyCreateService prepare(@Nullable Component component, @NotNull SymfonyCreateService service) {
        service.init();
        service.setTitle("Symfony: Service Generator");
        service.setIconImage(Symfony2Icons.getImage(Symfony2Icons.SYMFONY));
        service.pack();

        service.setMinimumSize(new Dimension(700, 500));
        service.setPreferredSize(new Dimension(800, 600));

        if (component != null) {
            service.setLocationRelativeTo(component);
        }

        // The invokeLater() method schedules the setVisible(true) call to run on the EDT asynchronously, which prevents the thread context conflict.
        // This is the standard approach for showing dialogs in IntelliJ plugins.
        ApplicationManager.getApplication().invokeLater(() -> service.setVisible(true));

        return service;
    }

    public static SymfonyCreateService create(@Nullable Component component, @NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor) {
        return prepare(component, new SymfonyCreateService(project, psiFile, editor));
    }

    public static SymfonyCreateService create(@Nullable Component component, @NotNull Project project, @NotNull PsiFile psiFile, @NotNull PhpClass phpClass, @Nullable Editor editor) {
        return prepare(component, new SymfonyCreateService(project, psiFile, editor, phpClass.getFQN()));
    }
}






