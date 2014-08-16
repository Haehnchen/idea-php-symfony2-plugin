package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.AnActionButtonUpdater;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TwigSettingsForm implements Configurable {

    private JPanel panel1;
    private JPanel panelTableView;
    private JButton resetToDefault;
    private TableView<TwigPath> tableView;
    private Project project;
    private boolean changed = false;
    private ListTableModel<TwigPath> modelList;

    public TwigSettingsForm(@NotNull Project project) {

        this.project = project;

        this.tableView = new TableView<TwigPath>();
        this.modelList = new ListTableModel<TwigPath>(
            new NamespaceColumn(),
            new PathColumn(project),
            new TypeColumn(),
            new CustomColumn(),
            new DisableColumn()
        );

        this.attachItems(true);

        this.tableView.setModelAndUpdateColumns(this.modelList);

        this.modelList.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                TwigSettingsForm.this.changed = true;
            }
        });

        resetToDefault.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                TwigSettingsForm.this.resetList();

                List<TwigPath> sortableLookupItems = new ArrayList<TwigPath>();
                sortableLookupItems.addAll(TwigHelper.getTwigNamespaces(TwigSettingsForm.this.project, false));
                Collections.sort(sortableLookupItems);

                for (TwigPath twigPath : sortableLookupItems) {
                    // dont use managed class here
                    // @TODO state to enabled (should not be here)
                    TwigSettingsForm.this.modelList.addRow(twigPath.clone().setEnabled(true));
                }
            }
        });
    }

    private void attachItems(boolean includeSettings) {

        // dont load on project less context
        if(this.project == null) {
            return;
        }
        
        List<TwigPath> sortableLookupItems = new ArrayList<TwigPath>();
        sortableLookupItems.addAll(TwigHelper.getTwigNamespaces(this.project, includeSettings));
        Collections.sort(sortableLookupItems);

        for (TwigPath twigPath : sortableLookupItems) {
            // dont use managed class here
            this.modelList.addRow(twigPath.clone());
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Twig";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ToolbarDecorator tablePanel = ToolbarDecorator.createDecorator(this.tableView, new ElementProducer<TwigPath>() {
            @Override
            public TwigPath createElement() {
                //IdeFocusManager.getInstance(TwigSettingsForm.this.project).requestFocus(TwigNamespaceDialog.getWindows(), true);
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean canCreateElement() {
                return true;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        tablePanel.setEditAction(new AnActionButtonRunnable() {
            @Override
            public void run(AnActionButton anActionButton) {
                TwigSettingsForm.this.openTwigPathDialog(TwigSettingsForm.this.tableView.getSelectedObject());
            }
        });


        tablePanel.setAddAction(new AnActionButtonRunnable() {
            @Override
            public void run(AnActionButton anActionButton) {
               TwigSettingsForm.this.openTwigPathDialog(null);
            }
        });

        tablePanel.setEditActionUpdater(new AnActionButtonUpdater() {
            @Override
            public boolean isEnabled(AnActionEvent e) {
                TwigPath twigPath = TwigSettingsForm.this.tableView.getSelectedObject();
                return twigPath != null && twigPath.isCustomPath();
            }
        });

        tablePanel.setRemoveActionUpdater(new AnActionButtonUpdater() {
            @Override
            public boolean isEnabled(AnActionEvent e) {
                TwigPath twigPath = TwigSettingsForm.this.tableView.getSelectedObject();
                return twigPath != null && twigPath.isCustomPath();
            }
        });

        tablePanel.disableUpAction();
        tablePanel.disableDownAction();

        this.panelTableView.add(tablePanel.createPanel());

        return this.panel1;
    }

    @Override
    public boolean isModified() {
        return this.changed;
    }

    @Override
    public void apply() throws ConfigurationException {
        List<TwigNamespaceSetting> twigPaths = new ArrayList<TwigNamespaceSetting>();

        for(TwigPath twigPath :this.tableView.getListTableModel().getItems()) {
            // only custom and disabled path need to save
            if((!twigPath.isEnabled() && twigPath.getRelativePath(this.project) != null) || twigPath.isCustomPath()) {
                twigPaths.add(new TwigNamespaceSetting(twigPath.getNamespace(), twigPath.getRelativePath(this.project), twigPath.isEnabled(), twigPath.getNamespaceType(), twigPath.isCustomPath()));
            }
        }

        getSettings().twigNamespaces = twigPaths;
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
        this.attachItems(true);
        this.changed = false;
    }

    @Override
    public void disposeUIResources() {
        this.resetList();
    }

    private class NamespaceColumn extends ColumnInfo<TwigPath, String> {

        public NamespaceColumn() {
            super("Namespace");
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
             return twigPath.getNamespace();
        }
    }

    private class PathColumn extends ColumnInfo<TwigPath, String> {

        private Project project;

        public PathColumn(Project project) {
            super("Path");
            this.project = project;
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.getRelativePath(this.project);
        }
    }

    private class CustomColumn extends ColumnInfo<TwigPath, String> {

        public CustomColumn() {
            super("Parser");
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.isCustomPath() ? "Custom" : "Internal";
        }
    }

    private class TypeColumn extends ColumnInfo<TwigPath, String> {

        public TypeColumn() {
            super("Type");
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.getNamespaceType().toString();
        }
    }

    private abstract class BooleanColumn extends ColumnInfo<TwigPath, Boolean>
    {
        public BooleanColumn(String name) {
            super(name);
        }

        public boolean isCellEditable(TwigPath groupItem)
        {
            return true;
        }

        public Class getColumnClass()
        {
            return Boolean.class;
        }
    }

    private class DisableColumn extends BooleanColumn {

        public DisableColumn() {
            super("on");
        }

        public Boolean valueOf(TwigPath twigPath) {
            return twigPath.isEnabled();
        }

        public void setValue(TwigPath twigPath, Boolean value){
            twigPath.setEnabled(value);
            TwigSettingsForm.this.tableView.getListTableModel().fireTableDataChanged();
        }

        public int getWidth(JTable table) {
            return 50;
        }

    }

    private void openTwigPathDialog(@Nullable TwigPath twigPath) {
        TwigNamespaceDialog twigNamespaceDialog;
        if(twigPath == null) {
            twigNamespaceDialog = new TwigNamespaceDialog(project, this.tableView);
        } else {
            twigNamespaceDialog = new TwigNamespaceDialog(project, this.tableView, twigPath);
        }

        Dimension dim = new Dimension();
        dim.setSize(500, 190);
        twigNamespaceDialog.setTitle("Twig Namespace");
        twigNamespaceDialog.setMinimumSize(dim);
        twigNamespaceDialog.pack();
        twigNamespaceDialog.setLocationRelativeTo(TwigSettingsForm.this.panel1);

        twigNamespaceDialog.setVisible(true);
    }

}
