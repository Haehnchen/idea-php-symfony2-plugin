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
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.routing.dict.RoutingFile;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class RoutingSettingsForm implements Configurable {

    private JPanel panel1;
    private JPanel listviewPanel;
    private JButton buttonReset;
    private TableView<RoutingFile> tableView;
    private Project project;
    private boolean changed = false;
    private ListTableModel<RoutingFile> modelList;

    public RoutingSettingsForm(@NotNull Project project) {

        this.project = project;
        this.tableView = new TableView<RoutingFile>();

        this.modelList = new ListTableModel<RoutingFile>(
            new PathColumn(project),
            new ExistsColumn(project)
        );

        this.initList();

        this.modelList.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                RoutingSettingsForm.this.changed = true;
            }
        });

        this.tableView.setModelAndUpdateColumns(this.modelList);

        buttonReset.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                resetList();

                // add default path
                for (String defaultContainerPath : Settings.DEFAULT_CONTAINER_PATHS) {
                    RoutingSettingsForm.this.modelList.addRow(new RoutingFile(defaultContainerPath));
                }

            }
        });
    }

    private void initList() {
        List<RoutingFile> containerFiles = getSettings().routingFiles;
        if(containerFiles != null && containerFiles.size() > 0) {
            this.modelList.addRows(containerFiles);
        }
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Container";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ToolbarDecorator tablePanel = ToolbarDecorator.createDecorator(this.tableView, new ElementProducer<RoutingFile>() {
            @Override
            public RoutingFile createElement() {
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
                RoutingFile containerFile = RoutingSettingsForm.this.tableView.getSelectedObject();
                if(containerFile == null) {
                    return;
                }

                String uri = UiSettingsUtil.getPathDialog(project, PhpFileType.INSTANCE);
                if(uri == null) {
                    return;
                }

                containerFile.setPath(uri);
                RoutingSettingsForm.this.changed = true;
            }
        });

        tablePanel.setAddAction(new AnActionButtonRunnable() {
            @Override
            public void run(AnActionButton anActionButton) {
                String uri = UiSettingsUtil.getPathDialog(project, PhpFileType.INSTANCE);
                if(uri == null) {
                    return;
                }

                RoutingSettingsForm.this.tableView.getListTableModel().addRow(new RoutingFile(uri));
                RoutingSettingsForm.this.changed = true;
            }
        });

        this.panel1.add(tablePanel.createPanel());

        return this.panel1;
    }

    @Override
    public boolean isModified() {
        return this.changed;
    }

    @Override
    public void apply() throws ConfigurationException {
        List<RoutingFile> containerFiles = new ArrayList<RoutingFile>();

        for(RoutingFile containerFile :this.tableView.getListTableModel().getItems()) {
            containerFiles.add(new RoutingFile(containerFile.getPath()));
        }

        getSettings().routingFiles = containerFiles;
        this.changed = false;
    }

    private Settings getSettings() {
        return Settings.getInstance(this.project);
    }

    @Override
    public void reset() {
        this.resetList();
        this.initList();
        this.changed = false;
    }

    private void resetList() {
        // clear list, easier?
        while(this.modelList.getRowCount() > 0) {
            this.modelList.removeRow(0);
        }
    }

    @Override
    public void disposeUIResources() {
    }

    private class PathColumn extends ColumnInfo<RoutingFile, String> {

        private Project project;

        public PathColumn(Project project) {
            super("Path");
            this.project = project;
        }

        @Nullable
        @Override
        public String valueOf(RoutingFile containerFile) {
            return containerFile.getPath();
        }
    }

    private class ExistsColumn extends ColumnInfo<RoutingFile, String> {

        private Project project;

        public ExistsColumn(Project project) {
            super("Path");
            this.project = project;
        }

        @Nullable
        @Override
        public String valueOf(RoutingFile containerFile) {
            return containerFile.exists(this.project) ? "EXISTS" : "ERROR";
        }
    }
}
