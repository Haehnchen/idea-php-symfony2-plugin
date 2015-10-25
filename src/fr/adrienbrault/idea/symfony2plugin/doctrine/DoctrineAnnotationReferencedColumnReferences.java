package fr.adrienbrault.idea.symfony2plugin.doctrine;


import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.parser.PhpDocElementTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.dict.PhpDocCommentAnnotation;
import de.espend.idea.php.annotation.dict.PhpDocTagAnnotation;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelField;
import fr.adrienbrault.idea.symfony2plugin.doctrine.dict.DoctrineModelFieldLookupElement;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DoctrineAnnotationReferencedColumnReferences implements PhpAnnotationReferenceProvider {

    @Nullable
    @Override
    public PsiReference[] getPropertyReferences(AnnotationPropertyParameter annotationPropertyParameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {

        PsiElement element = annotationPropertyParameter.getElement();
        if(!Symfony2ProjectComponent.isEnabled(annotationPropertyParameter.getProject()) ||
            !(element instanceof StringLiteralExpression) ||
            !PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(), "\\Doctrine\\ORM\\Mapping\\JoinColumn")
            )
        {
            return new PsiReference[0];
        }

        // @Foo(targetEntity="Foo\Class")
        if(annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.PROPERTY_VALUE && "referencedColumnName".equals(annotationPropertyParameter.getPropertyName())) {

            PhpDocComment phpDocComment = PsiTreeUtil.getParentOfType(element, PhpDocComment.class);
            if(phpDocComment != null) {
                PhpDocCommentAnnotation phpDocCommentAnnotationContainer = AnnotationUtil.getPhpDocCommentAnnotationContainer(phpDocComment);

                if(phpDocCommentAnnotationContainer != null) {

                    PhpDocTagAnnotation phpDocTagAnnotation = phpDocCommentAnnotationContainer.getFirstPhpDocBlock(
                        "\\Doctrine\\ORM\\Mapping\\ManyToOne",
                        "\\Doctrine\\ORM\\Mapping\\ManyToMany",
                        "\\Doctrine\\ORM\\Mapping\\OneToOne",
                        "\\Doctrine\\ORM\\Mapping\\OneToMany"
                    );

                    if(phpDocTagAnnotation != null) {

                        PhpPsiElement phpDocAttrList = phpDocTagAnnotation.getPhpDocTag().getFirstPsiChild();

                        // @TODO: remove nested on Annotation plugin update
                        if(phpDocAttrList != null) {
                            if(phpDocAttrList.getNode().getElementType() == PhpDocElementTypes.phpDocAttributeList) {
                                PhpPsiElement phpPsiElement = phpDocAttrList.getFirstPsiChild();
                                if(phpPsiElement instanceof StringLiteralExpression) {
                                    PhpClass phpClass = de.espend.idea.php.annotation.util.PhpElementsUtil.getClassInsideAnnotation(((StringLiteralExpression) phpPsiElement));
                                    if(phpClass != null) {
                                        Collection<DoctrineModelField> lists = EntityHelper.getModelFields(phpClass);
                                        if(lists.size() > 0) {
                                            return new PsiReference[] {
                                                new EntityReference((StringLiteralExpression) element, lists)
                                            };
                                        }
                                    }
                                }
                            }
                        }


                    }

                }

            }

        }

        return new PsiReference[0];
    }

    public class EntityReference extends PsiPolyVariantReferenceBase<PsiElement> {

        final private Collection<DoctrineModelField> doctrineModelField;
        final private String content;

        public EntityReference(StringLiteralExpression psiElement, Collection<DoctrineModelField> doctrineModelField) {
            super(psiElement);
            this.doctrineModelField = doctrineModelField;
            this.content = psiElement.getContents();
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean b) {

            Collection<PsiElement> targets = new ArrayList<PsiElement>();
            for(DoctrineModelField field: doctrineModelField) {
                if(this.content.equals(field.getColumn())) {
                    targets.addAll(field.getTargets());
                }
            }

            return PsiElementResolveResult.createResults(targets);
        }

        @NotNull
        @Override
        public Object[] getVariants() {

            List<LookupElement> lookupElements = new ArrayList<LookupElement>();
            for(DoctrineModelField field: doctrineModelField) {
                String column = field.getColumn();
                if(column != null) {
                    lookupElements.add(new DoctrineModelFieldLookupElement(field).withLookupName(column));
                }
            }

            return lookupElements.toArray();

        }

    }

}
