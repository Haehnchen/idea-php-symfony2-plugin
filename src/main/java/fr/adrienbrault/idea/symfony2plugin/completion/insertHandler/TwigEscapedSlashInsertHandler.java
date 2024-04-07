package fr.adrienbrault.idea.symfony2plugin.completion.insertHandler;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import com.jetbrains.php.lang.psi.elements.PhpClassMember;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * "Foo\Foo" => "Foo\\Foo"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigEscapedSlashInsertHandler implements InsertHandler<LookupElement> {
    private static final TwigEscapedSlashInsertHandler instance = new TwigEscapedSlashInsertHandler();

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement item) {
        if (!(item.getObject() instanceof SmartPsiElementPointer<?> smartPsiElementPointer)) {
            return;
        }

        String classFqn = null;
        String fieldName = null;

        if (smartPsiElementPointer.getElement() instanceof PhpClassMember phpClassMember) {
            classFqn = phpClassMember.getContainingClass().getFQN();
            fieldName = phpClassMember.getName();
        }

        Document document = context.getDocument();
        document.deleteString(context.getStartOffset(), context.getTailOffset());

        String s = StringUtils.stripStart(classFqn, "\\").replace("\\", "\\\\") + "::" + fieldName;
        document.insertString(context.getStartOffset(), s);
        context.commitDocument();

        PsiElement elementAt = context.getFile().findElementAt(context.getEditor().getCaretModel().getOffset());
        if (elementAt == null) {
            return;
        }

        context.getEditor().getCaretModel().moveCaretRelatively(s.length(), 0, false, false, true);
    }

    public static TwigEscapedSlashInsertHandler getInstance(){
        return instance;
    }
}
