package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslatorLookupElement extends LookupElement {

    private String translation_string;
    private String domain;

    public TranslatorLookupElement(String translation_string, String domain) {
        this.translation_string = translation_string;
        this.domain = domain;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return translation_string;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(domain);
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.TRANSLATION);
    }

}
