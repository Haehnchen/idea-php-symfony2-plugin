package fr.adrienbrault.idea.symfony2plugin.util.service;

import java.io.InputStream;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public interface ServiceParserInterface {
    String getXPathFilter();
    void parser(InputStream file);
}
