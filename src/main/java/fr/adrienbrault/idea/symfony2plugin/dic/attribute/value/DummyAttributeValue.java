package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DummyAttributeValue implements AttributeValueInterface {

    @NotNull
    private final PsiElement psiElement;

    public DummyAttributeValue(@NotNull PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    @Nullable
    @Override
    public String getString(@NotNull String key) {
        return null;
    }

    @Nullable
    @Override
    public Boolean getBoolean(@NotNull String key) {
        return null;
    }

    @Nullable
    @Override
    public String getString(@NotNull String key, String defaultValue) {
        return defaultValue;
    }

    @Nullable
    @Override
    public Boolean getBoolean(@NotNull String key, Boolean defaultValue) {
        return defaultValue;
    }

    @NotNull
    @Override
    public Collection<String> getTags() {
        return Collections.emptySet();
    }

    @NotNull
    @Override
    public PsiElement getPsiElement() {
        return this.psiElement;
    }
}
