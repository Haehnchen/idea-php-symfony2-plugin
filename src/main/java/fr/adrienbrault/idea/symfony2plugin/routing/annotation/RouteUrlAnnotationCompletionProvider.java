package fr.adrienbrault.idea.symfony2plugin.routing.annotation;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import de.espend.idea.php.annotation.extension.PhpAnnotationCompletionProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationCompletionProviderParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
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
        if (!Symfony2ProjectComponent.isEnabled(annotationPropertyParameter.getProject()) || !PhpElementsUtil.isInstanceOf(annotationPropertyParameter.getPhpClass(), "\\Symfony\\Component\\Routing\\Annotation\\Route")) {
            return;
        }

        if (annotationPropertyParameter.getType() == AnnotationPropertyParameter.Type.DEFAULT || "path".equals(annotationPropertyParameter.getPropertyName())) {
            for (Route route : RouteHelper.getAllRoutes(annotationPropertyParameter.getProject()).values()) {
                String path = route.getPath();
                if (path != null && !route.getName().startsWith("_")) {
                    LookupElementBuilder element = LookupElementBuilder.create(path)
                        .withTypeText(route.getName())
                        .withIcon(Symfony2Icons.ROUTE_WEAK);

                    annotationCompletionProviderParameter.getResult().addElement(element);
                }
            }
        }
    }
}
