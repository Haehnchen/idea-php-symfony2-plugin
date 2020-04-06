package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.ListTableModel;
import com.jetbrains.plugins.webDeployment.config.WebServerConfig;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.dict.UiPathColumnInfo;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.dict.WebServerFileDialogExtensionCallback;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.WebDeploymentUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
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
        this.tableView = new TableView<>();

        this.modelList = new ListTableModel<>(
            new UiPathColumnInfo.PathColumn(),
            new UiPathColumnInfo.TypeColumn(project)
        );

        this.fillContainerList();

        this.modelList.addTableModelListener(e ->
            ContainerSettingsForm.this.changed = true
        );

        this.tableView.setModelAndUpdateColumns(this.modelList);

        buttonReset.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                resetContainerList();

                // add default path
                for (String defaultContainerPath : ServiceContainerUtil.getContainerFiles(project)) {
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

        tablePanel.setEditAction(anActionButton -> {
            ContainerFile containerFile = ContainerSettingsForm.this.tableView.getSelectedObject();
            if(containerFile != null) {
                String uri = UiSettingsUtil.getPathDialog(project, StdFileTypes.XML);
                if(uri != null) {
                    containerFile.setPath(uri);
                    ContainerSettingsForm.this.changed = true;
                }

            }
        });

        tablePanel.setAddAction(anActionButton -> {
            String uri = UiSettingsUtil.getPathDialog(project, StdFileTypes.XML);
            if(uri != null) {
                ContainerSettingsForm.this.tableView.getListTableModel().addRow(new ContainerFile(uri));
                ContainerSettingsForm.this.changed = true;
            }
        });

        if(WebDeploymentUtil.isEnabled(project)) {
            addWebDeploymentButton(tablePanel);
        }

        this.panel1.add(tablePanel.createPanel());
        return this.panel1;
    }

    private void addWebDeploymentButton(ToolbarDecorator tablePanel) {
        tablePanel.addExtraAction(new AnActionButton("Remote", AllIcons.Actions.Download) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                UiSettingsUtil.openFileDialogForDefaultWebServerConnection(project, new WebServerFileDialogExtensionCallback("xml") {
                    @Override
                    public void success(@NotNull WebServerConfig server, @NotNull WebServerConfig.RemotePath remotePath) {
                        ContainerSettingsForm.this.tableView.getListTableModel().addRow(
                            new ContainerFile("remote://" + org.apache.commons.lang.StringUtils.stripStart(remotePath.path, "/"))
                        );

                        ContainerSettingsForm.this.changed = true;
                    }
                });
            }
        });
    }

    @Override
    public boolean isModified() {
        return this.changed;
    }

    @Override
    public void apply() throws ConfigurationException {
        List<ContainerFile> containerFiles = new ArrayList<>();

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
}
