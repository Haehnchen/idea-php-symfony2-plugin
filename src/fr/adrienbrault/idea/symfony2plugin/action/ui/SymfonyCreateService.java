package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.psi.PsiFile;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.DefaultServiceNameStrategy;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.JavascriptServiceNameStrategy;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.ServiceNameStrategyInterface;
import fr.adrienbrault.idea.symfony2plugin.action.generator.naming.ServiceNameStrategyParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

public class SymfonyCreateService extends JDialog {

    private JPanel panel1;
    private JPanel content;
    private JTextField textFieldClassName;
    private JPanel tableViewPanel;
    private JTextArea textAreaOutput;
    private JButton generateButton;
    private JButton buttonCopy;
    private JButton closeButton;

    private JRadioButton radioButtonOutXml;
    private JRadioButton radioButtonOutYaml;
    private JTextField textFieldServiceName;

    private TableView<MethodParameter.MethodModelParameter> tableView;
    private ListTableModel<MethodParameter.MethodModelParameter> modelList;

    private Map<String, ContainerService> serviceClass;
    private Set<String> serviceSetComplete;

    private Project project;
    private String className;

    @Nullable
    private PsiFile psiFile;

    public SymfonyCreateService(Project project, String className) {
        this(project, (PsiFile) null);
        this.className = className;
    }

    public SymfonyCreateService(Project project, @Nullable PsiFile psiFile) {
        this.project = project;
        this.psiFile = psiFile;
    }

    private static ServiceNameStrategyInterface[] NAME_STRATEGIES = new ServiceNameStrategyInterface[] {
        new JavascriptServiceNameStrategy(),
        new DefaultServiceNameStrategy(),
    };

    public void init() {

        setContentPane(panel1);
        setModal(true);

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

        if(this.className != null) {
            this.textFieldClassName.setText(this.className);
            update();
        }

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

    }

    private void generateServiceDefinition() {

        if(StringUtils.isBlank(textFieldClassName.getText())) {
            return;
        }

        ServiceBuilder.OutputType outputType = ServiceBuilder.OutputType.XML;
        if(radioButtonOutYaml.isSelected()) {
            outputType = ServiceBuilder.OutputType.Yaml;
        }

        textAreaOutput.setText(new ServiceBuilder(this.modelList.getItems(), this.project).build(
            outputType,
            textFieldClassName.getText(),
            textFieldServiceName.getText()
        ));
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

        String className = textFieldClassName.getText();
        if(className.startsWith("\\")) {
            className = className.substring(1);
        }

        if(className.length() == 0) {
            return;
        }

        textFieldServiceName.setText(generateServiceName(className));

        PhpClass phpClass = PhpElementsUtil.getClass(project, className);
        if(phpClass == null) {
            JOptionPane.showMessageDialog(null, String.format("invalid %s class", className));
            return;
        }

        ArrayList<MethodParameter.MethodModelParameter> modelParameters = new ArrayList<MethodParameter.MethodModelParameter>();

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

    public void setClassName(String className) {
        this.className = className;
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

        Set<String> possibleServices = new LinkedHashSet<String>();
        List<ContainerService> matchedContainer = new ArrayList<ContainerService>();

        PhpPsiElement phpPsiElement = parameter.getFirstPsiChild();
        if(!(phpPsiElement instanceof ClassReference)) {
            return possibleServices;
        }

        String type = ((ClassReference) phpPsiElement).getFQN();
        if(type != null) {
            PhpClass typeClass = PhpElementsUtil.getClassInterface(project, type);
            if(typeClass != null) {
                for(Map.Entry<String, ContainerService> entry: serviceClass.entrySet()) {
                    if(entry.getValue().getClassName() != null) {
                        PhpClass serviceClass = PhpElementsUtil.getClassInterface(project, entry.getValue().getClassName());
                        if(serviceClass != null) {
                            if(new Symfony2InterfacesUtil().isInstanceOf(serviceClass, typeClass)) {
                                matchedContainer.add(entry.getValue());
                            }
                        }
                    }

                }
            }
        }

        if(matchedContainer.size() > 0) {

            // weak service have lower priority
            Collections.sort(matchedContainer, new ContainerServicePriorityWeakComparator());

            // lower priority of services like "doctrine.orm.default_entity_manager"
            Collections.sort(matchedContainer, new ContainerServicePriorityNameComparator());

            for(ContainerService containerService: matchedContainer) {
                possibleServices.add(containerService.getName());
            }

        }



        return possibleServices;
    }


    public static class ContainerServicePriorityNameComparator implements Comparator<ContainerService> {

        private static String[] LOWER_PRIORITY = new String[] { "debug", "default", "abstract"};

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

}






