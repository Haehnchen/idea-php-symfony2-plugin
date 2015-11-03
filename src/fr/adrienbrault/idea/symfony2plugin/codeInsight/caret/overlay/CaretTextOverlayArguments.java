package fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay;

import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CaretTextOverlayArguments {

    @NotNull
    private final CaretEvent caretEvent;

    @NotNull
    private final PsiFile psiFile;

    @NotNull
    private final PsiElement psiElement;

    public CaretTextOverlayArguments(@NotNull CaretEvent caretEvent, @NotNull PsiFile psiFile, @NotNull PsiElement psiElement) {
        this.caretEvent = caretEvent;
        this.psiFile = psiFile;
        this.psiElement = psiElement;
    }

    @NotNull
    public PsiFile getPsiFile() {
        return psiFile;
    }

    @NotNull
    public PsiElement getPsiElement() {
        return psiElement;
    }

    @NotNull
    public Project getProject() {
        return psiElement.getProject();
    }

    @NotNull
    public CaretEvent getCaretEvent() {
        return caretEvent;
    }
}
