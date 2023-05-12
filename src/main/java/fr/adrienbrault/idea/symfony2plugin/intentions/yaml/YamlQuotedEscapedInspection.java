package fr.adrienbrault.idea.symfony2plugin.intentions.yaml;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlQuotedEscapedInspection extends LocalInspectionTool {

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();

        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element.getNode().getElementType() == YAMLTokenTypes.SCALAR_DSTRING && SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
                    // "Foo\Foo" -> "Foo\\Foo"
                    String text = StringUtils.strip(element.getText(), "\"");

                    // dont check to long strings
                    // ascii chars that need to be escape; some @see Symfony\Component\Yaml\Unescaper
                    if (text.length() < 255 && text.matches(".*[^\\\\]\\\\[^\\\\0abtnvfre \"/N_LPxuU].*")) {
                        holder.registerProblem(element, "Not escaping a backslash in a double-quoted string is deprecated", ProblemHighlightType.WEAK_WARNING);
                    }
                } else if (element.getNode().getElementType() == YAMLTokenTypes.TEXT && SymfonyUtil.isVersionGreaterThenEquals(project, "2.8")) {
                    // @foo -> "@foo"
                    String text;
                    if (parentIsErrorAndHasPreviousElement(element)) {
                        text = element.getParent().getPrevSibling().getText();
                    } else {
                        text = element.getText();
                    }

                    if (text.length() > 1 || (parentIsErrorAndHasPreviousElement(element) && text.length() >= 1)) {
                        String startChar = text.substring(0, 1);
                        if (startChar.equals("@") || startChar.equals("`") || startChar.equals("|") || startChar.equals(">")) {
                            holder.registerProblem(element, String.format("Deprecated usage of '%s' at the beginning of unquoted string", startChar), ProblemHighlightType.WEAK_WARNING);
                        } else if (startChar.equals("%")) {
                            // deprecated in => "3.1"; but as most user will need to migrate in 2.8 let them know it already
                            holder.registerProblem(element, "Not quoting a scalar starting with the '%' indicator character is deprecated since Symfony 3.1", ProblemHighlightType.WEAK_WARNING);
                        }
                    }
                }
                super.visitElement(element);
            }
        };
    }

    private boolean parentIsErrorAndHasPreviousElement(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();

        return parent instanceof PsiErrorElement && parent.getPrevSibling() != null;
    }
}
