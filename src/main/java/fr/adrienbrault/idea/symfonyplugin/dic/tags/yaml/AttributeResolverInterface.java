package fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface AttributeResolverInterface {
    @Nullable
    String getAttribute(@NotNull String attr);
}
