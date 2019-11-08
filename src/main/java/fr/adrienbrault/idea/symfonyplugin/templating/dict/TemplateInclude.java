package fr.adrienbrault.idea.symfonyplugin.templating.dict;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateInclude {
    @NotNull
    private final PsiElement psiElement;

    @NotNull
    private final String templateName;

    @NotNull
    private final TYPE type;

    public enum TYPE {
        INCLUDE, INCLUDE_FUNCTION, EMBED, IMPORT, FROM, FORM_THEME
    }

    public TemplateInclude(@NotNull PsiElement psiElement, @NotNull String templateName, @NotNull TYPE type) {
        this.psiElement = psiElement;
        this.templateName = templateName;
        this.type = type;
    }

    @NotNull
    public PsiElement getPsiElement() {
        return psiElement;
    }

    @NotNull
    public String getTemplateName() {
        return templateName;
    }

    @NotNull
    public TYPE getType() {
        return type;
    }
}
