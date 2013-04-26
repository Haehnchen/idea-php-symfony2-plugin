package fr.adrienbrault.idea.symfony2plugin.asset;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class AssetLookupElement extends LookupElement {
    VirtualFile file;
    VirtualFile webDirectory;
    Project project;

    public AssetLookupElement(VirtualFile file, VirtualFile webDirectory, Project project) {
        this.file = file;
        this.webDirectory = webDirectory;
        this.project = project;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return "" + VfsUtil.getRelativePath(file, webDirectory, '/');
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(IconUtil.getIcon(file, 0, project));
    }
}
