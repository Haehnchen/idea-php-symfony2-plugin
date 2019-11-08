package fr.adrienbrault.idea.symfonyplugin.templating.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import fr.adrienbrault.idea.symfonyplugin.util.dict.ResourceFileInsertHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class QuotedInsertHandler implements InsertHandler<LookupElement> {

    @NotNull
    private static final QuotedInsertHandler instance = new QuotedInsertHandler();

    @NotNull
    public static QuotedInsertHandler getInstance(){
        return instance;
    }

    @Override
    public void handleInsert(InsertionContext context, LookupElement lookupElement) {
        if(ResourceFileInsertHandler.isStringBeforeCaret(context.getEditor(), context, "'") ||
           ResourceFileInsertHandler.isStringBeforeCaret(context.getEditor(), context, "\""))
        {
            return;
        }

        int startOffset = context.getStartOffset();
        context.getDocument().insertString(startOffset, "\"");
        context.getDocument().insertString(startOffset + lookupElement.getLookupString().length() + 1, "\"");

        // move to end
        context.getEditor().getCaretModel().moveCaretRelatively(1, 0, false, false, true);
    }
}
