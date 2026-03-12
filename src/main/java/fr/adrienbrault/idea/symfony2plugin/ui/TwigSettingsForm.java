package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.DefaultProject;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.ListTableModel;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigSettingsForm implements Configurable {

    private JPanel panel1;
    private JCheckBox chkTwigBundleNamespaceSupport;
    private TableView<TwigPath> tableView;
    private final Project project;
    private boolean changed = false;
    private ListTableModel<TwigPath> modelList;
    private final AtomicInteger reloadRequestId = new AtomicInteger();
    private volatile boolean disposed = false;

    public TwigSettingsForm(@NotNull Project project) {
        this.project = project;
    }

    private void attachItems() {
        reloadTwigPaths(true, false);
    }

    private void reloadTwigPaths(boolean includeSettings, boolean forceEnableAll) {
        if (this.project instanceof DefaultProject) {
            return;
        }

        this.resetList();

        if (DumbService.getInstance(project).isDumb()) {
            this.tableView.getEmptyText().setText("Loading after indexing completes...");
        } else {
            this.tableView.getEmptyText().setText("Loading Twig namespaces...");
        }

        int requestId = this.reloadRequestId.incrementAndGet();

        ReadAction
            .nonBlocking(() -> {
                List<TwigPath> sortableLookupItems = new ArrayList<>(TwigUtil.getTwigNamespaces(this.project, includeSettings));
                sortableLookupItems.sort(new TwigUtil.TwigPathNamespaceComparator());
                return sortableLookupItems;
            })
            .inSmartMode(this.project)
            .expireWhen(() -> this.disposed || this.project.isDisposed() || requestId != this.reloadRequestId.get())
            .finishOnUiThread(ModalityState.any(), twigPaths -> this.applyTwigPaths(requestId, twigPaths, forceEnableAll))
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    private void applyTwigPaths(int requestId, @NotNull List<TwigPath> twigPaths, boolean forceEnableAll) {
        if (this.disposed || this.project.isDisposed() || requestId != this.reloadRequestId.get()) {
            return;
        }

        this.resetList();

        for (TwigPath twigPath : twigPaths) {
            this.modelList.addRow(forceEnableAll ? TwigPath.createClone(twigPath, true) : TwigPath.createClone(twigPath));
        }

        if (twigPaths.isEmpty()) {
            this.tableView.getEmptyText().setText("No Twig namespaces found.");
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
        this.disposed = false;
        panel1 = new JPanel(new BorderLayout());
        JPanel panelTableView = new JPanel(new BorderLayout());
        panel1.add(panelTableView, BorderLayout.CENTER);

        chkTwigBundleNamespaceSupport = new JCheckBox("Support Bundle Namespaces");
        chkTwigBundleNamespaceSupport.setSelected(true);
        chkTwigBundleNamespaceSupport.setToolTipText("Example: Foobar:Bar:Foo.html.twig (for older Symfony Versions)");
        JButton buttonJsonExample = new JButton("JSON Example");
        JButton resetToDefault = new JButton("Reset To Default");

        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.add(new JLabel("Manage Twig Namespaces"));
        northPanel.add(chkTwigBundleNamespaceSupport);
        northPanel.add(buttonJsonExample);
        northPanel.add(resetToDefault);
        panel1.add(northPanel, BorderLayout.NORTH);

        this.tableView = new TableView<>();
        this.modelList = new ListTableModel<>(
            new NamespaceColumn(),
            new PathColumn(project),
            new TypeColumn(),
            new CustomColumn(),
            new DisableColumn()
        );

        this.attachItems();

        this.tableView.setModelAndUpdateColumns(this.modelList);

        this.modelList.addTableModelListener(e -> TwigSettingsForm.this.changed = true);

        resetToDefault.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                TwigSettingsForm.this.reloadTwigPaths(false, true);
            }
        });

        ToolbarDecorator tablePanel = ToolbarDecorator.createDecorator(this.tableView, new ElementProducer<>() {
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

        tablePanel.setEditAction(anActionButton -> TwigSettingsForm.this.openTwigPathDialog(TwigSettingsForm.this.tableView.getSelectedObject()));


        tablePanel.setAddAction(anActionButton -> TwigSettingsForm.this.openTwigPathDialog(null));

        tablePanel.setEditActionUpdater(e -> {
            TwigPath twigPath = TwigSettingsForm.this.tableView.getSelectedObject();
            return twigPath != null && twigPath.isCustomPath();
        });

        tablePanel.setRemoveActionUpdater(e -> {
            TwigPath twigPath = TwigSettingsForm.this.tableView.getSelectedObject();
            return twigPath != null && twigPath.isCustomPath();
        });

        tablePanel.disableUpAction();
        tablePanel.disableDownAction();

        panelTableView.add(tablePanel.createPanel());

        buttonJsonExample.addActionListener(e -> TwigJsonExampleDialog.open(TwigSettingsForm.this.panel1));

        return this.panel1;
    }

    @Override
    public boolean isModified() {
        return this.changed
            || getSettings().twigBundleNamespaceSupport != chkTwigBundleNamespaceSupport.isSelected();
    }

    @Override
    public void apply() throws ConfigurationException {
        List<TwigNamespaceSetting> twigPaths = new ArrayList<>();

        for (TwigPath twigPath : this.tableView.getListTableModel().getItems()) {
            // only custom and disabled path need to save
            if ((!twigPath.isEnabled() && twigPath.getRelativePath(this.project) != null) || twigPath.isCustomPath()) {
                twigPaths.add(new TwigNamespaceSetting(twigPath.getNamespace(), twigPath.getRelativePath(this.project), twigPath.isEnabled(), twigPath.getNamespaceType(), twigPath.isCustomPath()));
            }
        }

        getSettings().twigBundleNamespaceSupport = chkTwigBundleNamespaceSupport.isSelected();
        getSettings().twigNamespaces = twigPaths;
        this.changed = false;
    }

    private Settings getSettings() {
        return Settings.getInstance(this.project);
    }

    private void resetList() {
        // clear list, easier?
        while (this.modelList.getRowCount() > 0) {
            this.modelList.removeRow(0);
        }

    }

    @Override
    public void reset() {
        this.attachItems();
        this.updateUIFromSettings();
        this.changed = false;
    }

    private void updateUIFromSettings() {
        this.chkTwigBundleNamespaceSupport.setSelected(getSettings().twigBundleNamespaceSupport);
    }

    @Override
    public void disposeUIResources() {
        this.disposed = true;
        this.reloadRequestId.incrementAndGet();
        this.resetList();
    }

    private static class NamespaceColumn extends ColumnInfo<TwigPath, String> {

        public NamespaceColumn() {
            super("Namespace");
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.getNamespace();
        }
    }

    private static class PathColumn extends ColumnInfo<TwigPath, String> {

        private final Project project;

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

    private static class CustomColumn extends ColumnInfo<TwigPath, String> {

        public CustomColumn() {
            super("Parser");
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.isCustomPath() ? "Custom" : "Internal";
        }
    }

    private static class TypeColumn extends ColumnInfo<TwigPath, String> {

        public TypeColumn() {
            super("Type");
        }

        @Nullable
        @Override
        public String valueOf(TwigPath twigPath) {
            return twigPath.getNamespaceType().toString();
        }
    }

    private abstract static class BooleanColumn extends ColumnInfo<TwigPath, Boolean> {
        public BooleanColumn(String name) {
            super(name);
        }

        public boolean isCellEditable(TwigPath groupItem) {
            return true;
        }

        public Class<?> getColumnClass() {
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

        public void setValue(TwigPath twigPath, Boolean value) {
            int index = TwigSettingsForm.this.tableView.getListTableModel().getItems().indexOf(twigPath);
            if (index >= 0) {
                TwigPath newTwigPath = TwigPath.createClone(twigPath, value);
                TwigSettingsForm.this.tableView.getListTableModel().removeRow(index);
                TwigSettingsForm.this.tableView.getListTableModel().insertRow(index, newTwigPath);
            }
        }

        public int getWidth(JTable table) {
            return 50;
        }

    }

    private void openTwigPathDialog(@Nullable TwigPath twigPath) {
        TwigNamespaceDialog twigNamespaceDialog;
        if (twigPath == null) {
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
