package fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

public class YamlDuplicateParameterKeyInspection extends YamlDuplicateServiceKeyInspection {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            visitRoot(psiFile, "parameters", holder);
        }

        return super.buildVisitor(holder, isOnTheFly);
    }

}
