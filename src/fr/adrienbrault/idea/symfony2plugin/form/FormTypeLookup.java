package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeLookup extends LookupElement {

    private String key;
    private String name;

    public FormTypeLookup(String key, String name) {
        this.key = key;
        this.name = name;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return name;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(key);
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.FORM_TYPE);
    }

}