package fr.adrienbrault.idea.symfonyplugin.intentions.xml;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfonyplugin.intentions.php.XmlServiceArgumentIntention;
import fr.adrienbrault.idea.symfonyplugin.intentions.ui.ServiceSuggestDialog;
import fr.adrienbrault.idea.symfonyplugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceSuggestIntention extends PsiElementBaseIntentionAction {

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement.getProject())) {
            return false;
        }

        final XmlTag argumentTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if(argumentTag == null || !"argument".equals(argumentTag.getName())) {
            return false;
        }

        return XmlServiceArgumentIntention.getServiceTagValid(psiElement) != null;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        XmlTag xmlTag = XmlServiceArgumentIntention.getServiceTagValid(psiElement);
        if (xmlTag == null) {
            return;
        }

        String aClass = XmlHelper.getClassFromServiceDefinition(xmlTag);
        if(aClass == null || StringUtils.isBlank(aClass)) {
            return;
        }

        final XmlTag argumentTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if(argumentTag == null) {
            return;
        }

        int argumentIndex = XmlHelper.getArgumentIndex(argumentTag);

        Set<String> suggestions = ServiceUtil.getServiceSuggestionsForServiceConstructorIndex(project, aClass, argumentIndex);
        if(suggestions.size() == 0) {
            HintManager.getInstance().showErrorHint(editor, "No suggestion found");
            return;
        }

        ServiceSuggestDialog.create(editor, suggestions, new MyInsertCallback(argumentTag));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String getText() {
        return "Symfony: Suggest Service";
    }

    public static class MyInsertCallback implements ServiceSuggestDialog.Callback {
        private final XmlTag argumentTag;

        public MyInsertCallback(XmlTag argumentTag) {
            this.argumentTag = argumentTag;
        }

        @Override
        public void insert(@NotNull String selected) {

            // set type="service" for lazy devs
            if(ContainerUtil.find(argumentTag.getAttributes(), xmlAttribute -> "type".equals(xmlAttribute.getName())) == null) {
                argumentTag.setAttribute("type", "service");
            }

            // append type="SERVICE"
            argumentTag.setAttribute("id", selected);
        }
    }
}
