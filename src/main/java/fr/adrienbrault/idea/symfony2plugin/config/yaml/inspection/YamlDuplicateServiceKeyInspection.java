package fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;
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
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof YAMLKeyValue yamlKeyValue) {
                    visitRoot(yamlKeyValue, "services", holder);
                }

                super.visitElement(element);
            }
        };
    }

    protected void visitRoot(@NotNull YAMLKeyValue yamlKeyValue, String rootName, @NotNull ProblemsHolder holder) {
        if (yamlKeyValue.getParent() instanceof YAMLMapping yamlMapping) {
            String keyText1 = yamlKeyValue.getKeyText();
            if (!keyText1.startsWith("_")) {
                if (yamlMapping.getParent() instanceof YAMLKeyValue yamlKeyValue1) {
                    if (rootName.equals(yamlKeyValue1.getKeyText())) {
                        int found = 0;
                        for (YAMLKeyValue keyValue : yamlMapping.getKeyValues()) {
                            String keyText = keyValue.getKeyText();

                            if (keyText1.equals(keyText)) {
                                found++;
                            }

                            if (found == 2) {
                                final PsiElement keyElement = yamlKeyValue.getKey();
                                assert keyElement != null;
                                holder.registerProblem(keyElement, "Symfony: Duplicate key", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);

                                break;
                            }
                        }
                    }
                }
            }
        }
    }
}
