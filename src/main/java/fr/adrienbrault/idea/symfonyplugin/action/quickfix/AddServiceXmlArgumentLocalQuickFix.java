package fr.adrienbrault.idea.symfonyplugin.action.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfonyplugin.action.ServiceActionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AddServiceXmlArgumentLocalQuickFix implements LocalQuickFix {

    private final List<String> args;

    public AddServiceXmlArgumentLocalQuickFix(List<String> args) {
        this.args = args;
    }

    @NotNull
    @Override
    public String getName() {
        return "Symfony: missing argument";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @Override
    public void applyFix(final @NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {

        final PsiElement parent = problemDescriptor.getPsiElement().getParent();
        if(!(parent instanceof XmlTag)) {
            return;
        }

        ServiceActionUtil.fixServiceArgument(args, (XmlTag) parent);
    }

}
