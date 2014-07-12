package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.AssistantReferenceUtil;
import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class MethodParameterReferenceSettingsForm  implements Configurable {
    private JPanel panel1;
    private JPanel panelConfigTableView;
    private JButton buttonHelp;

    private TableView<MethodParameterSetting> tableView;
    private Project project;
    private boolean changed = false;
    private ListTableModel<MethodParameterSetting> modelList;

    public MethodParameterReferenceSettingsForm(Project project) {
        this.project = project;

        this.tableView = new TableView<MethodParameterSetting>();
        this.modelList = new ListTableModel<MethodParameterSetting>(
            new CallToColumn(),
            new MethodColumn(),
            new IndexColumn(),
            new ProviderColumn(),
            new ContributorColumn(),
            new ContributorDataColumn()
        );

        this.attachItems();

        this.tableView.setModelAndUpdateColumns(this.modelList);
        this.tableView.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                MethodParameterReferenceSettingsForm.this.changed = true;
            }
        });

        buttonHelp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                IdeHelper.openUrl(Symfony2ProjectComponent.HELP_URL + "extension/method_parameter.html");
            }
        });

    }

    private void attachItems() {
        for (MethodParameterSetting MethodParameterSetting : AssistantReferenceUtil.getMethodsParameterSettings(this.project)) {
            this.modelList.addRow(MethodParameterSetting);
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return null;
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ToolbarDecorator tablePanel = ToolbarDecorator.createDecorator(this.tableView, new ElementProducer<MethodParameterSetting>() {
            @Override
            public MethodParameterSetting createElement() {
                //IdeFocusManager.getInstance(TwigSettingsForm.this.project).requestFocus(TwigNamespaceDialog.getWindows(), true);
                return null;
            }

            @Override
            public boolean canCreateElement() {
                return true;
            }
        });

        tablePanel.setEditAction(new AnActionButtonRunnable() {
            @Override
            public void run(AnActionButton anActionButton) {
                MethodParameterReferenceSettingsForm.this.openTwigPathDialog(MethodParameterReferenceSettingsForm.this.tableView.getSelectedObject());
            }
        });


        tablePanel.setAddAction(new AnActionButtonRunnable() {
            @Override
            public void run(AnActionButton anActionButton) {
                MethodParameterReferenceSettingsForm.this.openTwigPathDialog(null);
            }
        });

        tablePanel.disableUpAction();
        tablePanel.disableDownAction();

        this.panelConfigTableView.add(tablePanel.createPanel());

        return this.panel1;
    }

    @Override
    public boolean isModified() {
        return this.changed;
    }

    @Override
    public void apply() throws ConfigurationException {
        List<MethodParameterSetting> methodParameterSettings = new ArrayList<MethodParameterSetting>();

        for(MethodParameterSetting methodParameterSetting :this.tableView.getListTableModel().getItems()) {
            methodParameterSettings.add(methodParameterSetting);
        }

        getSettings().methodParameterSettings = methodParameterSettings;
        this.changed = false;
    }

    private Settings getSettings() {
        return Settings.getInstance(this.project);
    }

    private void resetList() {
        // clear list, easier?
        while(this.modelList.getRowCount() > 0) {
            this.modelList.removeRow(0);
        }

    }

    @Override
    public void reset() {
        this.resetList();
        this.attachItems();
        this.changed = false;
    }

    @Override
    public void disposeUIResources() {

    }

    private class CallToColumn extends ColumnInfo<MethodParameterSetting, String> {

        public CallToColumn() {
            super("CallTo");
        }

        @Nullable
        @Override
        public String valueOf(MethodParameterSetting methodParameterSetting) {
            return methodParameterSetting.getCallTo();
        }
    }

    private class MethodColumn extends ColumnInfo<MethodParameterSetting, String> {

        public MethodColumn() {
            super("Method");
        }

        @Nullable
        @Override
        public String valueOf(MethodParameterSetting methodParameterSetting) {
            return methodParameterSetting.getMethodName();
        }
    }

    private class IndexColumn extends ColumnInfo<MethodParameterSetting, Integer> {

        public IndexColumn() {
            super("Index");
        }

        @Nullable
        @Override
        public Integer valueOf(MethodParameterSetting methodParameterSetting) {
            return methodParameterSetting.getIndexParameter();
        }
    }

    private class ProviderColumn extends ColumnInfo<MethodParameterSetting, String> {

        public ProviderColumn() {
            super("Provider");
        }

        @Nullable
        @Override
        public String valueOf(MethodParameterSetting methodParameterSetting) {
            return methodParameterSetting.getReferenceProviderName();
        }
    }

    private class ContributorColumn extends ColumnInfo<MethodParameterSetting, String> {

        public ContributorColumn() {
            super("Contributor");
        }

        @Nullable
        @Override
        public String valueOf(MethodParameterSetting methodParameterSetting) {
            return methodParameterSetting.getContributorName();
        }
    }

    private class ContributorDataColumn extends ColumnInfo<MethodParameterSetting, String> {

        public ContributorDataColumn() {
            super("ContributorData");
        }

        @Nullable
        @Override
        public String valueOf(MethodParameterSetting methodParameterSetting) {
            return methodParameterSetting.getContributorData();
        }
    }

    private void openTwigPathDialog(@Nullable MethodParameterSetting methodParameterSetting) {
        MethodParameterDialog twigNamespaceDialog;
        if(methodParameterSetting == null) {
            twigNamespaceDialog = new MethodParameterDialog(project, this.tableView);
        } else {
            twigNamespaceDialog = new MethodParameterDialog(project, this.tableView, methodParameterSetting);
        }

        Dimension dim = new Dimension();
        dim.setSize(500, 190);
        twigNamespaceDialog.setTitle("MethodParameterSetting");
        twigNamespaceDialog.setMinimumSize(dim);
        twigNamespaceDialog.pack();
        twigNamespaceDialog.setLocationRelativeTo(this.panel1);

        twigNamespaceDialog.setVisible(true);
    }

}
