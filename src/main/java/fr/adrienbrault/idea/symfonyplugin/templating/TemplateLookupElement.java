package fr.adrienbrault.idea.symfonyplugin.templating;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    private InsertHandler<LookupElement> insertHandler = null;

    public TemplateLookupElement(@NotNull String templateName, @NotNull VirtualFile virtualFile, @NotNull VirtualFile projectBaseDir) {
        this.templateName = templateName;
        this.virtualFile = virtualFile;
        this.projectBaseDir = projectBaseDir;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return templateName;
    }

    public void handleInsert(InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
        }
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(this.virtualFile.getFileType().getIcon());
        presentation.setTypeText(VfsUtil.getRelativePath(this.virtualFile, this.projectBaseDir, '/'));
        presentation.setTypeGrayed(true);
    }
}
