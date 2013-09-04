package fr.adrienbrault.idea.symfony2plugin.config.dic;

import org.jetbrains.annotations.Nullable;

public class EventDispatcherSubscribedEvent {

    private String stringValue;
    private String fqnClassName;
    private String signature = null;
    private String type = "EventSubscriber";

    public EventDispatcherSubscribedEvent(String stringValue, String fqnClassName) {
        this.stringValue = stringValue;
        this.fqnClassName = fqnClassName;
    }

    public EventDispatcherSubscribedEvent(String stringValue, String fqnClassName, String signature) {
        this(stringValue, fqnClassName);
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

}
