package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dic.DoctrineMetadataModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface DoctrineMappingDriverInterface {
    DoctrineMetadataModel getMetadata(@NotNull DoctrineMappingDriverArguments arguments);
}
