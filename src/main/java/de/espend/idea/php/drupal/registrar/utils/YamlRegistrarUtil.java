package de.espend.idea.php.drupal.registrar.utils;

import com.intellij.psi.PsiElement;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLScalar;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlRegistrarUtil {

    @Nullable
    public static String getYamlScalarKey(@NotNull PsiElement psiElement) {
        PsiElement parent = psiElement.getParent();

        if(!(parent instanceof YAMLScalar)) {
            return null;
        }

        String text = ((YAMLScalar) parent).getTextValue();
        if(StringUtils.isBlank(text)) {
            return null;
        }

        return text;
    }
}
