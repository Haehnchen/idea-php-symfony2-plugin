package fr.adrienbrault.idea.symfony2plugin.util.service;

import java.io.File;

public interface ServiceParserInterface {
    public String getXPathFilter();
    public void parser(File file);
}
