package fr.adrienbrault.idea.symfony2plugin.dic.attribute.value;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpKeyValueAttributeValue extends AttributeValueAbstract {
    @NotNull
    private final Map<String, String> values;

    public PhpKeyValueAttributeValue(@NotNull PsiElement psiElement, @NotNull Map<String, String> values) {
        super(psiElement);
        this.values = values;
    }

    @Nullable
    @Override
    public String getString(@NotNull String key) {
        return this.values.get(key);
    }
}
