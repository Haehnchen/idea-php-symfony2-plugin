package fr.adrienbrault.idea.symfony2plugin.codeInsight;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * @deprecated Use core features
 */
@Deprecated
public abstract class GotoCompletionProvider implements GotoCompletionProviderInterfaceEx {
    @NotNull
    private final PsiElement element;
    private final Project project;

    public GotoCompletionProvider(@NotNull PsiElement element) {
        this.element = element;
        this.project = element.getProject();
    }

    @NotNull
    protected Project getProject() {
        return this.project;
    }

    @NotNull
    protected PsiElement getElement() {
        return this.element;
    }

    @NotNull
    @Override
    public Collection<LookupElement> getLookupElements() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<PsiElement> getPsiTargets(PsiElement element) {
        return Collections.emptyList();
    }
}
