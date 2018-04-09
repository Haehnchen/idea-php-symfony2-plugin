package fr.adrienbrault.idea.symfony2plugin.webDeployment.container;

import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.dic.webDeployment.ServiceContainerRemoteFileStorage;
import fr.adrienbrault.idea.symfony2plugin.extension.CompiledServiceBuilderArguments;
import fr.adrienbrault.idea.symfony2plugin.extension.CompiledServiceBuilderFactory;
import fr.adrienbrault.idea.symfony2plugin.webDeployment.utils.RemoteWebServerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RemoteCompiledServiceContainerLoader implements CompiledServiceBuilderFactory {

    @NotNull
    public Builder create() {
        return new Builder() {
            private long remoteBuildTime = -1;

            @Override
            public void build(@NotNull CompiledServiceBuilderArguments args) {
                ServiceContainerRemoteFileStorage extension = getExtension(args.getProject());
                if (extension == null) {
                    return;
                }

                this.remoteBuildTime = extension.getState().getBuildTime();
                args.addStreams(extension.getState().getInputStreams());
            }

            @Override
            public boolean isModified(@NotNull Project project) {
                ServiceContainerRemoteFileStorage extension = getExtension(project);

                long remoteBuildTime = -1;
                if(extension != null) {
                    remoteBuildTime = extension.getState().getBuildTime();
                }

                return remoteBuildTime != this.remoteBuildTime;
            }
        };
    }

    @Nullable
    private static ServiceContainerRemoteFileStorage getExtension(@NotNull Project project) {
        return RemoteWebServerUtil.getExtensionInstance(
            project,
            ServiceContainerRemoteFileStorage.class
        );
    }
}
