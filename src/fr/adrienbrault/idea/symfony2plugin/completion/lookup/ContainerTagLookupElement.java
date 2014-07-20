package fr.adrienbrault.idea.symfony2plugin.completion.lookup;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

public class ContainerTagLookupElement extends LookupElement {

    final private String tag;
    private boolean isWeak = false;

    public ContainerTagLookupElement(String tag) {
        this.tag = tag;
    }

    public ContainerTagLookupElement(String tag, boolean isWeak) {
        this.tag = tag;
        this.isWeak = isWeak;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return tag;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeGrayed(true);
        presentation.setIcon(isWeak ? Symfony2Icons.SERVICE_TAG_WEAK : Symfony2Icons.SERVICE_TAG);
    }
}
