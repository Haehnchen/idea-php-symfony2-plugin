package fr.adrienbrault.idea.symfony2plugin.extension;

import fr.adrienbrault.idea.symfony2plugin.assistant.signature.MethodSignatureSetting;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface MethodSignatureTypeProviderExtension {
    Collection<MethodSignatureSetting> getSignatures(MethodSignatureTypeProviderParameter parameter);
}
