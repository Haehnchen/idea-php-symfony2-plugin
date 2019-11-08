package fr.adrienbrault.idea.symfonyplugin.intentions.yaml;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.util.SymfonyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import static fr.adrienbrault.idea.symfonyplugin.util.VersionUtil.productVersionGreaterThanOrEqual;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlUnquotedColon extends LocalInspectionTool {

    public static String MESSAGE = "Using a colon in the unquoted mapping value is deprecated since Symfony 2.8 and will throw a ParseException in 3.0";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject()) || !SymfonyUtil.isVersionGreaterThenEquals(holder.getProject(), "2.8")) {
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
            // every array element implements this interface
            // check for inside "foo: <foo: foo>"
            if(!isIllegalColonExpression(element)) {
                super.visitElement(element);
                return;
            }

            // element need to be inside key value
            PsiElement yamlKeyValue = element.getParent();
            if(!(yamlKeyValue instanceof YAMLKeyValue)) {
                super.visitElement(element);
                return;
            }

            // invalid inline item "foo: foo: foo", also check text length
            String text = yamlKeyValue.getText();
            if(!text.contains(": ") || text.contains("\n") || text.length() > 200) {
                super.visitElement(element);
                return;
            }

            // attach notification "foo: <foo: foo>"
            holder.registerProblem(
                element,
                YamlUnquotedColon.MESSAGE,
                ProblemHighlightType.WEAK_WARNING
            );

            super.visitElement(element);
        }

        private boolean isIllegalColonExpression(PsiElement element) {

            if (productVersionGreaterThanOrEqual(2018, 3)) {
                return (element instanceof YAMLCompoundValue) && element.getNode().getElementType() == YAMLElementTypes.MAPPING;
            }

            return (element instanceof YAMLCompoundValue) && element.getNode().getElementType() == YAMLElementTypes.COMPOUND_VALUE;
        }
    }
}
