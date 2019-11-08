package fr.adrienbrault.idea.symfonyplugin;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfonyplugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfonyplugin.extension.ServiceContainerLoader;
import fr.adrienbrault.idea.symfonyplugin.extension.ServiceContainerLoaderParameter;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerSettingsLoader implements ServiceContainerLoader {

    private static Condition<ContainerFile> CONDITION = new ContainerFileCondition();

    @Override
    public void attachContainerFile(ServiceContainerLoaderParameter parameter) {

        List<ContainerFile> settingsContainerFiles = Settings.getInstance(parameter.getProject()).containerFiles;
        if(settingsContainerFiles == null) {
            return;
        }

        List<ContainerFile> filter = ContainerUtil.filter(settingsContainerFiles, CONDITION);
        parameter.addContainerFiles(filter);
    }

    private static class ContainerFileCondition implements Condition<ContainerFile> {
        @Override
        public boolean value(ContainerFile containerFile) {
            return containerFile.getPath() != null && !containerFile.getPath().startsWith("remote://");
        }
    }
}
