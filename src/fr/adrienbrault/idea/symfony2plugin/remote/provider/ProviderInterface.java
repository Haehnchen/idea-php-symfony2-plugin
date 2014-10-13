package fr.adrienbrault.idea.symfony2plugin.remote.provider;

import com.google.gson.JsonElement;

public interface ProviderInterface {
    public void collect(JsonElement jsonElement);
    public String getAlias();
}
