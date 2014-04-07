package fr.adrienbrault.idea.symfony2plugin.extension;

import java.util.Collection;

public interface DoctrineModelProvider {
    public Collection<DoctrineModelProviderParameter.DoctrineModel> collectModels(DoctrineModelProviderParameter parameter);
}
