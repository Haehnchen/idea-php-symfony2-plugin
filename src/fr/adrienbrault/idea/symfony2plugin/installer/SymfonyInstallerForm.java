package fr.adrienbrault.idea.symfony2plugin.installer;

import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.io.HttpRequests;
import com.jetbrains.php.composer.InterpretersComboWithBrowseButton;
import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SymfonyInstallerForm {

    private JComboBox comboVersions;
    private JButton buttonRefresh;
    private JPanel mainPanel;
    private JPanel panelInterpreter;
    private JCheckBox checkBoxDemo;
    private JLabel labelDemoApp;
    private InterpretersComboWithBrowseButton interpretersComboWithBrowseButton;

    public SymfonyInstallerForm() {

        checkBoxDemo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                boolean state = e.getStateChange() != ItemEvent.SELECTED;
                buttonRefresh.setEnabled(state);
                comboVersions.setEnabled(state);
            }
        });

    }

    @Nullable
    private List<SymfonyInstallerVersion> getVersions() {

        String userAgent = String.format("%s / %s / Symfony Plugin %s",
            ApplicationInfo.getInstance().getVersionName(),
            ApplicationInfo.getInstance().getBuild(),
            PluginManager.getPlugin(PluginId.getId("fr.adrienbrault.idea.symfony2plugin")).getVersion()
        );

        String content;
        try {
            content = HttpRequests.request("http://symfony.com/versions.json").userAgent(userAgent).readString(new ProgressIndicatorBase());
        } catch (IOException e) {
            return null;
        }

        return SymfonyInstallerUtil.getVersions(content);
    }

    private void appendSymfonyVersions()
    {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            public void run() {
                List<SymfonyInstallerVersion> symfonyInstallerVersions1 = getVersions();
                if(symfonyInstallerVersions1 != null) {
                    comboVersions.setModel(new ListComboBoxModel<SymfonyInstallerVersion>(symfonyInstallerVersions1));
                    comboVersions.updateUI();
                }
            }
        });
    }

    public JComponent getContentPane()
    {
        return this.mainPanel;
    }

    private void createUIComponents() {

        comboVersions = new ComboBox();
        comboVersions.setRenderer(new ListCellRenderer());
        comboVersions.setModel(new ListComboBoxModel<SymfonyInstallerVersion>(new ArrayList<SymfonyInstallerVersion>()));

        buttonRefresh = new JButton("Reload");

        buttonRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appendSymfonyVersions();
            }
        });

        panelInterpreter = interpretersComboWithBrowseButton = new InterpretersComboWithBrowseButton(ProjectManager.getInstance().getDefaultProject());

        appendSymfonyVersions();
    }

    private static class ListCellRenderer extends ListCellRendererWrapper<SymfonyInstallerVersion> {

        @Override
        public void customize(JList list, SymfonyInstallerVersion value, int index, boolean selected, boolean hasFocus) {
            if(value != null) {
                setText(value.getPresentableName());
            }
        }
    }

    public SymfonyInstallerVersion getVersion() {

        if(checkBoxDemo.isSelected()) {
            return new SymfonyInstallerVersion("demo", "Demo Application");
        }

        Object selectedItem = this.comboVersions.getSelectedItem();
        if(selectedItem instanceof SymfonyInstallerVersion) {
            return ((SymfonyInstallerVersion) selectedItem);
        }

        return null;
    }

    public String getInterpreter() {
        return this.interpretersComboWithBrowseButton.getPhpPath();
    }

    @Nullable
    public ValidationInfo validate()
    {
        return this.interpretersComboWithBrowseButton.validatePhpPath(ProjectManager.getInstance().getDefaultProject());
    }

}
