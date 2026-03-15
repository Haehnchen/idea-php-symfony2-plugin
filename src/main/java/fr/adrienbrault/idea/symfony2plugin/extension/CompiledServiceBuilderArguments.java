package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CompiledServiceBuilderArguments {

    public record StreamEntry(@NotNull InputStream stream, @NotNull VirtualFile virtualFile) {}

    @NotNull
    final private Collection<StreamEntry> streams = new ArrayList<>();

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
    public Collection<StreamEntry> getStreams() {
        return streams;
    }

    public void addStream(@NotNull InputStream inputStream, @NotNull VirtualFile virtualFile) {
        streams.add(new StreamEntry(inputStream, virtualFile));
    }
}
