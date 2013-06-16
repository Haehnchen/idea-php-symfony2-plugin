package fr.adrienbrault.idea.symfony2plugin.config.component;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterLookupElement extends LookupElement {

    private String parameterKey;
    private String parameterValue;
    private PsiElement psiElement;
    private InsertHandler<LookupElement> insertHandler = null;

    public ParameterLookupElement(String parameterKey, String parameterValue) {
        this.parameterKey = parameterKey;
        this.parameterValue = parameterValue;
    }

    public ParameterLookupElement(String name, String value, InsertHandler<LookupElement> insertHandler, PsiElement psiElement) {
        this(name, value);
        this.insertHandler = insertHandler;
        this.psiElement = psiElement;
    }

    @NotNull
    @Override
    public Object getObject() {
        return this.psiElement != null ? this.psiElement : this;
    }

    public void handleInsert(InsertionContext context) {
        if (this.insertHandler != null) {
            this.insertHandler.handleInsert(context, this);
        }
    }

    @NotNull
    @Override
    public String getLookupString() {
        return parameterKey;
    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeText(parameterValue);
        presentation.setTypeGrayed(true);
        presentation.setIcon(Symfony2Icons.PARAMETER);
    }

}