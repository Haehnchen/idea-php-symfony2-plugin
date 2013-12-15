package fr.adrienbrault.idea.symfony2plugin.action.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class SymfonyCreateService extends JDialog {

    private JPanel panel1;
    private JPanel content;
    private JTextField textFieldClassName;
    private JPanel tableViewPanel;
    private JTextArea textAreaOutput;
    private JButton generateButton;
    private JButton buttonCopy;
    private JButton closeButton;

    private TableView<MethodParameter.MethodModelParameter> tableView;
    private ListTableModel<MethodParameter.MethodModelParameter> modelList;

    private Map<String, String> serviceClass;
    private Set<String> serviceSetComplete;

    private Project project;
    private PsiFile psiFile;

    public SymfonyCreateService(Project project, PsiFile psiFile) {
        this.project = project;
        this.psiFile = psiFile;

        setContentPane(panel1);
        setModal(true);

        this.modelList = new ListTableModel<MethodParameter.MethodModelParameter>(
            new IconColumn(),
            new NamespaceColumn(),
            new ParameterIndexColumn(),
            new ServiceColumn(),
            new IsServiceColumn()
        );

        this.tableView = new TableView<MethodParameter.MethodModelParameter>();
        this.tableView.setModelAndUpdateColumns(this.modelList);

        tableViewPanel.add(ToolbarDecorator.createDecorator(this.tableView)
            .disableAddAction()
            .disableDownAction()
            .disableRemoveAction()
            .disableUpDownActions()
            .createPanel()
        );

        this.serviceClass = ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap();

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

    }

    private void generateServiceDefinition() {
        textAreaOutput.setText(new ServiceBuilder(this.modelList.getItems()).build(
            this.psiFile instanceof XmlFile ? ServiceBuilder.OutputType.XML : ServiceBuilder.OutputType.Yaml,
            textFieldClassName.getText())
        );
    }

    private void update() {

        String className = textFieldClassName.getText();
        if(className.startsWith("\\")) {
            className = className.substring(1);
        }

        if(className.length() == 0) {
            return;
        }

        PhpClass phpClass = PhpElementsUtil.getClass(project, className);
        if(phpClass == null) {
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

        // @TODO:  better name match debug/default name should have lower weight
        if(services.size() > 0) {
            return services.iterator().next();
        }

        return null;
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
            super("Method");
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

        Set<String> possibleServices = new TreeSet<String>();

        PhpPsiElement phpPsiElement = parameter.getFirstPsiChild();
        if(phpPsiElement instanceof ClassReference) {

            String type = ((ClassReference) phpPsiElement).getFQN();
            PhpClass typeClass = PhpElementsUtil.getClassInterface(project, type);
            if(typeClass != null) {
                for(Map.Entry<String, String> service: serviceClass.entrySet()) {

                    PhpClass serviceClass = PhpElementsUtil.getClass(project, service.getValue());
                    if(serviceClass != null) {
                        if(new Symfony2InterfacesUtil().isInstanceOf(serviceClass, typeClass)) {
                            possibleServices.add(service.getKey());
                        }
                    }

                }
            }

        }

        return possibleServices;
    }


}






