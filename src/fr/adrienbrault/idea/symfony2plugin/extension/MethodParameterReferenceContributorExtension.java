package fr.adrienbrault.idea.symfony2plugin.extension;


import fr.adrienbrault.idea.symfony2plugin.assistant.reference.MethodParameterSetting;

import java.util.Collection;

public interface MethodParameterReferenceContributorExtension {
    public Collection<MethodParameterSetting> getSettings(MethodParameterReferenceContributorParameter parameter);
}
