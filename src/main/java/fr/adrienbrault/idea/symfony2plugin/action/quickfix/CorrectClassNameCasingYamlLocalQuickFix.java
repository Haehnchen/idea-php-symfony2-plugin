package fr.adrienbrault.idea.symfony2plugin.action.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlPsiElementFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class CorrectClassNameCasingYamlLocalQuickFix implements LocalQuickFix {

    private final String replacementFQN;

    public CorrectClassNameCasingYamlLocalQuickFix(String replacementFQN) {

        this.replacementFQN = replacementFQN;
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getFamilyName() {
        return "YAML";
    }

    @Nls(capitalization = Nls.Capitalization.Sentence)
    @NotNull
    @Override
    public String getName() {
        return "Use " + replacementFQN;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        PsiElement psiElement1 = descriptor.getPsiElement();
        YAMLKeyValue replacement = YamlPsiElementFactory.createFromText(
                project,
                YAMLKeyValue.class,
                "class: " + replacementFQN
        );

        if (replacement != null && replacement.getValue() != null) {
            psiElement1.replace(replacement.getValue());
        }
    }
}
