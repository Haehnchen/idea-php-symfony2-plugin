package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Tag;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.AbstractUiFilePath;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
@Tag("container_file")
public class ContainerFile extends AbstractUiFilePath {

    public ContainerFile() {
    }

    public ContainerFile(String path) {
        this.path = path;
    }

    @Nullable
    public File getFile(Project project) {
        if (!FileUtil.isAbsolute(this.path)) {
            VirtualFile virtualFile = VfsUtil.findRelativeFile(this.path, project.getBaseDir());
            if(virtualFile == null) {
                return null;
            }

            return VfsUtil.virtualToIoFile(virtualFile);
        }

        File file = new File(this.path);
        if(!file.exists()) {
           return null;
        }

        return file;
    }
}
