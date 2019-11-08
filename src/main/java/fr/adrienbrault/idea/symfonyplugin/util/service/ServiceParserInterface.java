package fr.adrienbrault.idea.symfonyplugin.util.service;

import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceParserInterface {
    String getXPathFilter();
    void parser(InputStream file);
}
