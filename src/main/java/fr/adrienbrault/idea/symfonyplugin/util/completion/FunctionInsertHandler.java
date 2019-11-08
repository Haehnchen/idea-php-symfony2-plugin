package fr.adrienbrault.idea.symfonyplugin.util.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FunctionInsertHandler implements InsertHandler<LookupElement> {

    private static final FunctionInsertHandler instance = new FunctionInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {

        // check this:
        // {{ form_javasc|() }}
        // {{ form_javasc| }}
        if(PhpInsertHandlerUtil.isStringAtCaret(context.getEditor(), "(")) {
            return;
        }

        String addText = "()";
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), addText);

        context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);

    }

    public static FunctionInsertHandler getInstance(){
        return instance;
    }

}
