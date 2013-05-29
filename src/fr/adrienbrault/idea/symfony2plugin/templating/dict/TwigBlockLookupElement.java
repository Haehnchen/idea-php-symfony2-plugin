package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;

import icons.PhpIcons;
import org.jetbrains.annotations.NotNull;

public class TwigBlockLookupElement extends LookupElement {

    private TwigBlock twigBlock;

    public TwigBlockLookupElement(TwigBlock twigBlock) {
        this.twigBlock = twigBlock;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return twigBlock.getName();
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(twigBlock.getShortcutName());
        presentation.setTypeGrayed(true);
        presentation.setIcon(PhpIcons.TwigFileIcon);
    }
}
