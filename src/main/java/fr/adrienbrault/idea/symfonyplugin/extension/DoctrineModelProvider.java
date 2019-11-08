package fr.adrienbrault.idea.symfonyplugin.extension;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface DoctrineModelProvider {
    Collection<DoctrineModelProviderParameter.DoctrineModel> collectModels(DoctrineModelProviderParameter parameter);
}
