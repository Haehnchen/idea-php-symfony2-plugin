package fr.adrienbrault.idea.symfonyplugin.util.yaml.visitor;

import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterVisitor {
    @NotNull
    private final String className;

    @NotNull
    private final String method;

    private final int parameterIndex;

    public ParameterVisitor(@NotNull String className, @NotNull String method, int parameterIndex) {
        this.className = className;
        this.method = method;
        this.parameterIndex = parameterIndex;
    }

    @NotNull
    public String getClassName() {
        return className;
    }

    public String getMethod() {
        return method;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }
}
