package fr.adrienbrault.idea.symfony2plugin.config.component.parser;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParameterServiceParser extends AbstractServiceParser {

    protected Map<String, String> parameterMap = new ConcurrentHashMap<String, String>();

    @Override
    public String getXPathFilter() {
        return "";
    }

    public void parser(final File file) {
        this.parameterMap = new ConcurrentHashMap<String, String>() {{
            putAll(ParameterServiceCollector.collect(file));
        }};
    }

    public Map<String, String> getParameterMap() {
        return parameterMap;
    }

}