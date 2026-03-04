package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public final class SymfonyCommandRunConfigurationType extends ConfigurationTypeBase implements DumbAware {

    public static final String ID = "SymfonyCommandRunConfigurationType";

    public SymfonyCommandRunConfigurationType() {
        super(ID, "Symfony Command", "Run Symfony console commands", Symfony2Icons.SYMFONY);
        addFactory(new ConfigurationFactory(this) {
            @NotNull
            @Override
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new SymfonyCommandRunConfiguration(project, this, "Symfony Command");
            }

            @NotNull
            @Override
            public String getId() {
                return "Symfony Command";
            }
        });
    }

    @NotNull
    public static SymfonyCommandRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(SymfonyCommandRunConfigurationType.class);
    }

    public ConfigurationFactory getFactory() {
        return getConfigurationFactories()[0];
    }
}
