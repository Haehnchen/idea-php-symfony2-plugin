package fr.adrienbrault.idea.symfony2plugin.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.profiler.widget.SymfonyProfilerWidget;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyStatusbarWidgetFactory implements StatusBarWidgetFactory {
    @Override
    public @NonNls
    @NotNull String getId() {
        return "symfony.status.bar";
    }

    @Override
    public @Nls
    @NotNull String getDisplayName() {
        return "Symfony";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return Symfony2ProjectComponent.isEnabled(project);
    }

    @Override
    public @NotNull StatusBarWidget createWidget(@NotNull Project project) {
        return new SymfonyProfilerWidget(project);
    }

}
