package fr.adrienbrault.idea.symfonyplugin.dic.webDeployment.dict;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceParameterStorage {

    @NotNull
    private final Collection<InputStream> inputStreams;

    private final long buildTime;

    public ServiceParameterStorage(@NotNull Collection<InputStream> inputStreams) {
        this.inputStreams = inputStreams;
        this.buildTime = System.currentTimeMillis();
    }

    @NotNull
    public Collection<InputStream> getInputStreams() {
        return inputStreams;
    }

    public long getBuildTime() {
        return buildTime;
    }
}
