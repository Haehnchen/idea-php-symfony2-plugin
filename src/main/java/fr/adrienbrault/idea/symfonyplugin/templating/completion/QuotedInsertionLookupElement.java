package fr.adrienbrault.idea.symfonyplugin.templating.completion;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class QuotedInsertionLookupElement extends LookupElement {

    @NotNull
    private final LookupElement lookupElement;

    public QuotedInsertionLookupElement(@NotNull LookupElement lookupElement) {
        this.lookupElement = lookupElement;
    }

    @NotNull
    @Override
    public String getLookupString() {
        return this.lookupElement.getLookupString();
    }

    @Nullable
    @Override
    public PsiElement getPsiElement() {
        return lookupElement.getPsiElement();
    }

    @Override
    public boolean isValid() {
        return lookupElement.isValid();
    }

    @Override
    public void handleInsert(InsertionContext context) {
        QuotedInsertHandler.getInstance().handleInsert(context, this);
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
        lookupElement.renderElement(presentation);
    }
}
