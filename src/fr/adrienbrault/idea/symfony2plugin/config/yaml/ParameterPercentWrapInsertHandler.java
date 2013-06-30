package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class ParameterPercentWrapInsertHandler implements InsertHandler<LookupElement> {

    private static final ParameterPercentWrapInsertHandler instance = new ParameterPercentWrapInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {

        String insertText = null;
        if((lookupElement.getObject() instanceof PsiElement)) {

            PsiElement psi = (PsiElement) lookupElement.getObject();
            insertText = psi.getText();
            return;
        }

        if((lookupElement.getObject() instanceof String)) {
            insertText = (String) lookupElement.getObject();
        }

        if(insertText == null) {
            return;
        }

        if(!insertText.startsWith("%")) {
            context.getDocument().insertString(context.getStartOffset(), "%");
        }

        // %| is also fired
        if(!insertText.endsWith("%") || insertText.length() == 1) {
            context.getDocument().insertString(context.getTailOffset(), "%");
            context.getEditor().getCaretModel().moveCaretRelatively(1, 0, false, false, true);
        }

    }

    public static ParameterPercentWrapInsertHandler getInstance(){
        return instance;
    }

}
