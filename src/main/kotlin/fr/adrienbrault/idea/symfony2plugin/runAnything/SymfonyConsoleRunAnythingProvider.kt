package fr.adrienbrault.idea.symfony2plugin.runAnything

import com.intellij.execution.ExecutionException
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.ide.actions.runAnything.activity.RunAnythingProviderBase
import com.intellij.ide.actions.runAnything.items.RunAnythingItem
import com.intellij.ide.actions.runAnything.items.RunAnythingItemBase
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ReadAction
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandRunConfiguration
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandRunConfigurationType
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandTestRunLineMarkerProvider
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand
import javax.swing.Icon

/**
 * Run Anything provider for Symfony console commands.
 *
 * Provides completion and execution of Symfony console commands in the Run Anything popup
 * (Ctrl+Ctrl). Commands are discovered from project PHP classes using existing
 * [SymfonyCommandUtil] infrastructure.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class SymfonyConsoleRunAnythingProvider : RunAnythingProviderBase<SymfonyCommand>() {

    override fun getValues(dataContext: DataContext, pattern: String): Collection<SymfonyCommand> {
        val project = CommonDataKeys.PROJECT.getData(dataContext)
        if (!Symfony2ProjectComponent.isEnabled(project)) return emptyList()

        val lowerPattern = pattern.lowercase().trim()

        return ReadAction.compute<Collection<SymfonyCommand>, RuntimeException> {
            SymfonyCommandUtil.getCommands(project!!)
                .filter { it.name.lowercase().contains(lowerPattern) }
        }
    }

    override fun execute(dataContext: DataContext, value: SymfonyCommand) {
        val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return

        val runManager = RunManager.getInstance(project)
        val type = SymfonyCommandRunConfigurationType.getInstance()

        val settings = runManager.createConfiguration(value.name, type.factory)
        val configuration = settings.configuration as SymfonyCommandRunConfiguration
        configuration.commandName = value.name

        // "symfony cli" or bin/console
        configuration.executionMode = if (SymfonyCommandTestRunLineMarkerProvider.isSymfonyCliAvailable()) {
            SymfonyCommandRunConfiguration.ExecutionMode.SYMFONY_CLI
        } else {
            SymfonyCommandRunConfiguration.ExecutionMode.PHP_INTERPRETER
        }

        configuration.name = value.name

        runManager.addConfiguration(settings)
        runManager.selectedConfiguration = settings

        try {
            ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), settings)
                .buildAndExecute()
        } catch (e: ExecutionException) {
            Symfony2ProjectComponent.getLogger().warn("Failed to execute Symfony command: ${value.name}", e)
        }
    }

    override fun getCompletionGroupTitle(): String = "Symfony Console"

    override fun getCommand(value: SymfonyCommand): String = value.name

    override fun getIcon(value: SymfonyCommand): Icon = Symfony2Icons.SYMFONY

    override fun getMainListItem(dataContext: DataContext, value: SymfonyCommand): RunAnythingItem {
        val shortName = value.fqn.substringAfterLast('\\').ifEmpty { value.fqn }
        return object : RunAnythingItemBase(value.name, Symfony2Icons.SYMFONY) {
            override fun getDescription(): String = shortName
        }
    }

    override fun getHelpGroupTitle(): String = "Symfony"
}
