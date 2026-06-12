package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandRunConfigurationProducer extends LazyRunConfigurationProducer<SymfonyCommandRunConfiguration> {

    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return SymfonyCommandRunConfigurationType.getInstance().getFactory();
    }

    @Override
    protected boolean setupConfigurationFromContext(@NotNull SymfonyCommandRunConfiguration configuration, @NotNull ConfigurationContext context, @NotNull Ref<PsiElement> sourceElement) {
        List<String> commandNames = getCommandNames(context);
        if (commandNames.isEmpty()) {
            return false;
        }

        String commandName = commandNames.get(0);
        configuration.setCommandName(commandName);
        configuration.setName(commandName);

        if (SymfonyCommandTestRunLineMarkerProvider.isSymfonyCliAvailable()) {
            configuration.setExecutionMode(SymfonyCommandRunConfiguration.ExecutionMode.SYMFONY_CLI);
        } else {
            configuration.setExecutionMode(SymfonyCommandRunConfiguration.ExecutionMode.PHP_INTERPRETER);
        }

        return true;
    }

    @Override
    public boolean isConfigurationFromContext(@NotNull SymfonyCommandRunConfiguration configuration, @NotNull ConfigurationContext context) {
        List<String> commandNames = getCommandNames(context);
        if (commandNames.isEmpty()) {
            return false;
        }

        return commandNames.contains(configuration.getCommandName());
    }

    @NotNull
    private static List<String> getCommandNames(@NotNull ConfigurationContext context) {
        Location<?> location = context.getLocation();
        if (!(location instanceof PsiLocation)) {
            return List.of();
        }

        PhpNamedElement commandTarget = SymfonyCommandTestRunLineMarkerProvider.getCommandTargetContext(location.getPsiElement());
        if (commandTarget == null) {
            return List.of();
        }

        return SymfonyCommandTestRunLineMarkerProvider.getCommandNames(commandTarget);
    }
}
