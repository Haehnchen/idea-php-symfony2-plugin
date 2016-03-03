package fr.adrienbrault.idea.symfony2plugin.util.service;

import java.io.InputStream;

public interface ServiceParserInterface {
    String getXPathFilter();
    void parser(InputStream file);
}
