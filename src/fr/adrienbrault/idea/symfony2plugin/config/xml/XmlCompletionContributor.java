package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.completion.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.EventCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassAndParameterCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.TagNameCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlCompletionContributor extends CompletionContributor {

    public XmlCompletionContributor() {
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("class"), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-service"), new ServiceCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-class"), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("parent"), new ServiceCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getParameterWithClassEndingPattern(), new PhpClassCompletionProvider());

        extend(CompletionType.BASIC, XmlHelper.getTagAttributePattern("tag", "name"), new TagNameCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagAttributePattern("tag", "event"), new EventCompletionProvider());

        extend(CompletionType.BASIC, XmlHelper.getTagAttributePattern("call", "method"), new ServiceCallsMethodCompletion());

    }

    private class ServiceCallsMethodCompletion extends CompletionProvider<CompletionParameters> {

        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            PsiElement psiElement = completionParameters.getPosition();

            // search for parent service definition
            XmlTag callXmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            XmlTag xmlTag = PsiTreeUtil.getParentOfType(callXmlTag, XmlTag.class);
            if(xmlTag == null || !xmlTag.getName().equals("service")) {
                return;
            }

            XmlAttribute classAttribute = xmlTag.getAttribute("class");
            if(classAttribute == null) {
                return;
            }

            PhpClass phpClass = ServiceUtil.getResolvedClass(psiElement.getProject(), classAttribute.getValue());
            if(phpClass != null) {
                PhpElementsUtil.addClassPublicMethodCompletion(completionResultSet, phpClass);
            }

        }

    }

}

