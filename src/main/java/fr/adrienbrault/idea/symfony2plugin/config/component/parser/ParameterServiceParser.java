package fr.adrienbrault.idea.symfony2plugin.config.component.parser;

import com.intellij.openapi.vfs.VirtualFile;
import fr.adrienbrault.idea.symfony2plugin.util.service.AbstractServiceParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ParameterServiceParser extends AbstractServiceParser {

    protected final Map<String, String> parameterMap = new ConcurrentHashMap<>();

    @Override
    public String getXPathFilter() {
        return "";
    }

    @Override
    public void parser(@NotNull InputStream inputStream, @Nullable VirtualFile sourceFile) {
        this.parameterMap.putAll(ParameterServiceCollector.collect(inputStream));
    }

    public Map<String, String> getParameterMap() {
        return parameterMap;
    }

}