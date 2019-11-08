package fr.adrienbrault.idea.symfonyplugin.util.service;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceFileInterface {
    Object parser();
    void setCacheInvalid();
}
