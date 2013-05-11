package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateLookupElement extends LookupElement {

    private String templateName;
    private TwigFile twigFile;
    private PsiElement psiElement = null;

    @Nullable
    private InsertHandler<LookupElement> insertHandler = null;

    public TemplateLookupElement(String templateName, TwigFile twigFile) {
        this.templateName = templateName;
        this.twigFile = twigFile;
    }

    public TemplateLookupElement(String templateName, TwigFile twigFile, PsiElement psiElement, InsertHandler<LookupElement> insertHandler) {
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
    public PsiElement getObject() {
        return this.psiElement;
    }

    public void handleInsert(InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
        }
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(icons.PhpIcons.TwigFileIcon);
        presentation.setTypeText(VfsUtil.getRelativePath(twigFile.getContainingDirectory().getVirtualFile(), twigFile.getProject().getBaseDir(), '/'));
        presentation.setTypeGrayed(true);
    }

}
