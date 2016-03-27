package fr.adrienbrault.idea.symfony2plugin.intentions.yaml;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlQuotedEscapedInspection extends LocalInspectionTool {

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(element.getNode().getElementType() == YAMLTokenTypes.SCALAR_DSTRING) {
                    // "Foo\Foo" -> "Foo\\Foo"
                    String text = StringUtils.strip(element.getText(), "\"");
                    if(text.matches(".*[^\\\\]\\\\[^\\\\].*")) {
                        holder.registerProblem(element, "Not escaping a backslash in a double-quoted string is deprecated", ProblemHighlightType.LIKE_DEPRECATED);
                    }
                } else if (element.getNode().getElementType() == YAMLTokenTypes.TEXT) {
                    // @foo -> "@foo"
                    String text = element.getText();
                    if(text.length() > 1) {
                        String startChar = text.substring(0, 1);
                        if(startChar.equals("@") || startChar.equals("`") || startChar.equals("|") || startChar.equals(">")) {
                            holder.registerProblem(element, String.format("Deprecated usage of '%s' at the beginning of unquoted string", startChar), ProblemHighlightType.LIKE_DEPRECATED);
                        } else if(startChar.equals("%")) {
                            holder.registerProblem(element, "Not quoting a scalar starting with the '%' indicator character is deprecated since Symfony 3.1", ProblemHighlightType.LIKE_DEPRECATED);
                        }
                    }
                }
                super.visitElement(element);
            }
        };
    }

}
