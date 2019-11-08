package fr.adrienbrault.idea.symfonyplugin.util.psi;

import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParentPathPatternCondition extends PatternCondition<PsiElement> {

    @NotNull
    private final Class<? extends PsiElement>[] classes;

    public ParentPathPatternCondition(@NotNull Class<? extends PsiElement>... parentClasses) {
        super("Parent path pattern");
        this.classes = parentClasses;
    }

    @Override
    public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
        for (Class<? extends PsiElement> aClass : classes) {
            psiElement = psiElement.getParent();
            if(psiElement == null || !aClass.isInstance(psiElement)) {
                return false;
            }
        }

        return true;
    }
}
