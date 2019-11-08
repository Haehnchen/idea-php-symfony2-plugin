package fr.adrienbrault.idea.symfonyplugin.config.component.parser;

import fr.adrienbrault.idea.symfonyplugin.config.component.ParameterLookupElement;
import fr.adrienbrault.idea.symfonyplugin.dic.ContainerParameter;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterLookupPercentElement extends ParameterLookupElement {

    public ParameterLookupPercentElement(@NotNull ContainerParameter containerParameter) {
        super(containerParameter);
    }

    @NotNull
    public String getLookupString() {
        return "%" + containerParameter.getName() + "%";
    }
}
