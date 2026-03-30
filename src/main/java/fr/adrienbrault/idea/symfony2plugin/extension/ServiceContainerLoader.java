package fr.adrienbrault.idea.symfony2plugin.extension;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @deprecated Extension point removed
 */
@Deprecated
public interface ServiceContainerLoader {
    void attachContainerFile(ServiceContainerLoaderParameter parameter);
}
