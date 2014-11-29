package fr.adrienbrault.idea.symfony2plugin.completion.lookup;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;

public class PostfixTemplateLookupElement extends LookupElement {

    private final String template;
    private final String expr;

    public PostfixTemplateLookupElement(String template, String expr) {
        this.template = template;
        this.expr = expr;
    }

    @Override
    public void handleInsert(InsertionContext context) {
        int lengthOfTypedKey = context.getTailOffset() - context.getStartOffset();


        PsiFile file = context.getFile();
        PsiElement point = PsiUtilCore.getElementAtOffset(file, context.getStartOffset() - 1);

        String templateKey = expr;
        Editor editor = context.getEditor();

        PsiElement psiElement = point.getPrevSibling();

        String text = psiElement.getText();
        templateKey = templateKey.replace("$EXPR$", text);

        int startOffset = psiElement.getTextRange().getStartOffset();
        context.getDocument().deleteString(startOffset, context.getTailOffset());

        if (lengthOfTypedKey < templateKey.length()) {

            int endChar = templateKey.indexOf("$END$");
            if(endChar > 0) {
                templateKey = templateKey.replace("$END$", "");
                context.getDocument().insertString(startOffset, templateKey);
                editor.getCaretModel().moveToOffset(startOffset + endChar);
            } else {
                context.getDocument().insertString(startOffset, templateKey);
            }

            CodeStyleManager.getInstance(context.getProject()).reformatRange(file, startOffset, startOffset + templateKey.length(), true);
            PsiDocumentManager.getInstance(context.getProject()).commitDocument(editor.getDocument());
        }

    }

    public void renderElement(LookupElementPresentation presentation) {
        presentation.setItemText(getLookupString());
        presentation.setTypeGrayed(true);
        presentation.setTypeText(expr);
    }

    @NotNull
    @Override
    public String getLookupString() {
        return template;
    }

}
