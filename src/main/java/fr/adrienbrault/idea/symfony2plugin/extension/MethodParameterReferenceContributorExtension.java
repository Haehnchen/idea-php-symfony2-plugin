package fr.adrienbrault.idea.symfony2plugin.extension;

import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface MethodParameterReferenceContributorExtension {
    Collection<MethodParameterSetting> getSettings(MethodParameterReferenceContributorParameter parameter);
}
