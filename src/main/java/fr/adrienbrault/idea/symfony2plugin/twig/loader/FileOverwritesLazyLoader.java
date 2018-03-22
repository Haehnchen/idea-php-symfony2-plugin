package fr.adrienbrault.idea.symfony2plugin.twig.loader;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileOverwritesLazyLoader {
    @NotNull
    private final Project project;

    @NotNull
    private final Map<VirtualFile, Collection<VirtualFile>> fileScope = new HashMap<>();

    @NotNull
    private final Map<VirtualFile, Collection<VirtualFile>> selfScope = new HashMap<>();

    public FileOverwritesLazyLoader(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public Collection<VirtualFile> getFiles(boolean includeSelf, @NotNull Collection<VirtualFile> virtualFiles) {
        Map<VirtualFile, Collection<VirtualFile>> target;

        if(includeSelf) {
            // {{ block("foo") }}
            target = selfScope;
        } else {
            target = fileScope;
        }

        Collection<VirtualFile> virtualFileCollection = new HashSet<>();

        for (VirtualFile virtualFile : virtualFiles) {
            if(!target.containsKey(virtualFile)) {
                PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                if(file != null) {
                    target.put(virtualFile, TwigFileUtil.collectParentFiles(includeSelf, file));
                } else {
                    target.put(virtualFile, Collections.emptySet());
                }
            }

            virtualFileCollection.addAll(target.get(virtualFile));
        }

        return virtualFileCollection;
    }
}
