package fr.adrienbrault.idea.symfony2plugin.util.service;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceFileInterface {
    Object parser();
    void setCacheInvalid();
}
