package fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class YamlDuplicateServiceKeyInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            visitRoot(psiFile, "services", holder);
        }

        return super.buildVisitor(holder, isOnTheFly);
    }

    protected void visitRoot(PsiFile psiFile, String rootName, @NotNull ProblemsHolder holder) {
        YAMLDocument document = PsiTreeUtil.findChildOfType(psiFile, YAMLDocument.class);
        if(document != null) {
            YAMLKeyValue yamlKeyValue = YamlHelper.getYamlKeyValue(document, rootName);
            if(yamlKeyValue != null) {
                YAMLCompoundValue yaml = PsiTreeUtil.findChildOfType(yamlKeyValue, YAMLCompoundValue.class);
                if(yaml != null) {
                    YamlHelper.attachDuplicateKeyInspection(yaml, holder);
                }

            }
        }
    }

}
