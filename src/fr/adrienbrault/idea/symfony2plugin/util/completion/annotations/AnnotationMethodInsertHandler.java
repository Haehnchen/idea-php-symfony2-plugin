package fr.adrienbrault.idea.symfony2plugin.util.completion.annotations;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import fr.adrienbrault.idea.symfony2plugin.util.annotation.AnnotationValue;
import org.jetbrains.annotations.NotNull;

public class AnnotationMethodInsertHandler implements InsertHandler<LookupElement> {

    private static final AnnotationMethodInsertHandler instance = new AnnotationMethodInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
        if(lookupElement.getObject() instanceof AnnotationValue) {
            String addText = "=\"\"";

            if(((AnnotationValue) lookupElement.getObject()).getType() == AnnotationValue.Type.Array) {
                addText = "={}";
            }
            PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), addText);

        } else {
            PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "=\"\"");
        }

        context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);
    }

    public static AnnotationMethodInsertHandler getInstance(){
        return instance;
    }

}
