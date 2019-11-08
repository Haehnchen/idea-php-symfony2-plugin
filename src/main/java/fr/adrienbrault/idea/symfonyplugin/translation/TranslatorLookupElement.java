package fr.adrienbrault.idea.symfonyplugin.translation;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslatorLookupElement extends LookupElement {
    @NotNull
    private String translationString;

    @NotNull
    private String domain;

    @Nullable
    private InsertHandler<TranslatorLookupElement> insertHandler;

    private boolean isWeak;

    public TranslatorLookupElement(@NotNull String translationString, @NotNull String domain) {
        this.translationString = translationString;
        this.domain = domain;
    }

    public TranslatorLookupElement(@NotNull String translationString, @NotNull String domain, @NotNull InsertHandler<TranslatorLookupElement> insertHandler) {
        this.translationString = translationString;
        this.domain = domain;
        this.insertHandler = insertHandler;
    }

    public TranslatorLookupElement(@NotNull String translationString, @NotNull String domain, boolean isWeak) {
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

    @Override
    public void handleInsert(InsertionContext context) {
        if(insertHandler != null) {
            insertHandler.handleInsert(context, this);
        }
    }

    @NotNull
    public String getDomain() {
        return domain;
    }
}
