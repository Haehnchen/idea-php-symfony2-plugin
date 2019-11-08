package fr.adrienbrault.idea.symfonyplugin.action.generator.naming;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceNameStrategyInterface {
    @Nullable
    String getServiceName(@NotNull ServiceNameStrategyParameter parameter);
}
