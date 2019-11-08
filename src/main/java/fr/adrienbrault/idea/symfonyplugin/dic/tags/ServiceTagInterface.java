package fr.adrienbrault.idea.symfony2plugin.dic.tags;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceTagInterface {

    @NotNull
    String getServiceId();

    @NotNull
    String getName();

    /**
     * Our abstract method to get tag attributes
     *
     * @param attr yaml hash attribute to get value of
     * @return value
     */
    @Nullable
    String getAttribute(@NotNull String attr);
}
