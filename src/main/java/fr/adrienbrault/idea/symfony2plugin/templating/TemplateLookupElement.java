package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateLookupElement extends LookupElement {
    @NotNull
    private final VirtualFile virtualFile;

    @NotNull
    private final VirtualFile projectBaseDir;

    @NotNull
    private final String templateName;

    private boolean bold = false;

    public TemplateLookupElement(@NotNull String templateName, @NotNull VirtualFile virtualFile, @NotNull VirtualFile projectBaseDir) {
        this.templateName = templateName;
        this.virtualFile = virtualFile;
        this.projectBaseDir = projectBaseDir;
    }

    public TemplateLookupElement(@NotNull String templateName, @NotNull VirtualFile virtualFile, @NotNull VirtualFile projectBaseDir, boolean bold) {
        this(templateName, virtualFile, projectBaseDir);
        this.bold = bold;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return templateName;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(this.virtualFile.getFileType().getIcon());
        presentation.setTypeText(VfsUtil.getRelativePath(this.virtualFile, this.projectBaseDir, '/'));
        presentation.setTypeGrayed(true);
        presentation.setItemTextBold(bold);
    }
}
