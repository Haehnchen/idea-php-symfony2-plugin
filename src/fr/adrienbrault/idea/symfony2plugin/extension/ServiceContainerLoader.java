package fr.adrienbrault.idea.symfony2plugin.extension;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceContainerLoader {
    void attachContainerFile(ServiceContainerLoaderParameter parameter);
}
