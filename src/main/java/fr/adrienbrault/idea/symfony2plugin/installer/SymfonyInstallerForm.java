package fr.adrienbrault.idea.symfony2plugin.installer;

import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerForm {
    private JButton buttonRefresh;
    private JPanel mainPanel;
    private JComboBox<String> comboProjectTypes;
    private JCheckBox checkBoxDownload;

    public SymfonyInstallerForm() {
        comboProjectTypes.setModel(new ListComboBoxModel<>(List.of("standard", "webapp", "full", "demo", "book")));
    }


    public JComponent getContentPane()
    {
        return this.mainPanel;
    }

    @NotNull
    public String getProjectType() {
        return (String) this.comboProjectTypes.getSelectedItem();
    }

    public boolean isDownloadInstallerSelected() {
        return this.checkBoxDownload.isSelected();
    }
}
