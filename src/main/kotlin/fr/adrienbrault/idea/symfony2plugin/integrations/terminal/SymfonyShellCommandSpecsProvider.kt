package fr.adrienbrault.idea.symfony2plugin.integrations.terminal

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.terminal.completion.spec.ShellRuntimeContext
import com.jetbrains.php.lang.PhpLanguage
import fr.adrienbrault.idea.symfony2plugin.Settings
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyCommandUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpec
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecConflictStrategy
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecInfo
import org.jetbrains.plugins.terminal.block.completion.spec.ShellCommandSpecsProvider
import org.jetbrains.plugins.terminal.block.completion.spec.dsl.ShellChildCommandsContext
import org.jetbrains.plugins.terminal.block.completion.spec.project

private val COMMAND_DATA_CACHE = Key.create<CachedValue<List<CommandData>>>("SYMFONY_TERMINAL_COMMAND_DATA")

/**
 * Provides terminal completion for Symfony console commands.
 *
 * Supports both `bin/console <command>` and `symfony console <command>`.
 * Dynamically discovers commands via [SymfonyCommandUtil] and exposes them as subcommands
 * with their options and arguments in the IDE's built-in terminal.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@ApiStatus.Experimental
class SymfonyShellCommandSpecsProvider : ShellCommandSpecsProvider {

    override fun getCommandSpecs(): List<ShellCommandSpecInfo> = listOf(
        ShellCommandSpecInfo.create(
            ShellCommandSpec("bin/console") {
                description("Symfony Console")
                subcommands { runtimeCtx -> addSymfonyCommands(runtimeCtx) }
            },
            ShellCommandSpecConflictStrategy.OVERRIDE
        ),
        ShellCommandSpecInfo.create(
            ShellCommandSpec("symfony") {
                subcommands { _ ->
                    subcommand("console") {
                        description("Symfony Console")
                        subcommands { runtimeCtx -> addSymfonyCommands(runtimeCtx) }
                    }
                }
            },
            ShellCommandSpecConflictStrategy.OVERRIDE
        ),
    )
}

private suspend fun ShellChildCommandsContext.addSymfonyCommands(runtimeCtx: ShellRuntimeContext) {
    val project = try {
        runtimeCtx.project
    } catch (_: IllegalStateException) {
        return
    }

    val settings = Settings.getInstance(project) ?: return
    if (!settings.pluginEnabled) return

    val commands = withContext(Dispatchers.Default) {
        try {
            collectCommandData(project)
        } catch (_: Exception) {
            emptyList()
        }
    }

    for (data in commands) {
        subcommand(data.name) {
            for (opt in data.options.values) {
                val names = buildList {
                    add("--${opt.name()}")
                    opt.shortcut()?.takeIf { it.isNotEmpty() }?.let { add("-$it") }
                }
                option(*names.toTypedArray()) {
                    val desc = opt.description()
                    if (!desc.isNullOrEmpty()) description(desc)
                }
            }
            for (arg in data.arguments.values) {
                argument {
                    displayName(arg.name())
                    optional()
                }
            }
        }
    }
}

internal fun collectCommandData(project: Project): List<CommandData> {
    if (DumbService.isDumb(project)) return emptyList()

    return CachedValuesManager.getManager(project).getCachedValue(
        project,
        COMMAND_DATA_CACHE,
        {
            ReadAction.nonBlocking<CachedValueProvider.Result<List<CommandData>>> {
                CachedValueProvider.Result.create(
                    collectCommandDataInner(project),
                    PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)
                )
            }.expireWhen { project.isDisposed }.executeSynchronously()
        },
        false
    )
}

private fun collectCommandDataInner(project: Project): List<CommandData> =
    SymfonyCommandUtil.getCommands(project).map { command ->
        val phpClass = PhpElementsUtil.getClassInterface(project, command.fqn)
        CommandData(
            name = command.name,
            options = if (phpClass != null) SymfonyCommandUtil.getCommandOptions(phpClass) else emptyMap(),
            arguments = if (phpClass != null) SymfonyCommandUtil.getCommandArguments(phpClass) else emptyMap(),
        )
    }

internal data class CommandData(
    val name: String,
    val options: Map<String, SymfonyCommandUtil.CommandOption>,
    val arguments: Map<String, SymfonyCommandUtil.CommandArgument>,
)
