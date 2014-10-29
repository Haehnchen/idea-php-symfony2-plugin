package fr.adrienbrault.idea.symfony2plugin.remote.completion.util;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.remote.completion.json.JsonRawLookupElement;
import fr.adrienbrault.idea.symfony2plugin.remote.completion.json.JsonRegistrar;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.*;

public class RemoteJsonUtil {

    public static Collection<JsonRawLookupElement> getProviderJsonFromFile(Project project, Set<String> providerSet) {

        VirtualFile virtualFile = VfsUtil.findRelativeFile(project.getBaseDir(), "_remote.json");
        if(virtualFile == null) {
            return Collections.emptyList();
        }

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(VfsUtil.virtualToIoFile(virtualFile)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            return Collections.emptyList();
        }

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(br).getAsJsonObject();

        return getProviderJsonRawLookupElements(providerSet, jsonObject);
    }

    public static Collection<JsonRawLookupElement> getProviderJsonRawLookupElements(Set<String> providerSet, JsonObject jsonObject) {

        if(!jsonObject.has("providers")) {
            return Collections.emptyList();
        }

        JsonObject providers = jsonObject.get("providers").getAsJsonObject();

        Collection<JsonRawLookupElement> jsonLookupElements = new ArrayList<JsonRawLookupElement>();
        Gson gson = new Gson();
        for(String provider: providerSet) {

            if(!providers.has(provider)) {
                continue;
            }

            JsonObject providerFoo = providers.get(provider).getAsJsonObject();
            if(!providerFoo.has("lookup_items")) {
                continue;
            }

            Type listType = new TypeToken<List<JsonRawLookupElement>>(){}.getType();

            Collection<JsonRawLookupElement> lookup_items = gson.fromJson(providerFoo.get("lookup_items"), listType);
            jsonLookupElements.addAll(lookup_items);
        }

        return jsonLookupElements;
    }

    public static Collection<JsonRegistrar> getRegistrarJsonFromFile(Project project) {

        VirtualFile virtualFile = VfsUtil.findRelativeFile(project.getBaseDir(), "_remote.json");
        if(virtualFile == null) {
            return Collections.emptyList();
        }

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(VfsUtil.virtualToIoFile(virtualFile)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();

            return Collections.emptyList();
        }

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = jsonParser.parse(br).getAsJsonObject();

        if(!jsonObject.has("registrar")) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<JsonRegistrar>>(){}.getType();
        return new Gson().fromJson(jsonObject.get("registrar"), listType);
    }

    public static Collection<JsonRegistrar> getJsonRegistrars(JsonObject jsonObject) {

        if(!jsonObject.has("registrar")) {
            return Collections.emptyList();
        }

        Type listType = new TypeToken<List<JsonRegistrar>>(){}.getType();
        return new Gson().fromJson(jsonObject.get("registrar"), listType);
    }

}
