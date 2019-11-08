package fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.driver;

import fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.dict.DoctrineMetadataModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface DoctrineMappingDriverInterface {
    DoctrineMetadataModel getMetadata(@NotNull DoctrineMappingDriverArguments arguments);
}
