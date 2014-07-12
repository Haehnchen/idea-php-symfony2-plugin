package fr.adrienbrault.idea.symfony2plugin.extension;

import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting;

import java.util.Collection;

public interface MethodSignatureTypeProviderExtension {
    public Collection<MethodSignatureSetting> getSignatures(MethodSignatureTypeProviderParameter parameter);
}
