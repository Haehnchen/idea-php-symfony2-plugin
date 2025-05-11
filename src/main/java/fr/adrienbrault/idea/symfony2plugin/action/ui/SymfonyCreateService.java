package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.intellij.concurrency.ThreadContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jetbrains.php.lang.psi.elements.*;
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
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.TableCellEditor;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCreateService extends JDialog {

    private JPanel panel1;
    private JPanel content;
    private JPanel tableViewPanel;
    private JTextArea textAreaOutput;
    private JButton generateButton;
    private JButton buttonCopy;
    private JButton closeButton;

    private JRadioButton radioButtonOutXml;
    private JRadioButton radioButtonOutYaml;
    private JTextField textFieldServiceName;
    private JButton buttonSettings;
    private JButton buttonInsert;
    private JPanel panelFoo;
    private JCheckBox checkBoxSymfonyIdClass;

    private TableView<MethodParameter.MethodModelParameter> tableView;
    private ListTableModel<MethodParameter.MethodModelParameter> modelList;

    private Map<String, ContainerService> serviceClass;
    private Set<String> serviceSetComplete;

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
        }

        // overwrite language output on direct file context
        if (this.psiFile instanceof YAMLFile) {
            radioButtonOutYaml.setSelected(true);
        } else if (this.psiFile instanceof XmlFile) {
            radioButtonOutXml.setSelected(true);
        }

        // lets use yaml as default
        if (!radioButtonOutYaml.isSelected() && !radioButtonOutXml.isSelected()) {
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

        this.serviceSetComplete = new TreeSet<>();
        serviceSetComplete.add("");
        serviceSetComplete.addAll(this.serviceClass.keySet());

        //update();

        this.modelList.addTableModelListener(e -> generateServiceDefinition());

        this.generateButton.addActionListener(e -> update());

        this.checkBoxSymfonyIdClass.setSelected(Settings.getInstance(project).serviceClassAsIdAttribute);
        this.checkBoxSymfonyIdClass.addItemListener(e -> {
            Settings.getInstance(project).serviceClassAsIdAttribute = checkBoxSymfonyIdClass.isSelected();
            generateServiceDefinition();
        });

        this.closeButton.addActionListener(e -> {
            setEnabled(false);
            dispose();
        });

        this.buttonCopy.addActionListener(e -> {
            if (StringUtils.isBlank(textAreaOutput.getText())) {
                return;
            }

            StringSelection stringSelection = new StringSelection(textAreaOutput.getText());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stringSelection, null);
        });

        this.buttonSettings.addActionListener(e -> SymfonyJavascriptServiceNameForm.create(SymfonyCreateService.this, project, classCompletionPanelWrapper.getClassName()));

        initClassName();

        radioButtonOutXml.addChangeListener(e -> generateServiceDefinition());

        radioButtonOutYaml.addChangeListener(e -> generateServiceDefinition());

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

    private void initClassName() {
        if (this.classInit != null) {
            classCompletionPanelWrapper.setClassName(StringUtils.stripStart(this.classInit, "\\"));
            return;
        }

        try {
            String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (data != null && data.length() <= 255 && data.matches("[_A-Za-z0-9\\\\]+")) {
                classCompletionPanelWrapper.setClassName(data);
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

        final XmlTag rootTag = ((XmlFile) SymfonyCreateService.this.psiFile).getRootTag();
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
        return new ServiceBuilder(this.modelList.getItems(), psiFile, this.checkBoxSymfonyIdClass.isSelected()).build(
            outputType,
            StringUtils.stripStart(classCompletionPanelWrapper.getClassName(), "\\"),
            textFieldServiceName.getText()
        );
    }

    private String createServiceAsText(@NotNull ServiceBuilder.OutputType outputType) {
        return new ServiceBuilder(this.modelList.getItems(), this.project, this.checkBoxSymfonyIdClass.isSelected()).build(
            outputType,
            StringUtils.stripStart(classCompletionPanelWrapper.getClassName(), "\\"),
            textFieldServiceName.getText()
        );
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
        }

        // save last selection
        Settings.getInstance(project).lastServiceGeneratorLanguage = outputType.toString().toLowerCase();

        textAreaOutput.setText(createServiceAsText(outputType));
    }

    private void update() {
        ApplicationManager.getApplication().runReadAction(new Thread(this::updateTask));
    }

    private void updateTask() {

        String className = classCompletionPanelWrapper.getClassName();
        if (className.startsWith("\\")) {
            className = className.substring(1);
        }

        if (className.isEmpty()) {
            return;
        }

        textFieldServiceName.setText("");

        PhpClass phpClass = PhpElementsUtil.getClass(project, className);
        if (phpClass == null) {
            return;
        }

        textFieldServiceName.setText(ServiceUtil.getServiceNameForClass(project, className));

        List<MethodParameter.MethodModelParameter> modelParameters = new ArrayList<>();

        for (Method method : phpClass.getMethods()) {
            if (method.getModifier().isPublic()) {
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Set<String> possibleServices = getPossibleServices(parameters[i]);
                    if (!possibleServices.isEmpty()) {
                        modelParameters.add(new MethodParameter.MethodModelParameter(method, parameters[i], i, possibleServices, getServiceName(possibleServices)));
                    } else {
                        modelParameters.add(new MethodParameter.MethodModelParameter(method, parameters[i], i, serviceSetComplete));
                    }

                }
            }

            method.getName();
        }

        modelParameters.sort(
            Comparator
                .comparing(MethodParameter.MethodModelParameter::getName)
                .thenComparingInt(MethodParameter.MethodModelParameter::getIndex)
        );

        while (this.modelList.getRowCount() > 0) {
            this.modelList.removeRow(0);
        }

        this.modelList.addRows(modelParameters);
        generateServiceDefinition();
    }

    @Nullable
    private String getServiceName(Set<String> services) {
        if (services.isEmpty()) {
            return null;
        }

        // we have a weight sorted Set, so first one
        return services.iterator().next();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(5, 5, 5, 5), -1, -1));
        content = new JPanel();
        content.setLayout(new BorderLayout(0, 0));
        panel1.add(content, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 5, new Insets(5, 0, 0, 0), -1, -1));
        content.add(panel2, BorderLayout.SOUTH);
        buttonCopy = new JButton();
        buttonCopy.setActionCommand("To Clipboard");
        buttonCopy.setText("To Clipboard");
        panel2.add(buttonCopy, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        closeButton = new JButton();
        closeButton.setText("Close");
        panel2.add(closeButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonSettings = new JButton();
        buttonSettings.setText("Settings");
        panel2.add(buttonSettings, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonInsert = new JButton();
        buttonInsert.setText("Insert");
        panel2.add(buttonInsert, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        content.add(panel3, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setText("Class:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateButton = new JButton();
        generateButton.setText("Generate");
        panel3.add(generateButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panelFoo = new JPanel();
        panelFoo.setLayout(new GridBagLayout());
        panel3.add(panelFoo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        content.add(panel4, BorderLayout.CENTER);
        tableViewPanel = new JPanel();
        tableViewPanel.setLayout(new BorderLayout(0, 0));
        panel4.add(tableViewPanel, BorderLayout.CENTER);
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(5, 0, 0, 0), -1, -1));
        panel4.add(panel5, BorderLayout.SOUTH);
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel6.add(panel7, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        radioButtonOutXml = new JRadioButton();
        radioButtonOutXml.setText("XML");
        panel7.add(radioButtonOutXml, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        radioButtonOutYaml = new JRadioButton();
        radioButtonOutYaml.setText("Yaml");
        panel7.add(radioButtonOutYaml, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textFieldServiceName = new JTextField();
        panel7.add(textFieldServiceName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        checkBoxSymfonyIdClass = new JCheckBox();
        checkBoxSymfonyIdClass.setText("Class as id (Symfony 3.3)");
        panel6.add(checkBoxSymfonyIdClass, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel5.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textAreaOutput = new JTextArea();
        textAreaOutput.setEditable(false);
        Font textAreaOutputFont = this.$$$getFont$$$("Courier New", -1, -1, textAreaOutput.getFont());
        if (textAreaOutputFont != null) textAreaOutput.setFont(textAreaOutputFont);
        textAreaOutput.setLineWrap(true);
        textAreaOutput.setRows(7);
        textAreaOutput.setText("");
        scrollPane1.setViewportView(textAreaOutput);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(radioButtonOutYaml);
        buttonGroup.add(radioButtonOutXml);
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

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
            modelParameter.setPossibleService(value);
            tableView.getListTableModel().fireTableDataChanged();
        }

        public Class getColumnClass() {
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
            ComboBox comboBox = new ComboBox(sorted.toArray(new String[0]), 200);
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

        public Class getColumnClass() {
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

    private Set<String> getPossibleServices(Parameter parameter) {

        PhpPsiElement phpPsiElement = parameter.getFirstPsiChild();
        if (!(phpPsiElement instanceof ClassReference)) {
            return Collections.emptySet();
        }

        String type = ((ClassReference) phpPsiElement).getFQN();
        if (type == null) {
            return Collections.emptySet();
        }

        return ServiceActionUtil.getPossibleServices(project, type, serviceClass);
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

        service.setMinimumSize(new Dimension(550, 250));

        if (component != null) {
            service.setLocationRelativeTo(component);
        }

        // https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html#read-access
        // fix: "Thread context was already set: com.intellij.openapi.actionSystem.ex.ActionContextElement"
        // https://intellij-support.jetbrains.com/hc/en-us/community/posts/14397678486418--Thread-context-was-already-set-CoroutineName-commit-workflow-when-showing-window-during-commit-check?page=1#community_comment_15082225353874
        try (var ignored = ThreadContext.resetThreadContext()) {
            service.setVisible(true);
        }

        return service;
    }

    public static SymfonyCreateService create(@Nullable Component component, @NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor) {
        return prepare(component, new SymfonyCreateService(project, psiFile, editor));
    }

    public static SymfonyCreateService create(@Nullable Component component, @NotNull Project project, @NotNull PsiFile psiFile, @NotNull PhpClass phpClass, @Nullable Editor editor) {
        return prepare(component, new SymfonyCreateService(project, psiFile, editor, phpClass.getFQN()));
    }
}






