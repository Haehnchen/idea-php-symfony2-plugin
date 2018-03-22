package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineAnnotationTargetEntityReferences implements PhpAnnotationReferenceProvider {
    @Nullable
    @Override
    public PsiReference[] getPropertyReferences(AnnotationPropertyParameter annotationPropertyParameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {

        if(!Symfony2ProjectComponent.isEnabled(annotationPropertyParameter.getProject()) || !(annotationPropertyParameter.getElement() instanceof StringLiteralExpression) || !PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(),
            "\\Doctrine\\ORM\\Mapping\\ManyToOne",
            "\\Doctrine\\ORM\\Mapping\\ManyToMany",
            "\\Doctrine\\ORM\\Mapping\\OneToOne",
            "\\Doctrine\\ORM\\Mapping\\OneToMany")
            )
        {
            return new PsiReference[0];
        }

        // @Foo(targetEntity="Foo\Class")
        if(annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.PROPERTY_VALUE && "targetEntity".equals(annotationPropertyParameter.getPropertyName())) {
            return new PsiReference[]{ new EntityReference((StringLiteralExpression) annotationPropertyParameter.getElement(), true) };
        }

        return new PsiReference[0];
    }

}
