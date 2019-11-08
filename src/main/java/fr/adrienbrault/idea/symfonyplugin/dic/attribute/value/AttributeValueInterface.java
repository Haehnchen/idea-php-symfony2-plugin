package fr.adrienbrault.idea.symfonyplugin.dic.attribute.value;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    PsiElement getPsiElement();
}
