package fr.adrienbrault.idea.symfony2plugin.util.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

public interface ServiceParserInterface {
    public String getXPathFilter();
    public Object parser(File file);
}
