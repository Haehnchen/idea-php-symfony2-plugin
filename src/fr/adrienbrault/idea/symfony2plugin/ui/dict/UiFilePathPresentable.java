package fr.adrienbrault.idea.symfony2plugin.ui.dict;

import org.jetbrains.annotations.NotNull;

public class UiFilePathPresentable {

    @NotNull
    private final String path;

    @NotNull
    private final String info;

    public UiFilePathPresentable(@NotNull String path, @NotNull String info) {
        this.path = path;
        this.info = info;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    @NotNull
    public String getInfo() {
        return info;
    }
}
