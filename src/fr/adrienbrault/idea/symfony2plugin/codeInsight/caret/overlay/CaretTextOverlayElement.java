package fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CaretTextOverlayElement {

    @NotNull
    private final String text;

    public CaretTextOverlayElement(@NotNull String text) {
        this.text = text;
    }

    @NotNull
    public String getText() {
        return text;
    }
}
