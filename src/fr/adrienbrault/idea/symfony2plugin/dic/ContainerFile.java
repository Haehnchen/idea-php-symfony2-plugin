package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.Nullable;

import java.io.File;

@Tag("container_file")
public class ContainerFile {

    private String path;

    public ContainerFile() {
    }

    public ContainerFile(String path) {
        this.path = path;
    }

    @Attribute("path")
    public String getPath() {
        return path;
    }

    public boolean exists(Project project) {
        if (!FileUtil.isAbsolute(this.path)) {
            return VfsUtil.findRelativeFile(this.path, project.getBaseDir()) != null;
        }

        File file = new File(this.path);
        return file.exists();
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

    public void setPath(String path) {
        this.path = path;
    }

}
