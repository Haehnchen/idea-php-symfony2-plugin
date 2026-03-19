package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TemplateMoveRenameUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateReference extends PsiPolyVariantReferenceBase<PsiElement> {
    @NotNull
    private final String templateName;

    public TemplateReference(@NotNull StringLiteralExpression element) {
        super(element);
        templateName = element.getContents();
    }

    @NotNull
    @Override
    public Object[] getVariants() {
        return TwigUtil.getAllTemplateLookupElements(getElement().getProject()).toArray();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        return PsiElementResolveResult
            .createResults(TwigUtil.getTemplatePsiElements(getElement().getProject(), templateName));
    }

    /**
     * Called by IntelliJ's move-file refactoring to update the template path when the referenced
     * Twig file is moved to a new location.
     */
    @Override
    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        if (!(element instanceof PsiFile newFile)) {
            return getElement();
        }

        VirtualFile newVirtualFile = newFile.getVirtualFile();
        if (newVirtualFile == null) {
            return getElement();
        }

        String newTemplateName = TemplateMoveRenameUtil.resolveNewTemplateName(
            getElement().getProject(), newVirtualFile, templateName
        );
        if (newTemplateName == null) {
            return getElement();
        }

        PsiElement result = TemplateMoveRenameUtil.applyToStringLiteralElement(getElement(), newTemplateName);
        return result != null ? result : getElement();
    }
}
