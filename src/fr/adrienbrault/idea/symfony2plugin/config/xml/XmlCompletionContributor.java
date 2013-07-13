package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.XmlPatterns;
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
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("class").inside(XmlHelper.getInsideTagPattern("services")), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-service").inside(XmlHelper.getInsideTagPattern("services")), new ServiceCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("factory-class").inside(XmlHelper.getInsideTagPattern("services")), new PhpClassAndParameterCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagPattern("parent").inside(XmlHelper.getInsideTagPattern("services")), new ServiceCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getParameterWithClassEndingPattern().inside(XmlHelper.getInsideTagPattern("parameters")), new PhpClassCompletionProvider());

        extend(CompletionType.BASIC, XmlHelper.getTagAttributePattern("tag", "name").inside(XmlHelper.getInsideTagPattern("services")), new TagNameCompletionProvider());
        extend(CompletionType.BASIC, XmlHelper.getTagAttributePattern("tag", "event").inside(XmlHelper.getInsideTagPattern("services")), new EventCompletionProvider());

        extend(CompletionType.BASIC, XmlHelper.getTagAttributePattern("call", "method").inside(XmlHelper.getInsideTagPattern("services")), new ServiceCallsMethodCompletion());

    }

    private class ServiceCallsMethodCompletion extends CompletionProvider<CompletionParameters> {

        /**
         * provides method completion
         *
         * <service id="fos_user.user_listener" class="FOS\UserBundle\Doctrine\CouchDB\UserListener" public="false">
         *   <call method="setMailer">
         *     <argument type="service" id="my_mailer" />
         *   </call>
         * </service>
         */
        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            if(!Symfony2ProjectComponent.isEnabled(completionParameters.getPosition())) {
                return;
            }

            PsiElement psiElement = completionParameters.getPosition();

            // check for valid xml file and services container
            if(!XmlPatterns.psiElement().inside(XmlHelper.getInsideTagPattern("services")).inFile(XmlHelper.getXmlFilePattern()).accepts(psiElement)) {
                return;
            }

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

