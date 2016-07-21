package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import fr.adrienbrault.idea.symfony2plugin.profiler.ProfilerIndex;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;

public class ProfilerRequest {

    private ArrayList<String> separatedLine = new ArrayList<>();
    private ProfilerIndex profilerIndex;

    public ProfilerRequest(String[] separatedLine, ProfilerIndex profilerIndex) {
        Collections.addAll(this.separatedLine, separatedLine);
        this.profilerIndex = profilerIndex;
    }

    public String getHash() {
        return this.separatedLine.get(0);
    }

    public String getHost() {
        return this.separatedLine.get(1);
    }

    public String getMethod() {
        return this.separatedLine.get(2);
    }

    public String getUrl() {
        return this.separatedLine.get(3);
    }

    public String getTimestamp() {
        return this.separatedLine.get(4);
    }

    @Nullable
    public String getContent() {
        return this.profilerIndex.getContent(this);
    }

    public <T extends CollectorInterface> T getCollector(Class<T> classFactory) {
        T factory = null;
        try {
            factory = classFactory.newInstance();
            factory.setProfilerRequest(this);
        } catch (InstantiationException | IllegalAccessException ignored) {
        }

        return factory;
    }

}
