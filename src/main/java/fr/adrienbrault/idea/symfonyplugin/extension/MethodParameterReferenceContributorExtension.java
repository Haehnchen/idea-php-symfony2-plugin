package fr.adrienbrault.idea.symfonyplugin.extension;

import fr.adrienbrault.idea.symfonyplugin.assistant.reference.MethodParameterSetting;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface MethodParameterReferenceContributorExtension {
    Collection<MethodParameterSetting> getSettings(MethodParameterReferenceContributorParameter parameter);
}
