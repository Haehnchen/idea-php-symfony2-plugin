package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ScriptRunnerUtil;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandRunConfiguration extends LocatableConfigurationBase<SymfonyCommandRunConfiguration.Config> {
    // @TODO: empty config for now: we need binary and arguments input. find suitable existing "configuration groups" for this
    public static class Config {}

    @Nullable
    private String commandName;

    protected SymfonyCommandRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
    }

    @Override
    public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new SettingsEditorGroup<>();
    }

    @Override
    public @Nullable RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return (executor1, runner) -> {
            VirtualFile projectDir = ProjectUtil.getProjectDir(getProject());

            ProcessHandler processHandler = ScriptRunnerUtil.execute("bin/console", projectDir.getPath(), null, new String[] {this.commandName}, null, (commandLine) -> {
                KillableProcessHandler handler = new KillableProcessHandler(commandLine);
                handler.setShouldKillProcessSoftly(false);
                return handler;
            });

            ConsoleViewImpl console = new ConsoleViewImpl(getProject(), true);
            console.attachToProcess(processHandler);
            return new DefaultExecutionResult(console, processHandler);
        };
    }

    public void setCommandName(@NotNull String commandName) {
        this.commandName = commandName;
    }
}
