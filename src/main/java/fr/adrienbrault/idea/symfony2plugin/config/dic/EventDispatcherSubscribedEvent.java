package fr.adrienbrault.idea.symfony2plugin.config.dic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EventDispatcherSubscribedEvent {

    private String stringValue;
    private String fqnClassName;

    @Nullable
    private final String methodName;

    private String signature = null;
    private String type = "EventSubscriber";

    public EventDispatcherSubscribedEvent(@NotNull String stringValue, @NotNull String fqnClassName, @Nullable String methodName) {
        this.stringValue = stringValue;
        this.fqnClassName = fqnClassName;
        this.methodName = methodName;
    }

    public EventDispatcherSubscribedEvent(@NotNull String stringValue, @NotNull String fqnClassName, @Nullable String methodName, @NotNull String signature) {
        this(stringValue, fqnClassName, methodName);
        this.signature = signature;
    }

    public String getStringValue() {
        return stringValue;
    }

    public String getFqnClassName() {
        return fqnClassName;
    }

    @Nullable
    public String getSignature() {
        return signature;
    }


    public String getType() {
        return type;
    }

    public EventDispatcherSubscribedEvent setType(String type) {
        this.type = type;
        return this;
    }

    @Nullable
    public String getMethodName() {
        return methodName;
    }}
