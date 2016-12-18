package fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLMapping;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlDuplicateServiceKeyInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitFile(PsiFile file) {
                visitRoot(file, "services", holder);
            }
        };
    }

    protected void visitRoot(PsiFile psiFile, String rootName, @NotNull ProblemsHolder holder) {
        if(!(psiFile instanceof YAMLFile)) {
            return;
        }

        YAMLKeyValue yamlKeyValue = YAMLUtil.getQualifiedKeyInFile((YAMLFile) psiFile, rootName);
        if(yamlKeyValue == null) {
            return;
        }

        YAMLCompoundValue yaml = PsiTreeUtil.findChildOfType(yamlKeyValue, YAMLMapping.class);
        if(yaml != null) {
            YamlHelper.attachDuplicateKeyInspection(yaml, holder);
        }
    }
}
