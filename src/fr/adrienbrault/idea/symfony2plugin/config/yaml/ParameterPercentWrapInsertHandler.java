package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import org.jetbrains.annotations.NotNull;

public class ParameterPercentWrapInsertHandler implements InsertHandler<LookupElement> {

    private static final ParameterPercentWrapInsertHandler instance = new ParameterPercentWrapInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
        StringBuilder textToInsertBuilder = new StringBuilder();
        textToInsertBuilder.append("%");
        context.getDocument().insertString(context.getStartOffset(), textToInsertBuilder);
        context.getDocument().insertString(context.getTailOffset(), "%");
        context.getEditor().getCaretModel().moveCaretRelatively(1, 0, false, false, true);
    }

    public static ParameterPercentWrapInsertHandler getInstance(){
        return instance;
    }

}
