package fr.adrienbrault.idea.symfony2plugin.util.completion;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.jetbrains.php.completion.insert.PhpReferenceInsertHandler;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpReferenceTrimBackslashInsertHandler implements InsertHandler<LookupElement> {

    private static final PhpReferenceTrimBackslashInsertHandler instance = new PhpReferenceTrimBackslashInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {

        // reuse core class + namespace insertHandler
        PhpReferenceInsertHandler.getInstance().handleInsert(context, lookupElement);

        // phpstorm8: remove leading backslash on PhpReferenceInsertHandler
        String backslash = context.getDocument().getText(new TextRange(context.getStartOffset(), context.getStartOffset() + 1));
        if("\\".equals(backslash)) {
            context.getDocument().deleteString(context.getStartOffset(), context.getStartOffset() + 1);
        }

    }

    public static PhpReferenceTrimBackslashInsertHandler getInstance(){
        return instance;
    }

}

