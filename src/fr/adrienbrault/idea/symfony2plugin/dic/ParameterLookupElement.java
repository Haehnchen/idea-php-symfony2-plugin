package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

public class ParameterLookupElement extends LookupElement {

    private String tag;

    public ParameterLookupElement(String tag) {
        this.tag = tag;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return tag;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.SERVICE_TAG);
    }
}
