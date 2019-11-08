package fr.adrienbrault.idea.symfonyplugin.doctrine.metadata;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfonyplugin.util.completion.PhpClassCompletionProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineXmlCompletionContributor extends CompletionContributor {

    public DoctrineXmlCompletionContributor() {

        // <entity name="Class\Name"/>
        // <document name="Class\Name"/>
        // <embeddable name="Class\Name"/>
        extend(CompletionType.BASIC, XmlPatterns.psiElement().withParent(PlatformPatterns.or(
            DoctrineMetadataPattern.getXmlModelClass(),
            DoctrineMetadataPattern.getXmlTargetEntityClass(),
            DoctrineMetadataPattern.getXmlTargetDocumentClass(),
            DoctrineMetadataPattern.getEmbeddableNameClassPattern()
        )), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {

                PsiElement psiElement = parameters.getOriginalPosition();
                if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                PhpClassCompletionProvider.addClassCompletion(parameters, resultSet, psiElement, false);
            }
        });

        // <entity repository-class="Class\Name"/>
        // <document repository-class="Class\Name"/>
        extend(CompletionType.BASIC, XmlPatterns.psiElement().withParent(PlatformPatterns.or(DoctrineMetadataPattern.getXmlRepositoryClass())),
            new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {

                PsiElement psiElement = parameters.getOriginalPosition();
                if(psiElement == null || !Symfony2ProjectComponent.isEnabled(psiElement)) {
                    return;
                }

                // @TODO: filter on doctrine manager
                resultSet.addAllElements(
                    DoctrineMetadataUtil.getObjectRepositoryLookupElements(psiElement.getProject())
                );
            }
        });
   }
}
