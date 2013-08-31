package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateLookupElement extends LookupElement {

    private String templateName;
    private PsiFile twigFile;
    private PsiElement psiElement = null;

    @Nullable
    private InsertHandler<LookupElement> insertHandler = null;

    @Deprecated
    public TemplateLookupElement(String templateName, TwigFile twigFile) {
        this(templateName, (PsiFile) twigFile);
    }

    public TemplateLookupElement(String templateName, PsiFile psiFile) {
        this.templateName = templateName;
        this.twigFile = psiFile;
    }

    public TemplateLookupElement(String templateName, PsiFile twigFile, PsiElement psiElement, InsertHandler<LookupElement> insertHandler) {
        this.templateName = templateName;
        this.twigFile = twigFile;
        this.insertHandler = insertHandler;
        this.psiElement = psiElement;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return templateName;
    }

    @NotNull
    public Object getObject() {
        return this.psiElement != null ? this.psiElement : super.getObject();
    }

    public void handleInsert(InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
        }
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(this.twigFile.getIcon(Iconable.ICON_FLAG_VISIBILITY));
        presentation.setTypeText(VfsUtil.getRelativePath(twigFile.getContainingDirectory().getVirtualFile(), twigFile.getProject().getBaseDir(), '/'));
        presentation.setTypeGrayed(true);
    }

}
