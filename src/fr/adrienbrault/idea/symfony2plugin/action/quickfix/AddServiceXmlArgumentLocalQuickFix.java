package fr.adrienbrault.idea.symfony2plugin.action.quickfix;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.action.ui.ServiceArgumentSelectionDialog;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        return "Missing Argument";
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Missing Argument";
    }

    @Override
    public void applyFix(final @NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {

        final PsiElement parent = problemDescriptor.getPsiElement().getParent();
        if(!(parent instanceof XmlTag)) {
            return;
        }

        Map<String, Set<String>> resolved = new LinkedHashMap<String, Set<String>>();
        Map<String, ContainerService> services = ContainerCollectionResolver.getServices(project);

        for (String arg : args) {
            resolved.put(arg, ServiceActionUtil.getPossibleServices(project, arg, services));
        }

        ServiceArgumentSelectionDialog.createDialog(project, resolved, new ServiceArgumentSelectionDialog.Callback() {
            @Override
            public void onOk(List<String> items) {
                for (String item : items) {
                    XmlTag tag = XmlElementFactory.getInstance(project).createTagFromText(String.format("<argument type=\"service\" id=\"%s\"/>", item), parent.getLanguage());
                    ((XmlTag) parent).addSubTag(tag, false);
                }
            }
        });

    }

}
