package fr.adrienbrault.idea.symfony2plugin.installer;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.*;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyInstallerCommandExecutor {

    private static final long CHECKING_TIMEOUT_IN_MILLISECONDS = 1000L;
    private SymfonyInstallerSettings symfonyInstallerSettings;
    private File temporaryDirectory;
    private final Project myProject;
    private final VirtualFile baseDir;
    private final String[] command;

    SymfonyInstallerCommandExecutor(@NotNull SymfonyInstallerSettings symfonyInstallerSettings, @Nullable File temporaryDirectory, @NotNull Project project, @NotNull VirtualFile baseDir, @NotNull String[] command) {
        this.symfonyInstallerSettings = symfonyInstallerSettings;
        this.temporaryDirectory = temporaryDirectory;
        this.myProject = project;
        this.baseDir = baseDir;
        this.command = command;
    }

    public void execute()  {
        Task task = new Task.Modal(this.myProject, getProgressTitle(), true) {
            public void run(@NotNull final ProgressIndicator indicator) {
                indicator.setIndeterminate(true);

                String[] myCommand = SymfonyInstallerCommandExecutor.this.command;

                StringBuilder sb = new StringBuilder();
                sb.append("Running: ");
                for (String aCommandToRun : Arrays.copyOfRange(myCommand, 1, myCommand.length)) {
                    if (aCommandToRun.length() > 35) {
                        aCommandToRun = "..." + aCommandToRun.substring(aCommandToRun.length() - 35);
                    }
                    sb.append(" ").append(aCommandToRun);
                }
                indicator.setText(sb.toString());

                boolean cancelledByUser = false;
                final StringBuilder outputBuilder = new StringBuilder();
                try {
                    OSProcessHandler processHandler = ScriptRunnerUtil.execute(myCommand[0], SymfonyInstallerCommandExecutor.this.baseDir.getPath(), null, Arrays.copyOfRange(myCommand, 1, myCommand.length));

                    processHandler.addProcessListener(new ProcessAdapter() {
                        @Override
                        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull com.intellij.openapi.util.Key outputType) {
                            String text = event.getText();
                            outputBuilder.append(text);

                            text = SymfonyInstallerUtil.formatConsoleTextIndicatorOutput(text);
                            if(StringUtils.isNotBlank(text)) {
                                indicator.setText2(text);
                            }

                        }
                    });

                    processHandler.startNotify();
                    for (;;){
                        boolean finished = processHandler.waitFor(CHECKING_TIMEOUT_IN_MILLISECONDS);
                        if (finished) {
                            break;
                        }
                        if (indicator.isCanceled()) {
                            cancelledByUser = true;
                            OSProcessManager.getInstance().killProcessTree(processHandler.getProcess());
                            break;
                        }
                    }

                }

                catch (ExecutionException e) {
                    SymfonyInstallerCommandExecutor.this.onError(e.getMessage());
                    return;
                }

                if(cancelledByUser) {
                    SymfonyInstallerCommandExecutor.this.onError("Checkout canceled");
                    return;
                }

                String output = outputBuilder.toString();
                if (StringUtils.isBlank(output) || !SymfonyInstallerUtil.isSuccessfullyInstalled(output)) {

                    String message = SymfonyInstallerUtil.formatExceptionMessage(output);
                    if(message == null) {
                        message = "The unexpected happens...";
                    }

                    SymfonyInstallerCommandExecutor.this.onError(message);
                    return;
                }

                indicator.setText2("Preparing Project Structure...");
                try {
                    File fromDir = new File(baseDir.getPath() + "/" + SymfonyInstallerUtil.PROJECT_SUB_FOLDER);
                    FileUtil.copyDirContent(fromDir, new File(baseDir.getPath()));
                    FileUtil.delete(fromDir);
                } catch (IOException e) {
                    SymfonyInstallerCommandExecutor.this.onError(e.getMessage());
                    return;
                }

                SymfonyInstallerCommandExecutor.this.onFinish(SymfonyInstallerUtil.extractSuccessMessage(output));
            }
        };

        ProgressManager.getInstance().run(task);
    }

    protected void onFinish(@Nullable String message) {
        IdeHelper.enablePluginAndConfigure(this.myProject);

        if(message != null) {
            // replace empty lines, provide html output, and remove our temporary path
            SymfonyInstallerProjectGenerator.showInfoNotification(myProject, message
                .replaceAll("(?m)^\\s*$[\n\r]{1,}", "")
                .replaceAll("(\r\n|\n)", "<br />")
                .replace("/" + SymfonyInstallerUtil.PROJECT_SUB_FOLDER, "")
            );
        }

        // remove temporary symfony installer folder
        if(temporaryDirectory != null) {
            FileUtil.delete(temporaryDirectory);
        }
    }

    protected void onError(@NotNull String message) {
        SymfonyInstallerProjectGenerator.showErrorNotification(myProject, message);
    }

    protected String getProgressTitle() {
        return String.format("Installing Symfony %s", this.symfonyInstallerSettings.getVersion().getPresentableName());
    }

}