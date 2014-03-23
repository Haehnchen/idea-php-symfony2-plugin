package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslatorLookupElement extends LookupElement {

    private String translationString;
    private String domain;
    private boolean isWeak;

    public TranslatorLookupElement(String translation_string, String domain) {
        this.translationString = translation_string;
        this.domain = domain;
    }

    public TranslatorLookupElement(String translationString, String domain, boolean isWeak) {
        this(translationString, domain);
        this.isWeak = isWeak;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return translationString;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(domain);
        presentation.setTypeGrayed(true);

        if(this.isWeak) {
            presentation.setIcon(Symfony2Icons.TRANSLATION_WEAK);
        } else {
            presentation.setIcon(Symfony2Icons.TRANSLATION);
        }

    }

}
