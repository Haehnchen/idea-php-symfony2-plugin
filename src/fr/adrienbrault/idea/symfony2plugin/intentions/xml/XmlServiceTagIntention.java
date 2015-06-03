package fr.adrienbrault.idea.symfony2plugin.intentions.xml;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.intentions.php.XmlServiceArgumentIntention;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceTag;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceTagIntention extends PsiElementBaseIntentionAction {
    
    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {

        if(psiElement.getContainingFile().getFileType() != XmlFileType.INSTANCE || !Symfony2ProjectComponent.isEnabled(psiElement.getProject())) {
            return false;
        }

        XmlTag serviceTagValid = XmlServiceArgumentIntention.getServiceTagValid(psiElement);
        if(serviceTagValid == null) {
            return false;
        }

        return true;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {

        XmlTag xmlTag = XmlServiceArgumentIntention.getServiceTagValid(psiElement);
        if(xmlTag == null) {
            return;
        }

        PhpClass phpClassFromXmlTag = ServiceActionUtil.getPhpClassFromXmlTag(xmlTag, new ContainerCollectionResolver.LazyServiceCollector(project));
        if(phpClassFromXmlTag == null) {
            return;
        }

        Set<String> phpServiceTags = ServiceUtil.getPhpClassServiceTags(phpClassFromXmlTag);
        if(phpServiceTags.size() == 0) {
            HintManager.getInstance().showErrorHint(editor, "Ops, no possible Tag found");
            return;
        }

        for (XmlTag tag : xmlTag.getSubTags()) {

            if(!"tag".equals(tag.getName())) {
                continue;
            }

            XmlAttribute name = tag.getAttribute("name");
            if(name == null) {
                continue;
            }

            String value = name.getValue();
            if(phpServiceTags.contains(value)) {
                phpServiceTags.remove(value);
            }

        }

        if(phpServiceTags.size() == 0) {
            HintManager.getInstance().showErrorHint(editor, "Ops, no need for additional tag");
            return;
        }

        for (String phpServiceTag : phpServiceTags) {
            ServiceTag serviceTag = new ServiceTag(phpClassFromXmlTag, phpServiceTag);
            ServiceUtil.decorateServiceTag(serviceTag);
            xmlTag.addSubTag(XmlElementFactory.getInstance(xmlTag.getProject()).createTagFromText(serviceTag.toXmlString(), xmlTag.getLanguage()), false);
        }

    }

    @NotNull
    @Override
    public String getFamilyName() {
        return "Foo";
    }

    @NotNull
    @Override
    public String getText() {
        return "Symfony: Add Tags";
    }

}
