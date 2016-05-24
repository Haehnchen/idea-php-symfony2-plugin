package fr.adrienbrault.idea.symfony2plugin.intentions.yaml;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlUnquotedColon extends LocalInspectionTool {

    public static String MESSAGE = "Using a colon in the unquoted mapping value is deprecated since Symfony 2.8 and will throw a ParseException in 3.0";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {
        private final ProblemsHolder holder;

        MyPsiElementVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(element.getNode().getElementType() != YAMLTokenTypes.TEXT) {
                super.visitElement(element);
                return;
            }

            PsiElement plainScalar = element.getParent();
            if(!(plainScalar instanceof YAMLPlainTextImpl)) {
                super.visitElement(element);
                return;
            }

            PsiElement yamlKeyValue = plainScalar.getParent();
            if(!(yamlKeyValue instanceof YAMLKeyValue)) {
                super.visitElement(element);
                return;
            }

            String text = ((YAMLPlainTextImpl) plainScalar).getTextValue();
            if(!text.contains(": ")) {
                super.visitElement(element);
                return;
            }

            holder.registerProblem(
                element,
                YamlUnquotedColon.MESSAGE,
                ProblemHighlightType.WEAK_WARNING
            );

            super.visitElement(element);
        }
    }
}
