package fr.adrienbrault.idea.symfonyplugin.extension;

import fr.adrienbrault.idea.symfonyplugin.assistant.signature.MethodSignatureSetting;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface MethodSignatureTypeProviderExtension {
    Collection<MethodSignatureSetting> getSignatures(MethodSignatureTypeProviderParameter parameter);
}
