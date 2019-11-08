package fr.adrienbrault.idea.symfonyplugin.installer;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import icons.PhpIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerTemplatesFactory extends ProjectTemplatesFactory {
    @NotNull
    @Override
    public String[] getGroups() {
        return new String[] { "PHP" };
    }

    public Icon getGroupIcon(String group) {
        return PhpIcons.Php_icon;
    }

    @NotNull
    @Override
    public ProjectTemplate[] createTemplates(String s, WizardContext wizardContext) {
        return new ProjectTemplate[] { new SymfonyInstallerProjectGenerator() };
    }
}
