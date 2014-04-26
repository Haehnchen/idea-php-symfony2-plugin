package fr.adrienbrault.idea.symfony2plugin.templating.annotation;

import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateReference;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

public class TemplateAnnotationReferences implements PhpAnnotationReferenceProvider {
    @Nullable
    @Override
    public PsiReference[] getPropertyReferences(AnnotationPropertyParameter annotationPropertyParameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {

        if(!(annotationPropertyParameter.getElement() instanceof StringLiteralExpression) || !PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(), TwigHelper.TEMPLATE_ANNOTATION_CLASS)) {
            return new PsiReference[0];
        }

        // @Foo("template.html.twig")
        if(annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.DEFAULT) {
            return new PsiReference[]{ new TemplateReference((StringLiteralExpression) annotationPropertyParameter.getElement()) };
        }

        // @Foo(template="template.html.twig")
        if(annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.PROPERTY_VALUE || "template".equals(annotationPropertyParameter.getPropertyName())) {
            return new PsiReference[]{ new TemplateReference((StringLiteralExpression) annotationPropertyParameter.getElement()) };
        }

        return new PsiReference[0];
    }

}
