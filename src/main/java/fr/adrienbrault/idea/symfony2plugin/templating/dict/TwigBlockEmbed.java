package fr.adrienbrault.idea.symfony2plugin.templating.dict;

import com.jetbrains.twig.elements.TwigBlockStatement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public record TwigBlockEmbed(@NotNull String templateName, @NotNull String blockName, @NotNull TwigBlockStatement target) {
}
