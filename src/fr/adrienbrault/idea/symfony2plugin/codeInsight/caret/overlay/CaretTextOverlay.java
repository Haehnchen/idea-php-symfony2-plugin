package fr.adrienbrault.idea.symfony2plugin.codeInsight.caret.overlay;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface CaretTextOverlay {

    @Nullable
    CaretTextOverlayElement getOverlay(@NotNull CaretTextOverlayArguments args);

    boolean accepts(@NotNull VirtualFile virtualFile);
}
