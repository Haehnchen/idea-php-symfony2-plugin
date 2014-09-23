package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;

public class DuplicateLocalRouteInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        // @TODO: detection of routing files in right way
        // routing.yml
        // comment.routing.yml
        // routing/foo.yml
        if(YamlHelper.isRoutingFile(psiFile)) {
            YAMLDocument document = PsiTreeUtil.findChildOfType(psiFile, YAMLDocument.class);
            if(document != null) {
                YamlHelper.attachDuplicateKeyInspection(document, holder);
            }
        }

        return super.buildVisitor(holder, isOnTheFly);
    }

}
