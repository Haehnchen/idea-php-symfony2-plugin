package fr.adrienbrault.idea.symfony2plugin.ui.dict;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public abstract class AbstractUiFilePath implements UiFilePathInterface {

    protected String path;

    public boolean exists(@NotNull Project project) {
        if (!FileUtil.isAbsolute(this.path)) {
            return VfsUtil.findRelativeFile(this.path, ProjectUtil.getProjectDir(project)) != null;
        }

        return new File(this.path).exists();
    }

    @Attribute("path")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean isRemote() {
        return this.path != null && this.path.startsWith("remote://");
    }
}
