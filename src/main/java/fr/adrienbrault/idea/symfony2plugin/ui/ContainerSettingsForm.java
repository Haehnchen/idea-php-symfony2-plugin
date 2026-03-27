package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.UiSettingsUtil;
import fr.adrienbrault.idea.symfony2plugin.ui.utils.dict.UiPathColumnInfo;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerSettingsForm implements Configurable {

    private JPanel panel1;
    private JButton buttonReset;
    private final TableView<ContainerFile> tableView;
    private final Project project;
    private boolean changed = false;
    private final ListTableModel<ContainerFile> modelList;


    public ContainerSettingsForm(@NotNull Project project) {
        createUIComponents();

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

    private void createUIComponents() {
        panel1 = new JPanel(new BorderLayout());

        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(new JLabel("Add additional Symfony xml container files"), BorderLayout.CENTER);
        buttonReset = new JButton("Reset To Default");
        northPanel.add(buttonReset, BorderLayout.EAST);

        panel1.add(northPanel, BorderLayout.NORTH);
    }

    private void fillContainerList() {
        List<ContainerFile> containerFiles = getSettings().containerFiles;
        if (containerFiles != null && !containerFiles.isEmpty()) {
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
        ToolbarDecorator tablePanel = ToolbarDecorator.createDecorator(this.tableView, new ElementProducer<>() {
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
            if (containerFile != null) {
                String uri = UiSettingsUtil.getPathDialog(project, XmlFileType.INSTANCE);
                if (uri != null) {
                    containerFile.setPath(uri);
                    ContainerSettingsForm.this.changed = true;
                }

            }
        });

        tablePanel.setAddAction(anActionButton -> {
            String uri = UiSettingsUtil.getPathDialog(project, XmlFileType.INSTANCE);
            if (uri != null) {
                ContainerSettingsForm.this.tableView.getListTableModel().addRow(new ContainerFile(uri));
                ContainerSettingsForm.this.changed = true;
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
        List<ContainerFile> containerFiles = new ArrayList<>();

        for (ContainerFile containerFile : this.tableView.getListTableModel().getItems()) {
            containerFiles.add(new ContainerFile(containerFile.getPath()));
        }

        getSettings().containerFiles = containerFiles;
        this.changed = false;

        SymfonyVarDirectoryWatcherKt.syncSymfonyVarDirectoryWatcher(project);
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
        while (this.modelList.getRowCount() > 0) {
            this.modelList.removeRow(0);
        }
    }
}
