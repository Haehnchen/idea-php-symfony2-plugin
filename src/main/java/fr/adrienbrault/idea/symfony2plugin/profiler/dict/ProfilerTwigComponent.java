package fr.adrienbrault.idea.symfony2plugin.profiler.dict;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Symfony UX Twig component data from the {@code twig_component} profiler collector.
 *
 * @param name Component name as rendered in Twig, for example {@code AlertBanner} or {@code Shop:Card}.
 * @param className Fully-qualified PHP class name with leading backslash, for example {@code \App\Twig\Components\ShopCard}.
 *                  Anonymous components use {@code \Symfony\UX\TwigComponent\AnonymousComponent}; {@code null} means the profiler did not provide a class.
 * @param template Logical Twig template name, for example {@code components/Shop/Card.html.twig}; no runtime path is stored here.
 * @param renderCount Render count from the profiler, used only for aggregation and ordering.
 */
public record ProfilerTwigComponent(
    @NotNull String name,
    @Nullable String className,
    @Nullable String template,
    int renderCount
) {
    private static final String ANONYMOUS_COMPONENT_CLASS = "\\Symfony\\UX\\TwigComponent\\AnonymousComponent";

    public ProfilerTwigComponent {
        Objects.requireNonNull(name, "name");

        if (className != null && !className.startsWith("\\")) {
            throw new IllegalArgumentException("className must be a fully-qualified PHP class name with leading backslash: " + className);
        }
    }

    public boolean isAnonymous() {
        return ANONYMOUS_COMPONENT_CLASS.equals(className);
    }

    @NotNull
    public ProfilerTwigComponent merge(@NotNull ProfilerTwigComponent component) {
        return new ProfilerTwigComponent(
            name,
            className != null ? className : component.className(),
            template != null ? template : component.template(),
            renderCount + component.renderCount()
        );
    }
}
