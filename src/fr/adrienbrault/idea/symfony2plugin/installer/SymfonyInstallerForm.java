package fr.adrienbrault.idea.symfony2plugin.installer;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.php.composer.InterpretersComboWithBrowseButton;
import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

public class SymfonyInstallerForm {

    private JComboBox comboVersions;
    private JButton buttonRefresh;
    private JPanel mainPanel;
    private JPanel panelInterpreter;
    private JCheckBox checkBoxDemo;
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

        buttonRefresh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                appendSymfonyVersions();
            }
        });

        // @TODO: use com.intellij.util.ui.ReloadableComboBoxPanel in Phpstorm9 api level
        comboVersions.setRenderer(new ListCellRenderer());
        appendSymfonyVersions();
    }

    @Nullable
    private List<SymfonyInstallerVersion> getVersions() {

        String content = SymfonyInstallerUtil.getDownloadVersions();
        if(content == null) {
            return null;
        }

        return SymfonyInstallerUtil.getVersions(content);
    }

    private void appendSymfonyVersions()
    {

        comboVersions.setModel(new ListComboBoxModel<SymfonyInstallerVersion>(new ArrayList<SymfonyInstallerVersion>()));

        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            public void run() {
                final List<SymfonyInstallerVersion> symfonyInstallerVersions1 = getVersions();
                if (symfonyInstallerVersions1 != null) {
                    UIUtil.invokeLaterIfNeeded(new Runnable() {
                        @Override
                        public void run() {
                            comboVersions.setModel(new ListComboBoxModel<SymfonyInstallerVersion>(symfonyInstallerVersions1));
                        }
                    });
                }
            }
        });

    }

    public JComponent getContentPane()
    {
        return this.mainPanel;
    }

    private void createUIComponents() {
        panelInterpreter = interpretersComboWithBrowseButton = new InterpretersComboWithBrowseButton(ProjectManager.getInstance().getDefaultProject());
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
        String text = interpretersComboWithBrowseButton.getText();
        if(StringUtils.isNotBlank(text)) {
            return text;
        }
        return this.interpretersComboWithBrowseButton.getPhpPath();
    }

    @Nullable
    public ValidationInfo validate()
    {
        return this.interpretersComboWithBrowseButton.validatePhpPath(ProjectManager.getInstance().getDefaultProject());
    }

}
