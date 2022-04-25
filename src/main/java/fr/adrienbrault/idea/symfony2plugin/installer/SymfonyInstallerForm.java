package fr.adrienbrault.idea.symfony2plugin.installer;

import com.intellij.openapi.ui.ValidationInfo;
import fr.adrienbrault.idea.symfony2plugin.installer.dict.SymfonyInstallerVersion;
import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerForm {
    private JButton buttonRefresh;
    private JPanel mainPanel;
    private JComboBox<String> comboProjectTypes;

    public SymfonyInstallerForm() {
        comboProjectTypes.setModel(new ListComboBoxModel<>(List.of("standard", "webapp", "full", "demo", "book")));
    }


    public JComponent getContentPane()
    {
        return this.mainPanel;
    }

    public SymfonyInstallerVersion getVersion() {
        return null;
    }

    @NotNull
    public String getProjectType() {
        return (String) this.comboProjectTypes.getSelectedItem();
    }

    @Nullable
    public ValidationInfo validate()
    {
        return new ValidationInfo("Test");
    }
}
