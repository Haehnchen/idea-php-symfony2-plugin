package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Tag;
import fr.adrienbrault.idea.symfony2plugin.ui.dict.AbstractUiFilePath;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.Nullable;

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
    public VirtualFile getVirtualFile(Project project) {
        if (!FileUtil.isAbsolute(this.path)) {
            return VfsUtil.findRelativeFile(this.path, ProjectUtil.getProjectDir(project));
        }

        VirtualFile virtualFile = VfsUtil.findFileByIoFile(new java.io.File(this.path), false);
        if (virtualFile == null || !virtualFile.exists()) {
            return null;
        }

        return virtualFile;
    }
}
