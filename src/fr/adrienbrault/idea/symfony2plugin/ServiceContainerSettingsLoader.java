package fr.adrienbrault.idea.symfony2plugin;

import fr.adrienbrault.idea.symfony2plugin.dic.ContainerFile;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoader;
import fr.adrienbrault.idea.symfony2plugin.extension.ServiceContainerLoaderParameter;

import java.util.List;

public class ServiceContainerSettingsLoader implements ServiceContainerLoader {
    @Override
    public void attachContainerFile(ServiceContainerLoaderParameter parameter) {

        List<ContainerFile> settingsContainerFiles = Settings.getInstance(parameter.getProject()).containerFiles;
        if(settingsContainerFiles == null) {
            return;
        }

        parameter.addContainerFiles(settingsContainerFiles);
    }
}
