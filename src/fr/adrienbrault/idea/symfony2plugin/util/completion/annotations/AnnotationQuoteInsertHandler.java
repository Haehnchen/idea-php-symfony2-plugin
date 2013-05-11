package fr.adrienbrault.idea.symfony2plugin.util.completion.annotations;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class AnnotationQuoteInsertHandler implements InsertHandler<LookupElement> {

    private static final AnnotationQuoteInsertHandler instance = new AnnotationQuoteInsertHandler();

    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {


        PsiElement psi = (PsiElement) lookupElement.getObject();

        //PhpInsertHandlerUtil.insertStringAtCaret(context.getEditor(), "()");
        // IntellijIdeaRulezzz :)
        String annotationText = psi.getParent().getText();
        if(!(annotationText.startsWith("('") || annotationText.startsWith("(\""))) {
            context.getDocument().insertString(context.getStartOffset(), new StringBuilder().append("'"));
        }



        if(!(annotationText.endsWith("')") || annotationText.endsWith("\")"))) {
            context.getDocument().insertString(context.getTailOffset(), new StringBuilder().append("'"));
        }
        context.getEditor().getCaretModel().moveCaretRelatively(1, 0, false, false, true);
    }

    public static AnnotationQuoteInsertHandler getInstance(){
        return instance;
    }

}
