package fr.adrienbrault.idea.symfony2plugin.routing.annotation;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.lexer.PhpDocTokenTypes;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttributesList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationCompletionProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationCompletionProviderParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;

/**
 * Guess route name on method
 *
 * - "@Route(name="follow_method")
 * - "#[Route(name: "follow_method")]
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class RouteNameAnnotationCompletionProvider implements PhpAnnotationCompletionProvider {
    @Override
    public void getPropertyValueCompletions(AnnotationPropertyParameter annotationPropertyParameter, AnnotationCompletionProviderParameter annotationCompletionProviderParameter) {
        if (!Symfony2ProjectComponent.isEnabled(annotationPropertyParameter.getProject()) || !PhpElementsUtil.isInstanceOf(annotationPropertyParameter.getPhpClass(), "\\Symfony\\Component\\Routing\\Annotation\\Route")) {
            return;
        }

        if (annotationPropertyParameter.getType() != AnnotationPropertyParameter.Type.PROPERTY_VALUE || !"name".equals(annotationPropertyParameter.getPropertyName())) {
            return;
        }

        String routeByMethod = null;

        PsiElement element = annotationPropertyParameter.getElement();
        if (element.getNode().getElementType() == PhpDocTokenTypes.DOC_STRING) {
            // @Route
            PhpDocTag phpDocTag = PsiTreeUtil.getParentOfType(element, PhpDocTag.class);
            if (phpDocTag != null) {
                routeByMethod = AnnotationBackportUtil.getRouteByMethod(phpDocTag);
            }
        } else if (element.getParent() instanceof StringLiteralExpression) {
            // #[Route]
            PhpAttributesList phpAttributesList = PsiTreeUtil.getParentOfType(element, PhpAttributesList.class);
            if (phpAttributesList != null) {
                PsiElement parent = phpAttributesList.getParent();
                if (parent instanceof Method) {
                    routeByMethod = AnnotationBackportUtil.getRouteByMethod((Method) parent);
                }
            }
        }

        if (routeByMethod != null) {
            LookupElementBuilder lookupElementBuilder = LookupElementBuilder
                .create(routeByMethod)
                .withIcon(Symfony2Icons.ROUTE_WEAK);

            annotationCompletionProviderParameter.getResult().addElement(lookupElementBuilder);
        }
    }
}
