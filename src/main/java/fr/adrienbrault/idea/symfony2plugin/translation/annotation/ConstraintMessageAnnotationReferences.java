package fr.adrienbrault.idea.symfony2plugin.translation.annotation;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationCompletionProvider;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationCompletionProviderParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationReference;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.Nullable;

/**
 * Support "@FooConstraint(message="sss")"
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConstraintMessageAnnotationReferences implements PhpAnnotationReferenceProvider, PhpAnnotationCompletionProvider {
    @Nullable
    @Override
    public PsiReference[] getPropertyReferences(AnnotationPropertyParameter parameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {
        if (!Symfony2ProjectComponent.isEnabled(parameter.getProject()) || !PhpElementsUtil.isInstanceOf(parameter.getPhpClass(), "\\Symfony\\Component\\Validator\\Constraint")) {
            return new PsiReference[0];
        }

        String propertyName = parameter.getPropertyName();
        if (propertyName == null || (!propertyName.startsWith("message") && !propertyName.endsWith("Message"))) {
            return new PsiReference[0];
        }

        PsiElement element = parameter.getElement();
        if (!(element instanceof StringLiteralExpression)) {
            return new PsiReference[0];
        }

        return new PsiReference[] { new TranslationReference((StringLiteralExpression) element, "validators")};
    }

    @Override
    public void getPropertyValueCompletions(AnnotationPropertyParameter parameter, AnnotationCompletionProviderParameter annotationCompletionProviderParameter) {
        if (!Symfony2ProjectComponent.isEnabled(parameter.getProject()) || !PhpElementsUtil.isInstanceOf(parameter.getPhpClass(), "\\Symfony\\Component\\Validator\\Constraint")) {
            return;
        }

        String propertyName = parameter.getPropertyName();
        if (propertyName == null || (!propertyName.startsWith("message") && !propertyName.endsWith("Message"))) {
            return;
        }

        annotationCompletionProviderParameter
            .getResult()
            .addAllElements(TranslationUtil.getTranslationLookupElementsOnDomain(parameter.getProject(), "validators"));
    }
}
