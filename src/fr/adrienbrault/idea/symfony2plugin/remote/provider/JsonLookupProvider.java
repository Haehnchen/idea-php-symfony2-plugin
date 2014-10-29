package fr.adrienbrault.idea.symfony2plugin.remote.provider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonLookupProvider implements ProviderInterface {

    private JsonObject jsonObject = null;

    public void collect(JsonElement jsonElement) {

        // reset
        this.jsonObject = null;

        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if(!jsonObject.has("registrar") || !jsonObject.has("providers")) {
            return;
        }

        this.jsonObject = jsonObject;

    }

    public String getAlias() {
        return "lookup";
    }

    public JsonObject getJsonObject() {
        return jsonObject;
    }
}
