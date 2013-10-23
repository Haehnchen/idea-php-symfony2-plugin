package fr.adrienbrault.idea.symfony2plugin;


import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class EditorCustomization implements com.intellij.ui.EditorCustomization {
    @Override
    public void customize(@NotNull EditorEx editor) {

        Project project = editor.getProject();
        if (project == null) {
            return;
        }

        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) {
            return;
        }

        System.out.println(file.getName());
    }
}
