package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.run.PhpRunConfigurationFactoryBase;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandRunConfigurationProducer extends LazyRunConfigurationProducer<SymfonyCommandRunConfiguration> {

    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return new PhpScrip().getConfigurationFactories()[0];
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull SymfonyCommandRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        Location location = context.getLocation();
        if (location instanceof PsiLocation) {
            PhpClass phpClass = SymfonyCommandTestRunLineMarkerProvider.getCommandContext(location.getPsiElement());
            if (phpClass != null) {
                List<String> commandNameFromClass = SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass);
                if (!commandNameFromClass.isEmpty()) {
                    // first name; on alias
                    String commandName = commandNameFromClass.iterator().next();
                    configuration.setCommandName(commandName);
                    configuration.setName(commandName);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull SymfonyCommandRunConfiguration configuration, @NotNull ConfigurationContext context) {
        Location location = context.getLocation();
        if (location instanceof PsiLocation) {
            PhpClass phpClass = SymfonyCommandTestRunLineMarkerProvider.getCommandContext(location.getPsiElement());
            if (phpClass != null) {
                return !SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass).isEmpty();
            }
        }

        return false;
    }

    private static final class PhpScrip implements ConfigurationType, DumbAware {
        private final ConfigurationFactory myFactory = new PhpRunConfigurationFactoryBase(this, "Symfony Command") {
            @NotNull
            public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
                return new SymfonyCommandRunConfiguration(project, this, "Symfony Command");
            }

            @NotNull
            public String getName() {
                return "Symfony Command";
            }
        };

        public PhpScrip() {
        }

        @Override
        public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
            return "Symfony Command";
        }

        @Override
        public @Nls(capitalization = Nls.Capitalization.Sentence) String getConfigurationTypeDescription() {
            return "Symfony Command";
        }

        public Icon getIcon() {
            return Symfony2Icons.SYMFONY;
        }

        public ConfigurationFactory[] getConfigurationFactories() {
            return new ConfigurationFactory[]{this.myFactory};
        }

        @NotNull
        public String getId() {
            return "symfony.command";
        }
    }
}
