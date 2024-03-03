package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormTypeLookup extends LookupElement {

    private final String key;
    private final String name;
    private boolean isWeak;

    public FormTypeLookup(@Nullable String key, String name) {
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

        presentation.setIcon(isWeak ? Symfony2Icons.FORM_TYPE_WEAK : Symfony2Icons.FORM_TYPE);
    }

    public FormTypeLookup withWeak(boolean isWeak) {
        this.isWeak = isWeak;
        return this;
    }

}