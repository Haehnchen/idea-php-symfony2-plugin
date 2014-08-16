package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateLookupElement extends LookupElement {

    private final VirtualFile virtualFile;
    private final VirtualFile projectBaseDir;

    private final String templateName;


    @Nullable
    private InsertHandler<LookupElement> insertHandler = null;

    @Deprecated
    public TemplateLookupElement(@NotNull String templateName, TwigFile twigFile) {
        this(templateName, (PsiFile) twigFile);
    }

    public TemplateLookupElement(@NotNull String templateName, @NotNull PsiFile psiFile) {
        this.templateName = templateName;
        this.virtualFile = psiFile.getVirtualFile();
        this.projectBaseDir = psiFile.getProject().getBaseDir();
    }

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
