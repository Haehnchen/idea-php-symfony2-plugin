package fr.adrienbrault.idea.symfonyplugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DuplicateLocalRouteInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        public MyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitFile(PsiFile file) {
            // @TODO: detection of routing files in right way
            // routing.yml
            // comment.routing.yml
            // routing/foo.yml
            if(!YamlHelper.isRoutingFile(file)) {
                return;
            }

            YAMLDocument document = PsiTreeUtil.findChildOfType(file, YAMLDocument.class);
            if(document == null) {
                return;
            }

            YAMLValue topLevelValue = document.getTopLevelValue();
            if(topLevelValue != null) {
                YamlHelper.attachDuplicateKeyInspection(topLevelValue, holder);
            }
        }
    }
}
