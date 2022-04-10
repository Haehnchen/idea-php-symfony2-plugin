package de.espend.idea.php.drupal.annotation;

import com.jetbrains.php.lang.PhpLangUtil;
import de.espend.idea.php.annotation.extension.PhpAnnotationCompletionProvider;
import de.espend.idea.php.annotation.extension.parameter.AnnotationCompletionProviderParameter;
import de.espend.idea.php.annotation.extension.parameter.AnnotationPropertyParameter;
import de.espend.idea.php.drupal.utils.TranslationUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 *
 * "@Translation("<caret>")"
 */
public class TranslationAnnotationReference implements PhpAnnotationCompletionProvider {
    @Override
    public void getPropertyValueCompletions(AnnotationPropertyParameter parameter, AnnotationCompletionProviderParameter annotationCompletionProviderParameter) {
        if(!isSupported(parameter)) {
            return;
        }

        TranslationUtil.attachGetTextLookupKeys(annotationCompletionProviderParameter.getResult(), parameter.getProject());
    }

    private boolean isSupported(@NotNull AnnotationPropertyParameter parameter) {
        return parameter.getType() == AnnotationPropertyParameter.Type.DEFAULT &&
            PhpLangUtil.equalsClassNames(StringUtils.stripStart(parameter.getPhpClass().getFQN(), "\\"), "Drupal\\Core\\Annotation\\Translation");
    }
}
