package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
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

public class ContainerSettingsForm implements Configurable {

    private JPanel panel1;
    private JPanel listviewPanel;
    private JButton buttonReset;
    private TableView<ContainerFile> tableView;
    private Project project;
    private boolean changed = false;
    private ListTableModel<ContainerFile> modelList;


    public ContainerSettingsForm(@NotNull Project project) {

        this.project = project;
        this.tableView = new TableView<ContainerFile>();

        this.modelList = new ListTableModel<ContainerFile>(
            new PathColumn(project),
            new ExistsColumn(project)
        );

        this.fillContainerList();

        this.modelList.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                ContainerSettingsForm.this.changed = true;
            }
        });

        this.tableView.setModelAndUpdateColumns(this.modelList);

        buttonReset.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                resetContainerList();

                // add default path
                for (String defaultContainerPath : Settings.DEFAULT_CONTAINER_PATHS) {
                    ContainerSettingsForm.this.modelList.addRow(new ContainerFile(defaultContainerPath));
                }

            }
        });
    }

    private void fillContainerList() {
        List<ContainerFile> containerFiles = getSettings().containerFiles;
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
        ToolbarDecorator tablePanel = ToolbarDecorator.createDecorator(this.tableView, new ElementProducer<ContainerFile>() {
            @Override
            public ContainerFile createElement() {
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
                ContainerFile containerFile = ContainerSettingsForm.this.tableView.getSelectedObject();
                if(containerFile != null) {
                    String uri = ContainerSettingsForm.this.getPathDialog(null);
                    if(uri != null) {
                        containerFile.setPath(uri);
                        ContainerSettingsForm.this.changed = true;
                    }

                }
            }
        });

        tablePanel.setAddAction(new AnActionButtonRunnable() {
            @Override
            public void run(AnActionButton anActionButton) {
                String uri = ContainerSettingsForm.this.getPathDialog(null);
                if(uri != null) {
                    ContainerSettingsForm.this.tableView.getListTableModel().addRow(new ContainerFile(uri));
                    ContainerSettingsForm.this.changed = true;
                }
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
        List<ContainerFile> containerFiles = new ArrayList<ContainerFile>();

        for(ContainerFile containerFile :this.tableView.getListTableModel().getItems()) {
            containerFiles.add(new ContainerFile(containerFile.getPath()));
        }

        getSettings().containerFiles = containerFiles;
        this.changed = false;
    }

    private Settings getSettings() {
        return Settings.getInstance(this.project);
    }

    @Override
    public void reset() {
        this.resetContainerList();
        this.fillContainerList();
        this.changed = false;
    }

    private void resetContainerList() {
        // clear list, easier?
        while(this.modelList.getRowCount() > 0) {
            this.modelList.removeRow(0);
        }
    }

    @Override
    public void disposeUIResources() {
    }

    private class PathColumn extends ColumnInfo<ContainerFile, String> {

        private Project project;

        public PathColumn(Project project) {
            super("Path");
            this.project = project;
        }

        @Nullable
        @Override
        public String valueOf(ContainerFile containerFile) {
            return containerFile.getPath();
        }
    }

    private class ExistsColumn extends ColumnInfo<ContainerFile, String> {

        private Project project;

        public ExistsColumn(Project project) {
            super("Path");
            this.project = project;
        }

        @Nullable
        @Override
        public String valueOf(ContainerFile containerFile) {
            return containerFile.exists(this.project) ? "EXISTS" : "ERROR";
        }
    }

    private String getPathDialog(String current) {
        VirtualFile projectDirectory = project.getBaseDir();

        VirtualFile selectedFileBefore = null;
        if(current != null) {
            selectedFileBefore = VfsUtil.findRelativeFile(current, projectDirectory);
        }

        VirtualFile selectedFile = FileChooser.chooseFile(
            FileChooserDescriptorFactory.createSingleFileDescriptor(StdFileTypes.XML),
            project,
            selectedFileBefore
        );

        if (null == selectedFile) {
            return null;
        }

        String path = VfsUtil.getRelativePath(selectedFile, projectDirectory, '/');
        if (null == path) {
            path = selectedFile.getPath();
        }

        return path;
    }

}
