package fr.adrienbrault.idea.symfonyplugin.config.component.parser;

import fr.adrienbrault.idea.symfonyplugin.util.service.AbstractServiceParser;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
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