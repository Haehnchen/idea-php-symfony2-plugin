package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface AttributeValueInterface {

    @Nullable
    String getString(@NotNull String key);

    @Nullable
    Boolean getBoolean(@NotNull String key);

    @Nullable
    String getString(@NotNull String key, @Nullable String defaultValue);

    @Nullable
    Boolean getBoolean(@NotNull String key, @Nullable Boolean defaultValue);

    @NotNull
    Collection<String> getTags();

    @NotNull
    PsiElement getPsiElement();
}
