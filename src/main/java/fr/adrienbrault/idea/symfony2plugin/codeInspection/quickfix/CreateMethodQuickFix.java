package fr.adrienbrault.idea.symfony2plugin.codeInspection.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.jetbrains.php.lang.PhpCodeUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.CodeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CreateMethodQuickFix implements LocalQuickFix {
    @NotNull
    private final SmartPsiElementPointer<PhpClass> smartPhpClass;

    @NotNull
    private final String functionName;

    @NotNull
    private final InsertStringInterface stringInterface;

    public CreateMethodQuickFix(@NotNull PhpClass phpClass, @NotNull String functionName, @NotNull InsertStringInterface stringInterface) {
        this.smartPhpClass = SmartPointerManager.getInstance(phpClass.getProject()).createSmartPsiElementPointer(phpClass);
        this.functionName = functionName;
        this.stringInterface = stringInterface;
    }

    @NotNull
    @Override
    public String getName() {
        return "Create Method";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Routing";
    }

    @Override
    public void applyFix(final @NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
        PhpClass phpClass = smartPhpClass.getElement();
        if(phpClass == null) {
            return;
        }

        final Method methodCreated = PhpCodeUtil.createMethodFromTemplate(
            phpClass, project, this.stringInterface.getStringBuilder(problemDescriptor, phpClass, functionName).toString()
        );

        if(methodCreated == null) {
            return;
        }

        final Editor editor = FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, phpClass.getContainingFile().getVirtualFile()), true);
        if(editor == null) {
            return;
        }

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

        final int insertPos = CodeUtil.getMethodInsertPosition(phpClass, functionName);
        if(insertPos == -1) {
            return;
        }

        WriteCommandAction.runWriteCommandAction(project, "Create Method", null, () -> {
            StringBuffer textBuf = new StringBuffer();
            textBuf.append("\n");
            textBuf.append(methodCreated.getText());

            editor.getDocument().insertString(insertPos, textBuf);
            final int endPos = insertPos + textBuf.length();

            CodeStyleManager.getInstance(project).reformatText(phpClass.getContainingFile(), insertPos, endPos);
            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

            Method insertedMethod = phpClass.findMethodByName(functionName);
            if(insertedMethod != null) {
                editor.getCaretModel().moveToOffset(insertedMethod.getTextRange().getStartOffset());
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
            }
        });
    }

    public interface InsertStringInterface {
        @NotNull
        StringBuilder getStringBuilder(@NotNull ProblemDescriptor problemDescriptor, @NotNull PhpClass phpClass, @NotNull String functionName);
    }
}
