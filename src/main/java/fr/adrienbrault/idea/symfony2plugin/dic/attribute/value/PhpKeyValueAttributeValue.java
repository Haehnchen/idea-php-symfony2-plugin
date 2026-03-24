package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpKeyValueAttributeValue extends AttributeValueAbstract {
    @NotNull
    private final Map<String, String> values;
    @NotNull
    private final Map<String, Collection<String>> arrayValues;
    @NotNull
    private final Collection<String> tags;

    public PhpKeyValueAttributeValue(@NotNull PsiElement psiElement, @NotNull Map<String, String> values) {
        this(psiElement, values, Collections.emptyMap(), Collections.emptyList());
    }

    public PhpKeyValueAttributeValue(@NotNull PsiElement psiElement, @NotNull Map<String, String> values, @NotNull Map<String, Collection<String>> arrayValues, @NotNull Collection<String> tags) {
        super(psiElement);
        this.values = values;
        this.arrayValues = arrayValues;
        this.tags = tags;
    }

    @Nullable
    @Override
    public String getString(@NotNull String key) {
        return this.values.get(key);
    }

    @NotNull
    @Override
    public Collection<String> getStringArray(@NotNull String key) {
        return this.arrayValues.getOrDefault(key, Collections.emptyList());
    }

    @NotNull
    @Override
    public Collection<String> getTags() {
        return this.tags;
    }
}
