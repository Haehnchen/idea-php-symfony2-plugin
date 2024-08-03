package fr.adrienbrault.idea.symfony2plugin.routing.annotation;

import de.espend.idea.php.annotation.extension.PhpAnnotationCompletionProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationCompletionProviderParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

/**
 * "@Route("/foobar1")"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteUrlAnnotationCompletionProvider implements PhpAnnotationCompletionProvider {
    @Override
    public void getPropertyValueCompletions(AnnotationPropertyParameter annotationPropertyParameter, AnnotationCompletionProviderParameter annotationCompletionProviderParameter) {
        if (!Symfony2ProjectComponent.isEnabled(annotationPropertyParameter.getProject()) || !PhpElementsUtil.isInstanceOf(annotationPropertyParameter.getPhpClass(), "\\Symfony\\Component\\Routing\\Annotation\\Route", "\\Symfony\\Component\\Routing\\Attribute\\Route")) {
            return;
        }

        if (annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.DEFAULT || "path".equals(annotationPropertyParameter.getPropertyName())) {
            annotationCompletionProviderParameter.getResult().addAllElements(RouteHelper.getRoutesPathLookupElements(annotationPropertyParameter.getProject()));
        }
    }
}
