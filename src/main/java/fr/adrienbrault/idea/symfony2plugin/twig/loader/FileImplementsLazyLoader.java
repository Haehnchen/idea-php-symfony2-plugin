package fr.adrienbrault.idea.symfony2plugin.twig.loader;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileImplementsLazyLoader {
    @NotNull
    private final Project project;

    @NotNull
    private final VirtualFile virtualFile;

    @Nullable
    private Collection<VirtualFile> files;

    public FileImplementsLazyLoader(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        this.project = project;
        this.virtualFile = virtualFile;
    }

    @NotNull
    public Collection<VirtualFile> getFiles() {
        if(files != null) {
            return files;
        }

        return files = TwigUtil.getTemplatesExtendingFile(project, virtualFile);
    }
}
