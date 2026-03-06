package fr.adrienbrault.idea.symfony2plugin.templating.inlay;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Inserts Twig code snippets at the current caret position.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariableInsertHandler {

    private final Editor editor;
    private final PsiFile file;

    public TwigVariableInsertHandler(@NotNull Editor editor, @NotNull PsiFile file) {
        this.editor = editor;
        this.file = file;
    }

    /** Insert {{ variable }} */
    public void insertPrintBlock(@NotNull String expression) {
        insertAtCaret("{{ " + expression + " }}");
    }

    /** Insert {% if variable %}...{% endif %} */
    public void insertIfStatement(@NotNull String variable) {
        insertAtCaret("{% if " + variable + " %}\n    \n{% endif %}");
    }

    /** Insert {% for item in variable %}...{% endfor %} */
    public void insertEmptyForeach(@NotNull String variable) {
        String item = singularize(variable);
        insertAtCaret("{% for " + item + " in " + variable + " %}\n    \n{% endfor %}");
    }

    /** Insert {% for item in variable.property %}{{ item }}{% endfor %} */
    public void insertFilledForeach(@NotNull String variable, @NotNull String property) {
        String item = singularize(property);
        insertAtCaret("{% for " + item + " in " + variable + "." + property + " %}\n    {{ " + item + " }}\n{% endfor %}");
    }

    /** Insert {% for item in variable %}{{ item.property }}{% endfor %} */
    public void insertForeachPropertyAccess(@NotNull String variable, @NotNull String property) {
        String item = singularize(variable);
        insertAtCaret("{% for " + item + " in " + variable + " %}\n    {{ " + item + "." + property + " }}\n{% endfor %}");
    }

    private void insertAtCaret(@NotNull String text) {
        WriteCommandAction.runWriteCommandAction(file.getProject(), () -> {
            Document document = editor.getDocument();
            int caretOffset = editor.getCaretModel().getOffset();
            int lineNumber = document.getLineNumber(caretOffset);
            int lineEnd = document.getLineEndOffset(lineNumber);

            document.insertString(lineEnd, "\n" + text);

            int newOffset = lineEnd + 1 + findInnerOffset(text);
            editor.getCaretModel().moveToOffset(newOffset);
        });
    }

    private int findInnerOffset(@NotNull String text) {
        int innerLine = text.indexOf("\n    \n");
        if (innerLine >= 0) return innerLine + 5;
        return text.length();
    }

    @NotNull
    private static String singularize(@NotNull String word) {
        if (word.endsWith("s")) {
            String unpluralized = StringUtil.unpluralize(word);
            if (unpluralized != null && !unpluralized.isBlank() && !unpluralized.equals(word)) {
                return unpluralized;
            }
        }

        if (word.endsWith("ies")) return word.substring(0, word.length() - 3) + "y";
        if (word.endsWith("ches") || word.endsWith("shes") || word.endsWith("sses") || word.endsWith("xes") || word.endsWith("zes") || word.endsWith("oes")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && !word.endsWith("ss")) return word.substring(0, word.length() - 1);
        return word + "Item";
    }
}
