package fr.adrienbrault.idea.symfony2plugin.remote.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class TwigProvider implements ProviderInterface {

    private Collection<TwigPath> twigPaths = new ArrayList<TwigPath>();

    public void collect(JsonElement jsonElement) {

        this.twigPaths = new ArrayList<TwigPath>();

        if(!jsonElement.getAsJsonObject().has("namespaces")) {
            return;
        }

        JsonElement namespaces = jsonElement.getAsJsonObject().get("namespaces");
        JsonObject namesapceObject = namespaces.getAsJsonObject();

        if(namesapceObject.has("add_path")) {
            for(Map.Entry<String, JsonElement> addPath : namesapceObject.get("add_path").getAsJsonObject().entrySet()) {
                String twigNamespace = addPath.getKey();
                for(JsonElement path : addPath.getValue().getAsJsonArray()) {
                    if("__main__".equals(twigNamespace)) {
                        twigPaths.add(new TwigPath(path.getAsString()));
                    } else {
                        twigPaths.add(new TwigPath(twigNamespace, path.getAsString()));
                    }
                }
            }
        }

        if(namesapceObject.has("bundle")) {
            for(Map.Entry<String, JsonElement> ns: namesapceObject.get("bundle").getAsJsonObject().entrySet()) {
                twigPaths.add(new TwigPath(ns.getKey(), ns.getValue().getAsString(), TwigPathIndex.NamespaceType.BUNDLE));
            }
        }

    }

    public Collection<TwigPath> getTwigPaths() {
        return twigPaths;
    }

    public String getAlias() {
        return "twig";
    }


}
