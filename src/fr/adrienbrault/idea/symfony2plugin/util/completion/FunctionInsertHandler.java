package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import org.jetbrains.annotations.NotNull;

public class FunctionInsertHandler implements InsertHandler<LookupElement> {

    private static final FunctionInsertHandler instance = new FunctionInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {


        String addText = "()";
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), addText);

        context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);

    }

    public static FunctionInsertHandler getInstance(){
        return instance;
    }

}
