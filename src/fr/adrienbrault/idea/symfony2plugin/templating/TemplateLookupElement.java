package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.vfs.VfsUtil;
import com.jetbrains.twig.TwigFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TemplateLookupElement extends LookupElement {

    private String templateName;
    private TwigFile twigFile;

    public TemplateLookupElement(String templateName, TwigFile twigFile) {
        this.templateName = templateName;
        this.twigFile = twigFile;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return templateName;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setIcon(icons.PhpIcons.TwigFileIcon);
        presentation.setTypeText(VfsUtil.getRelativePath(twigFile.getContainingDirectory().getVirtualFile(), twigFile.getProject().getBaseDir(), '/'));
        presentation.setTypeGrayed(true);
    }

}
