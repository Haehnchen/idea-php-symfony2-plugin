package fr.adrienbrault.idea.symfonyplugin.config.xml.inspection;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlDuplicateParameterKeyInspection extends XmlDuplicateServiceKeyInspection {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitFile(PsiFile file) {
                visitRoot(file, holder, "parameters", "parameter", "key");
            }
        };
    }

}
