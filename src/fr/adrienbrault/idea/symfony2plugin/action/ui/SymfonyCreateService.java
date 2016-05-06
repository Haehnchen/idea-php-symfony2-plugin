package fr.adrienbrault.idea.symfony2plugin.action.ui;

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
import com.intellij.util.Consumer;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.DefaultServiceNameStrategy;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.JavascriptServiceNameStrategy;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.ServiceNameStrategyInterface;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.ServiceNameStrategyParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.ClassCompletionPanelWrapper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;
import java.util.List;

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

    private TableView<MethodParameter.MethodModelParameter> tableView;
    private ListTableModel<MethodParameter.MethodModelParameter> modelList;

    private Map<String, ContainerService> serviceClass;
    private Set<String> serviceSetComplete;

    private Project project;

    @Nullable
    private PsiFile psiFile;

    @Nullable
    private Editor editor;

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

    private static ServiceNameStrategyInterface[] NAME_STRATEGIES = new ServiceNameStrategyInterface[] {
        new JavascriptServiceNameStrategy(),
        new DefaultServiceNameStrategy(),
    };

    public void init() {

        setContentPane(panel1);
        setModal(true);

        this.classCompletionPanelWrapper = new ClassCompletionPanelWrapper(project, panelFoo, new Consumer<String>() {
            @Override
            public void consume(String s) {
                update();
            }
        });

        this.modelList = new ListTableModel<MethodParameter.MethodModelParameter>(
            new IconColumn(),
            new NamespaceColumn(),
            new ParameterIndexColumn(),
            new ServiceColumn(),
            new IsServiceColumn()
        );

        // default is xml
        radioButtonOutXml.setSelected(true);
        if(this.psiFile instanceof YAMLFile) {
            radioButtonOutYaml.setSelected(true);
        }


        this.tableView = new TableView<MethodParameter.MethodModelParameter>();
        this.tableView.setModelAndUpdateColumns(this.modelList);

        tableViewPanel.add(ToolbarDecorator.createDecorator(this.tableView)
            .disableAddAction()
            .disableDownAction()
            .disableRemoveAction()
            .disableUpDownActions()
            .createPanel()
        );

        this.serviceClass = ContainerCollectionResolver.getServices(project);

        this.serviceSetComplete = new TreeSet<String>();
        serviceSetComplete.add("");
        serviceSetComplete.addAll(this.serviceClass.keySet());

        //update();

        this.modelList.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                generateServiceDefinition();
            }

        });

        this.generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });


        this.closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnabled(false);
                dispose();
            }
        });

        this.buttonCopy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(StringUtils.isBlank(textAreaOutput.getText())) {
                    return;
                }

                StringSelection stringSelection = new StringSelection(textAreaOutput.getText());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard ();
                clipboard.setContents(stringSelection, null);
            }
        });

        this.buttonSettings.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SymfonyJavascriptServiceNameForm.create(SymfonyCreateService.this, project, classCompletionPanelWrapper.getClassName());
            }
        });

        initClassName();

        radioButtonOutXml.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                generateServiceDefinition();
            }
        });

        radioButtonOutYaml.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                generateServiceDefinition();
            }
        });

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

        // insert
        if(this.psiFile instanceof XmlFile || this.psiFile instanceof YAMLFile) {
            this.buttonInsert.setEnabled(true);
            this.buttonInsert.setVisible(true);

            this.buttonInsert.requestFocusInWindow();
            this.getRootPane().setDefaultButton(buttonInsert);

            this.buttonInsert.addActionListener(e -> {
                if(psiFile instanceof XmlFile) {
                    insertXmlServiceTag();
                } else if(psiFile instanceof YAMLFile) {
                    insertYamlServiceTag();
                }
            });

        } else {
            this.buttonInsert.setEnabled(false);
            this.buttonInsert.setVisible(false);
        }

    }

    private void initClassName() {
        if(this.classInit != null) {
            classCompletionPanelWrapper.setClassName(StringUtils.stripStart(this.classInit, "\\"));
            return;
        }

        try {
            String data = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            if(data != null && data.length() <= 255 && data.matches("[_A-Za-z0-9\\\\]+")) {
                classCompletionPanelWrapper.setClassName(data);
            }
        } catch (UnsupportedFlavorException | IOException ignored) {
        }
    }

    private void insertYamlServiceTag() {
        if(!(this.psiFile instanceof YAMLFile)) {
            return;
        }

        String text = createServiceAsText(ServiceBuilder.OutputType.Yaml);
        YAMLKeyValue fromText = YamlPsiElementFactory.createFromText(project, YAMLKeyValue.class, text);
        if(fromText == null) {
            return;
        }

        PsiElement psiElement = YamlHelper.insertKeyIntoFile((YAMLFile) psiFile, fromText, "services");
        if(psiElement != null) {
            navigateToElement(new TextRange[] {psiElement.getTextRange()});
        }

        dispose();
    }

    private void insertXmlServiceTag() {

        final XmlTag rootTag = ((XmlFile) SymfonyCreateService.this.psiFile).getRootTag();
        if(rootTag == null) {
            return;
        }

        final TextRange[] textRange = {null};
        new WriteCommandAction.Simple(project, "Generate Service", SymfonyCreateService.this.psiFile) {
            @Override
            protected void run() {

                XmlTag services = rootTag.findFirstSubTag("services");
                XmlElementFactory instance = XmlElementFactory.getInstance(SymfonyCreateService.this.project);

                if(services == null) {
                    services = rootTag.addSubTag(instance.createTagFromText("<services/>", rootTag.getLanguage()), false);
                }

                XmlTag tag = instance.createTagFromText(createServiceAsText(ServiceBuilder.OutputType.XML).replace("\r\n", "\n").replace("\n", " "), services.getLanguage());

                textRange[0] = services.addSubTag(tag, false).getTextRange();
            }
        }.execute();

        navigateToElement(textRange);

        dispose();
    }

    private void navigateToElement(TextRange[] textRange) {
        if(editor != null && textRange[0] != null) {
            editor.getCaretModel().moveToOffset(textRange[0].getStartOffset() + 1);
            editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        }
    }

    private String createServiceAsText(@NotNull ServiceBuilder.OutputType outputType) {
        return new ServiceBuilder(this.modelList.getItems(), this.project).build(
            outputType,
            StringUtils.stripStart(classCompletionPanelWrapper.getClassName(), "\\"),
            textFieldServiceName.getText()
        );
    }

    private void generateServiceDefinition() {

        String className = classCompletionPanelWrapper.getClassName();
        if(StringUtils.isBlank(className)) {
            return;
        }

        if(className.startsWith("\\")) {
            className = className.substring(1);
        }

        // after cleanup class is empty
        if(StringUtils.isBlank(className)) {
            return;
        }

        ServiceBuilder.OutputType outputType = ServiceBuilder.OutputType.XML;
        if(radioButtonOutYaml.isSelected()) {
            outputType = ServiceBuilder.OutputType.Yaml;
        }

        textAreaOutput.setText(createServiceAsText(outputType));
    }

    private void update() {
        ApplicationManager.getApplication().runReadAction(new Thread(new Runnable() {
            @Override
            public void run() {
                updateTask();
            }
        }));
    }

    private void updateTask() {

        String className = classCompletionPanelWrapper.getClassName();
        if(className.startsWith("\\")) {
            className = className.substring(1);
        }

        if(className.length() == 0) {
            return;
        }

        textFieldServiceName.setText("");

        PhpClass phpClass = PhpElementsUtil.getClass(project, className);
        if(phpClass == null) {
            return;
        }

        textFieldServiceName.setText(generateServiceName(className));

        List<MethodParameter.MethodModelParameter> modelParameters = new ArrayList<MethodParameter.MethodModelParameter>();

        for(Method method: phpClass.getMethods()) {
            if(method.getModifier().isPublic()) {
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < parameters.length; i++) {
                    Set<String> possibleServices = getPossibleServices(parameters[i]);
                    if(possibleServices.size() > 0) {
                        modelParameters.add(new MethodParameter.MethodModelParameter(method, parameters[i], i, possibleServices, getServiceName(possibleServices)));
                    } else {
                        modelParameters.add(new MethodParameter.MethodModelParameter(method, parameters[i], i, serviceSetComplete));
                    }

                }
            }

            method.getName();
        }

        Collections.sort(modelParameters, new Comparator<MethodParameter.MethodModelParameter>() {
            @Override
            public int compare(MethodParameter.MethodModelParameter o1, MethodParameter.MethodModelParameter o2) {
                int i = o1.getName().compareTo(o2.getName());
                if (i != 0) {
                    return i;
                }

                return Integer.valueOf(o1.getIndex()).compareTo(o2.getIndex());
            }
        });

        while(this.modelList.getRowCount() > 0) {
            this.modelList.removeRow(0);
        }

        this.modelList.addRows(modelParameters);
        generateServiceDefinition();
    }

    @Nullable
    private String getServiceName(Set<String> services) {
        if(services.size() == 0) {
            return null;
        }

        // we have a weight sorted Set, so first one
        return services.iterator().next();
    }

    @Override
    protected void dialogInit() {
        super.dialogInit();
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

        public void setValue(MethodParameter.MethodModelParameter modelParameter, Boolean value){
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

        public void setValue(MethodParameter.MethodModelParameter modelParameter, String value){
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
            ComboBox comboBox = new ComboBox(sorted.toArray(new String[sorted.size()] ), 200);
            comboBox.setEditable(true);

            return new DefaultCellEditor(comboBox);
        }
    }

    private class NamespaceColumn extends ColumnInfo<MethodParameter.MethodModelParameter, String> {

        public NamespaceColumn() {
            super("Method");
        }

        @Nullable
        @Override
        public String valueOf(MethodParameter.MethodModelParameter modelParameter) {
            return modelParameter.getName();
        }
    }

    private class IconColumn extends ColumnInfo<MethodParameter.MethodModelParameter, Icon> {
        public IconColumn() {
            super("");
        }

        @Nullable
        @Override
        public Icon valueOf(MethodParameter.MethodModelParameter modelParameter) {
            return modelParameter.getMethod().getIcon();
        }

        public java.lang.Class getColumnClass() {
            return ImageIcon.class;
        }

        @Override
        public int getWidth(JTable table) {
            return 32;
        }

    }

    private class ParameterIndexColumn extends ColumnInfo<MethodParameter.MethodModelParameter, String> {

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
        if(!(phpPsiElement instanceof ClassReference)) {
            return Collections.emptySet();
        }

        String type = ((ClassReference) phpPsiElement).getFQN();
        if(type == null) {
            return Collections.emptySet();
        }

        return ServiceActionUtil.getPossibleServices(project, type, serviceClass);
    }

    public static class ContainerServicePriorityNameComparator implements Comparator<ContainerService> {

        public static String[] LOWER_PRIORITY = new String[] { "debug", "default", "abstract", "inner", "chain", "decorate", "delegat"};

        @Override
        public int compare(ContainerService o1, ContainerService o2) {

            if(this.isLowerPriority(o1.getName()) && this.isLowerPriority(o2.getName())) {
                return 0;
            }

            if(this.isLowerPriority(o1.getName())) {
                return 1;
            }

            return -1;
        }

        private boolean isLowerPriority(String name) {

            for(String lowerName: LOWER_PRIORITY) {
                if(name.contains(lowerName)) {
                    return true;
                }
            }

            return false;
        }

    }

    public static class ContainerServicePriorityWeakComparator implements Comparator<ContainerService> {
        @Override
        public int compare(ContainerService o1, ContainerService o2) {

            if(o1.isWeak() == o2.isWeak()) {
                return 0;
            }

            return (o1.isWeak() ? 1 : -1);
        }
    }

    @NotNull
    private String generateServiceName(@NotNull String className) {

        // normalize
        if(className.startsWith("\\")) {
            className = className.substring(1);
        }

        ServiceNameStrategyParameter parameter = new ServiceNameStrategyParameter(project, className);
        for (ServiceNameStrategyInterface nameStrategy : NAME_STRATEGIES) {
            String serviceName = nameStrategy.getServiceName(parameter);
            if(serviceName != null && StringUtils.isNotBlank(serviceName)) {
                return serviceName;
            }
        }

        return className.toLowerCase().replace("\\", "_");
    }

    private static SymfonyCreateService prepare(@NotNull SymfonyCreateService service) {
        service.init();
        service.setTitle("Symfony: Service Generator");
        service.setIconImage(Symfony2Icons.getImage(Symfony2Icons.SYMFONY));
        service.pack();

        service.setMinimumSize(new Dimension(550, 250));
        service.setLocationRelativeTo(null);
        service.setVisible(true);

        return service;
    }

    public static SymfonyCreateService create(@NotNull Project project, @NotNull PsiFile psiFile, @Nullable Editor editor) {
        return prepare(new SymfonyCreateService(project, psiFile, editor));
    }

    public static SymfonyCreateService create(@NotNull Project project, @NotNull PsiFile psiFile, @NotNull PhpClass phpClass, @Nullable Editor editor) {
        return prepare(new SymfonyCreateService(project, psiFile, editor, phpClass.getFQN()));
    }
}






