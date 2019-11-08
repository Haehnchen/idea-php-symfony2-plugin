package fr.adrienbrault.idea.symfonyplugin.extension;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CompiledServiceBuilderArguments {

    @NotNull
    final private Collection<InputStream> streams = new ArrayList<>();

    @NotNull
    private final Project project;

    public CompiledServiceBuilderArguments(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public Collection<InputStream> getStreams() {
        return streams;
    }

    public void addStream(@NotNull InputStream inputStream) {
        streams.add(inputStream);
    }

    public void addStreams(@NotNull Collection<InputStream>  inputStream) {
        streams.addAll(inputStream);
    }
}
