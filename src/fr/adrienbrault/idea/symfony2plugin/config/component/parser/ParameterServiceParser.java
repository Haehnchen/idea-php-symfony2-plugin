package fr.adrienbrault.idea.symfony2plugin.config.component.parser;

import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ParameterServiceParser extends AbstractServiceParser {

    protected Map<String, String> parameterMap = new ConcurrentHashMap<>();

    @Override
    public String getXPathFilter() {
        return "";
    }

    public void parser(final InputStream file) {
        this.parameterMap.putAll(ParameterServiceCollector.collect(file));
    }

    public Map<String, String> getParameterMap() {
        return parameterMap;
    }

}