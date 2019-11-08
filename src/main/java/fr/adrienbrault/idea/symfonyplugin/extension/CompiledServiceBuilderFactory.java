package fr.adrienbrault.idea.symfony2plugin.extension;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface CompiledServiceBuilderFactory {

    @NotNull
    Builder create();

    interface Builder {
        void build(@NotNull CompiledServiceBuilderArguments args);
        boolean isModified(@NotNull Project project);
    }
}
