package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import fr.adrienbrault.idea.symfony2plugin.templating.path.dict.TwigPathJson;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigConfigJson {
    private final Collection<TwigPathJson> namespaces = new ArrayList<>();

    public Collection<TwigPathJson> getNamespaces() {
        return namespaces;
    }
}
