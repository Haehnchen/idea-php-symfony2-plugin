package fr.adrienbrault.idea.symfony2plugin.dic.webDeployment.dict;

import org.jetbrains.annotations.NotNull;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceParameterStorage {

    @NotNull
    private final Map<String, String> serviceMap;

    @NotNull
    private final Map<String, String> parameterMap;

    public ServiceParameterStorage(@NotNull Map<String, String> serviceMap, @NotNull Map<String, String> parameterMap) {
        this.serviceMap = serviceMap;
        this.parameterMap = parameterMap;
    }

    @NotNull
    public Map<String, String> getServiceMap() {
        return serviceMap;
    }

    @NotNull
    public Map<String, String> getParameterMap() {
        return parameterMap;
    }
}
