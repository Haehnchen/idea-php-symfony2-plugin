package fr.adrienbrault.idea.symfony2plugin.translation.dict;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DomainFileMap {

    private final String loader;
    private final String Path;
    private final String languageKey;
    private final String domain;

    public DomainFileMap(String loader, String Path, String languageKey, String domain) {
        this.loader = loader;
        this.Path = Path;
        this.languageKey = languageKey;
        this.domain = domain;
    }

    public String getLoader() {
        return loader;
    }

    public String getPath() {
        return Path;
    }

    public String getLanguageKey() {
        return languageKey;
    }

    public String getDomain() {
        return domain;
    }

    @Nullable
    public VirtualFile getFile() {
        File file = new File(this.getPath());

        if(!file.exists()) {
              return null;
        }

        return VfsUtil.findFileByIoFile(file, false);
    }

    @Nullable
    public PsiFile getPsiFile(Project project) {
        VirtualFile virtualFile = this.getFile();
        return PsiElementUtils.virtualFileToPsiFile(project, virtualFile);
    }
}
