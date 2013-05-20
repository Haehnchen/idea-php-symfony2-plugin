package fr.adrienbrault.idea.symfony2plugin.config.annotation;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.config.doctrine.DoctrineStaticTypeLookupBuilder;
import fr.adrienbrault.idea.symfony2plugin.util.annotation.AnnotationIndex;
import fr.adrienbrault.idea.symfony2plugin.util.annotation.AnnotationLookupElement;
import fr.adrienbrault.idea.symfony2plugin.util.annotation.AnnotationValue;
import fr.adrienbrault.idea.symfony2plugin.util.completion.annotations.AnnotationTagInsertHandler;
import fr.adrienbrault.idea.symfony2plugin.util.completion.annotations.AnnotationMethodInsertHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class AnnotationCompletionContributor extends CompletionContributor {
    public AnnotationCompletionContributor() {

        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\OneToOne"), new AnnotationCompletionProvider(new DoctrineStaticTypeLookupBuilder(DoctrineStaticTypeLookupBuilder.InsertHandler.Annotations).getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.oneToOne)));
        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\OneToMany"), new AnnotationCompletionProvider(new DoctrineStaticTypeLookupBuilder(DoctrineStaticTypeLookupBuilder.InsertHandler.Annotations).getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.oneToMany)));
        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\ManyToOne"), new AnnotationCompletionProvider( new DoctrineStaticTypeLookupBuilder(DoctrineStaticTypeLookupBuilder.InsertHandler.Annotations).getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.manyToOne)));
        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\ManyToMany"), new AnnotationCompletionProvider(new DoctrineStaticTypeLookupBuilder(DoctrineStaticTypeLookupBuilder.InsertHandler.Annotations).getAssociationMapping(DoctrineStaticTypeLookupBuilder.Association.manyToMany)));

        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier("@ORM\\Column"), new AnnotationCompletionProvider(new DoctrineStaticTypeLookupBuilder(DoctrineStaticTypeLookupBuilder.InsertHandler.Annotations).getPropertyMappings()));

        extend(CompletionType.BASIC, AnnotationElementPatternHelper.getOrmProperties(), new AnnotationCompletionProvider(new DoctrineStaticTypeLookupBuilder(DoctrineStaticTypeLookupBuilder.InsertHandler.Annotations).getOrmFieldAnnotations()));


        // adding @Annotation(<values>)
        Set<String> controllerTagNames = AnnotationIndex.getControllerAnnotations().keySet();
        for (String controllerTagName : controllerTagNames) {
            extend(CompletionType.BASIC, AnnotationElementPatternHelper.getTextIdentifier(controllerTagName), new PhpControllerValueAnnotations(controllerTagName));
        }

        // adding @Annotation()
        extend( CompletionType.BASIC, AnnotationElementPatternHelper.getControllerActionMethodPattern(), new PhpControllerAnnotations());

    }


    private static class PhpControllerValueAnnotations extends CompletionProvider<CompletionParameters>{

        private String docTagName;
        public PhpControllerValueAnnotations(String docTagName) {
            this.docTagName = docTagName;
        }

        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {
            if(!AnnotationIndex.getControllerAnnotations().containsKey(this.docTagName)) {
                return;
            }

            ArrayList<AnnotationValue> annotationValues = AnnotationIndex.getControllerAnnotations().get(this.docTagName).getValues();
            if(null == annotationValues) {
                return;
            }

            for (AnnotationValue annotationValue : annotationValues) {
                resultSet.addElement(new AnnotationLookupElement(annotationValue.getName(), annotationValue, AnnotationMethodInsertHandler.getInstance()));
            }

        }

    }

    private static class PhpControllerAnnotations extends CompletionProvider<CompletionParameters>{

        public void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet resultSet) {

            PsiElement position = parameters.getPosition().getOriginalElement();
            PsiElement parent = position.getParent();

            if (((parent instanceof PhpDocComment)) || ((parent instanceof PhpDocTag))) {

                boolean at = parent instanceof PhpDocTag;
                Set<String> functags = AnnotationIndex.getControllerAnnotations().keySet();

                Method method = PsiTreeUtil.getNextSiblingOfType(parent, Method.class);
                if(null == method || !method.getName().endsWith("Action")) {
                    return;
                }


                for (String s : functags) {
                    resultSet.addElement(createDocTagLookup(at, s));
                }
            }

        }

        private LookupElementBuilder createDocTagLookup(boolean at, String s) {
            return LookupElementBuilder.create(at ? s.substring(1) : s).withBoldness(true).withIcon(Symfony2Icons.SYMFONY).withInsertHandler(AnnotationTagInsertHandler.getInstance());
        }

    }


}

