package fr.adrienbrault.idea.symfony2plugin.dic.command;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.KillableProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ScriptRunnerUtil;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.execution.ParametersListUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandRunConfiguration extends LocatableConfigurationBase<RunConfigurationOptions> {

    public enum ExecutionMode {
        SYMFONY_CLI("Symfony CLI"),
        PHP_INTERPRETER("PHP Interpreter");

        private final String displayName;

        ExecutionMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private ExecutionMode executionMode = ExecutionMode.SYMFONY_CLI;
    private String commandName;
    private String consolePath = "bin/console";
    private String workingDirectory;
    private String interpreterPath = "php";
    private String symfonyCliPath = "symfony";
    private String commandLineParameters;

    protected SymfonyCommandRunConfiguration(@NotNull Project project, @NotNull ConfigurationFactory factory, @NotNull String name) {
        super(project, factory, name);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new SymfonyCommandRunConfigurationEditor();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (commandName == null || commandName.isEmpty()) {
            throw new RuntimeConfigurationError("Command name is required");
        }
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return (executor1, runner) -> {
            Project project = getProject();
            VirtualFile projectDir = ProjectUtil.getProjectDir(project);

            String workDir = workingDirectory != null && !workingDirectory.isEmpty()
                ? workingDirectory
                : projectDir.getPath();

            List<String> command = new ArrayList<>();

            if (executionMode == ExecutionMode.SYMFONY_CLI) {
                String cli = symfonyCliPath != null && !symfonyCliPath.isEmpty() ? symfonyCliPath : "symfony";
                command.add(cli);
                command.add("console");
            } else {
                String interpreter = interpreterPath != null && !interpreterPath.isEmpty() ? interpreterPath : "php";
                String console = consolePath != null && !consolePath.isEmpty() ? consolePath : "bin/console";
                command.add(interpreter);
                command.add(console);
            }

            command.add(commandName);

            if (commandLineParameters != null && !commandLineParameters.isEmpty()) {
                command.addAll(ParametersListUtil.parse(commandLineParameters));
            }

            ProcessHandler processHandler = ScriptRunnerUtil.execute(
                command.get(0),
                workDir,
                null,
                command.subList(1, command.size()).toArray(new String[0]),
                null,
                commandLine -> {
                    KillableProcessHandler handler = new KillableProcessHandler(commandLine);
                    handler.setShouldKillProcessSoftly(false);
                    return handler;
                }
            );

            ConsoleViewImpl consoleView = new ConsoleViewImpl(project, false);
            consoleView.attachToProcess(processHandler);
            return new DefaultExecutionResult(consoleView, processHandler);
        };
    }

    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);
        Element settingsElement = element.getChild("symfony-command-settings");
        if (settingsElement != null) {
            commandName = settingsElement.getAttributeValue("commandName");
            consolePath = settingsElement.getAttributeValue("consolePath", "bin/console");
            workingDirectory = settingsElement.getAttributeValue("workingDirectory");
            interpreterPath = settingsElement.getAttributeValue("interpreterPath", "php");
            symfonyCliPath = settingsElement.getAttributeValue("symfonyCliPath", "symfony");
            commandLineParameters = settingsElement.getAttributeValue("commandLineParameters");
            String mode = settingsElement.getAttributeValue("executionMode");
            if (mode != null) {
                try {
                    executionMode = ExecutionMode.valueOf(mode);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        Element settingsElement = new Element("symfony-command-settings");
        settingsElement.setAttribute("executionMode", executionMode.name());
        if (commandName != null) settingsElement.setAttribute("commandName", commandName);
        if (consolePath != null) settingsElement.setAttribute("consolePath", consolePath);
        if (workingDirectory != null) settingsElement.setAttribute("workingDirectory", workingDirectory);
        if (interpreterPath != null) settingsElement.setAttribute("interpreterPath", interpreterPath);
        if (symfonyCliPath != null) settingsElement.setAttribute("symfonyCliPath", symfonyCliPath);
        if (commandLineParameters != null) settingsElement.setAttribute("commandLineParameters", commandLineParameters);
        element.addContent(settingsElement);
    }

    @NotNull
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(@NotNull ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    @Nullable
    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(@Nullable String commandName) {
        this.commandName = commandName;
    }

    @Nullable
    public String getConsolePath() {
        return consolePath;
    }

    public void setConsolePath(@Nullable String consolePath) {
        this.consolePath = consolePath;
    }

    @Nullable
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(@Nullable String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Nullable
    public String getInterpreterPath() {
        return interpreterPath;
    }

    public void setInterpreterPath(@Nullable String interpreterPath) {
        this.interpreterPath = interpreterPath;
    }

    @Nullable
    public String getSymfonyCliPath() {
        return symfonyCliPath;
    }

    public void setSymfonyCliPath(@Nullable String symfonyCliPath) {
        this.symfonyCliPath = symfonyCliPath;
    }

    @Nullable
    public String getCommandLineParameters() {
        return commandLineParameters;
    }

    public void setCommandLineParameters(@Nullable String commandLineParameters) {
        this.commandLineParameters = commandLineParameters;
    }
}
