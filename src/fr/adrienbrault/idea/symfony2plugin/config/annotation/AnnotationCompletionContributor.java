package fr.adrienbrault.idea.symfony2plugin.config.annotation;

import com.intellij.codeInsight.completion.*;
import fr.adrienbrault.idea.symfony2plugin.config.doctrine.DoctrineStaticTypeLookupBuilder;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationCompletionContributor extends CompletionContributor {
    public AnnotationCompletionContributor() {

        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\OneToOne"), new AnnotationCompletionProvider(DoctrineStaticTypeLookupBuilder.getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.oneToOne)));
        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\OneToMany"), new AnnotationCompletionProvider(DoctrineStaticTypeLookupBuilder.getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.oneToMany)));
        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\ManyToOne"), new AnnotationCompletionProvider(DoctrineStaticTypeLookupBuilder.getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.manyToOne)));
        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\ManyToMany"), new AnnotationCompletionProvider(DoctrineStaticTypeLookupBuilder.getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.manyToMany)));

        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\Column"), new AnnotationCompletionProvider(DoctrineStaticTypeLookupBuilder.getPropertyMappings()));

        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getOrmProperties(), new AnnotationCompletionProvider(DoctrineStaticTypeLookupBuilder.getOrmFieldAnnotations()));

    }
}

