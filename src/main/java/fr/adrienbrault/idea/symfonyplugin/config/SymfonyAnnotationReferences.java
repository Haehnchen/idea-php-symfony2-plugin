package fr.adrienbrault.idea.symfonyplugin.config;

import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.dic.ServiceReference;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyAnnotationReferences implements PhpAnnotationReferenceProvider {
    @Nullable
    @Override
    public PsiReference[] getPropertyReferences(AnnotationPropertyParameter annotationPropertyParameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {

        if(!Symfony2ProjectComponent.isEnabled(annotationPropertyParameter.getProject()) || !(annotationPropertyParameter.getElement() instanceof StringLiteralExpression)) {
            return new PsiReference[0];
        }

        if("service".equals(annotationPropertyParameter.getPropertyName()) && PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(), "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Route")) {
            return new PsiReference[]{ new ServiceReference((StringLiteralExpression) annotationPropertyParameter.getElement(), false) };
        }

        // JMSDiExtraBundle; @TODO: provide config
        if((annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.DEFAULT || "id".equals(annotationPropertyParameter.getPropertyName())) && PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(), "\\JMS\\DiExtraBundle\\Annotation\\Service")) {
            return new PsiReference[]{ new ServiceReference((StringLiteralExpression) annotationPropertyParameter.getElement(), false) };
        }

        if((annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.DEFAULT) && PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(), "\\JMS\\DiExtraBundle\\Annotation\\Inject")) {
            return new PsiReference[]{ new ServiceReference((StringLiteralExpression) annotationPropertyParameter.getElement(), false) };
        }

        if("class".equals(annotationPropertyParameter.getPropertyName()) && PhpElementsUtil.isEqualClassName(annotationPropertyParameter.getPhpClass(), "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\ParamConverter")) {
            return new PsiReference[]{ new PhpClassReference((StringLiteralExpression) annotationPropertyParameter.getElement(), true).setUseClasses(true).setUseInterfaces(true) };
        }

        return new PsiReference[0];
    }

}
