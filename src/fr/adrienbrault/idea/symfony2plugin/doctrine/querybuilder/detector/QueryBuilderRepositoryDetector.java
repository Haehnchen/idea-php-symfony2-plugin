package fr.adrienbrault.idea.symfony2plugin.doctrine.querybuilder.detector;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface QueryBuilderRepositoryDetector {

    @Nullable
    String getRepository(@NotNull QueryBuilderRepositoryDetectorParameter parameter);

}
