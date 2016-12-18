package fr.adrienbrault.idea.symfony2plugin.intentions.php;

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceArgumentIntention extends PsiElementBaseIntentionAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {

        XmlTag xmlTag = getServiceTagValid(psiElement);
        if(xmlTag == null) {
            return;
        }

        final List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes(xmlTag, true, new ContainerCollectionResolver.LazyServiceCollector(project));
        if (args == null) {
            return;
        }

        ServiceActionUtil.fixServiceArgument(args, xmlTag);

    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {

        if(psiElement.getContainingFile().getFileType() != XmlFileType.INSTANCE || !Symfony2ProjectComponent.isEnabled(psiElement.getProject())) {
            return false;
        }

        XmlTag serviceTagValid = getServiceTagValid(psiElement);
        if(serviceTagValid == null) {
            return false;
        }

        if(!ServiceActionUtil.isValidXmlParameterInspectionService(serviceTagValid)) {
            return false;
        }

        return ServiceActionUtil.getXmlMissingArgumentTypes(serviceTagValid, true, new ContainerCollectionResolver.LazyServiceCollector(project)) != null;
    }

    @Nullable
    public static XmlTag getServiceTagValid(@NotNull PsiElement psiElement) {

        XmlTag xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if(xmlTag == null) {
            return null;
        }

        if("service".equals(xmlTag.getName())) {
            return xmlTag;
        }

        xmlTag = PsiTreeUtil.getParentOfType(xmlTag, XmlTag.class);
        if(xmlTag != null && "service".equals(xmlTag.getName())) {
            return xmlTag;
        }

        return null;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Symfony";
    }

    @NotNull
    @Override
    public String getText() {
        return "Symfony: Add Arguments";
    }

}
