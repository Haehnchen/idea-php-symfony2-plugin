package fr.adrienbrault.idea.symfonyplugin.util.completion.annotations;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import fr.adrienbrault.idea.symfonyplugin.util.annotation.AnnotationIndex;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationTagInsertHandler implements InsertHandler<LookupElement> {

    private static final AnnotationTagInsertHandler instance = new AnnotationTagInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
        PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "()");
        context.getEditor().getCaretModel().moveCaretRelatively(-1, 0, false, false, true);

        String lookupString = lookupElement.getLookupString();
        if(AnnotationIndex.getControllerAnnotations().containsKey(lookupString)) {
            String useStatement = AnnotationIndex.getControllerAnnotations().get(lookupString).getUse();
            if(null != useStatement) {
                AnnotationUseImporter.insertUse(context, useStatement);
            }
        }

    }

    public static AnnotationTagInsertHandler getInstance(){
        return instance;
    }

}
