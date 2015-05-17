package fr.adrienbrault.idea.symfony2plugin.action.generator.naming;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceNameStrategyInterface {
    @Nullable
    public String getServiceName(@NotNull ServiceNameStrategyParameter parameter);
}
