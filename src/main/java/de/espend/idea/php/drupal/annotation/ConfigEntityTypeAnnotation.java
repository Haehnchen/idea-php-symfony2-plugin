package de.espend.idea.php.drupal.annotation;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import de.espend.idea.php.annotation.extension.PhpAnnotationCompletionProvider;
import de.espend.idea.php.annotation.extension.PhpAnnotationReferenceProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationCompletionProviderParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.annotation.extension.parameter.PhpAnnotationReferenceProviderParameter;
import de.espend.idea.php.drupal.index.PermissionIndex;
import de.espend.idea.php.drupal.utils.IndexUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * "@ContentEntityTypeAnnotation"
 */
public class ConfigEntityTypeAnnotation implements PhpAnnotationCompletionProvider, PhpAnnotationReferenceProvider {
    @Override
    public void getPropertyValueCompletions(AnnotationPropertyParameter parameter, AnnotationCompletionProviderParameter completionProviderParameter) {
        if(!isSupported(parameter)) {
            return;
        }

        if("admin_permission".equalsIgnoreCase(parameter.getPropertyName())) {
            completionProviderParameter.getResult().addAllElements(IndexUtil.getIndexedKeyLookup(parameter.getProject(), PermissionIndex.KEY));
        }
    }

    private boolean isSupported(@NotNull AnnotationPropertyParameter parameter) {
        return parameter.getType() != AnnotationPropertyParameter.Type.DEFAULT &&
            PhpLangUtil.equalsClassNames(StringUtils.stripStart(parameter.getPhpClass().getFQN(), "\\"), "Drupal\\Core\\Entity\\Annotation\\ConfigEntityType");
    }

    @Nullable
    @Override
    public PsiReference[] getPropertyReferences(AnnotationPropertyParameter parameter, PhpAnnotationReferenceProviderParameter phpAnnotationReferenceProviderParameter) {
        if(!isSupported(parameter)) {
            return new PsiReference[0];
        }

        String propertyName = parameter.getPropertyName();

        if("admin_permission".equalsIgnoreCase(propertyName)) {
            String contents = getContents(parameter.getElement());
            if(StringUtils.isBlank(contents)) {
                return new PsiReference[0];
            }

            return new PsiReference[] {new ContentEntityTypeAnnotation.MyPermissionPsiPolyVariantReferenceBase(parameter.getElement(), contents)};
        }

        return new PsiReference[0];
    }

    @Nullable
    private String getContents(@NotNull PsiElement element) {
        if(!(element instanceof StringLiteralExpression)) {
            return null;
        }

        String contents = ((StringLiteralExpression) element).getContents();
        if(StringUtils.isBlank(contents)) {
            return null;
        }

        return contents;
    }
}
