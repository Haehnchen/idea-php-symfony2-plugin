package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterLookupPercentElement;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlCompletionContributor;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerParameter;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.DoctrineMetadataPattern;
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleFileCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.completion.PhpClassAndParameterCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

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
        extend(CompletionType.BASIC, XmlHelper.getArgumentValuePattern(), new ArgumentParameterCompletionProvider());

        extend(
            CompletionType.BASIC,
            XmlPatterns.psiElement().withParent(XmlHelper.getImportResourcePattern()),
            new SymfonyBundleFileCompletionProvider("Resources/config", "Controller")
        );

        extend(
            CompletionType.BASIC,
            XmlPatterns.psiElement().withParent(XmlHelper.getImportResourcePattern()),
            new YamlCompletionContributor.DirectoryScopeCompletionProvider()
        );
    }

    private static class FormAliasParametersCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, final @NotNull CompletionResultSet completionResultSet) {
            PsiElement psiElement = completionParameters.getOriginalPosition();
            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

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

    /**
     *
     * <argument>%</argument>
     * <argument></argument>
     */
    private static class ArgumentParameterCompletionProvider extends CompletionProvider<CompletionParameters> {

        @Override
        protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

            PsiElement psiElement = completionParameters.getOriginalPosition();
            if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                return;
            }

            Project project = psiElement.getProject();
            for( Map.Entry<String, ContainerParameter> entry: ContainerCollectionResolver.getParameters(project).entrySet()) {
                completionResultSet.addElement(
                    new ParameterLookupPercentElement(entry.getValue())
                );
            }
        }

    }

}

