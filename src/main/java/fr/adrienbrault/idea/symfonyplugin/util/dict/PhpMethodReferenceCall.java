package fr.adrienbrault.idea.symfonyplugin.util.dict;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpMethodReferenceCall {

    @NotNull
    private final String clazz;

    @NotNull
    private final String[] methods;

    private final int index;

    public PhpMethodReferenceCall(@NotNull String clazz, @NotNull String... method) {
        this(clazz, method, 0);
    }

    public PhpMethodReferenceCall(@NotNull String clazz, @NotNull String method, int index) {
        this(clazz, new String[] { method }, index);
    }

    public PhpMethodReferenceCall(@NotNull String clazz, int index, @NotNull String... method) {
        this(clazz, method, index);
    }

    public PhpMethodReferenceCall(@NotNull String clazz, @NotNull String[] methods, int index) {
        this.clazz = clazz;
        this.methods = methods;
        this.index = index;
    }

    @NotNull
    public String getClassName() {
        return this.clazz;
    }

    @NotNull
    public String[] getMethods() {
        return this.methods;
    }

    public int getIndex() {
        return this.index;
    }
}
