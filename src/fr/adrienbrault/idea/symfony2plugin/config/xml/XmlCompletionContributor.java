package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.completion.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassAndParameterCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
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

        extend(CompletionType.BASIC, XmlHelper.getTagAttributePattern("tag", "alias").inside(XmlHelper.getInsideTagPattern("services")), new FormAliasParametersCompletionProvider());
    }

    private static class FormAliasParametersCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, final @NotNull CompletionResultSet completionResultSet) {
            PsiElement psiElement = completionParameters.getOriginalPosition();

            XmlTag xmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
            if(xmlTag == null) {
                return;
            }

            XmlTag xmlTagService = PsiTreeUtil.getParentOfType(xmlTag, XmlTag.class);
            if(xmlTagService != null) {
                XmlAttribute xmlAttribute = xmlTagService.getAttribute("class");
                if(xmlAttribute != null) {
                    String value = xmlAttribute.getValue();
                    if(value != null && StringUtils.isNotBlank(value)) {
                        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), value);
                        if(phpClass != null) {
                            FormUtil.attachFormAliasesCompletions(phpClass, completionResultSet);
                        }
                    }
                }
            }

        }
    }

}

