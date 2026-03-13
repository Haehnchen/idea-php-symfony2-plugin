package fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.driver;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.dict.DoctrineMetadataModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-file cache decorator for {@link DoctrineMappingDriverInterface} implementations
 * that parse metadata from a single PSI file (XML/YAML mapping drivers).
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class DoctrineMappingDriverFileCacheDecorator implements DoctrineMappingDriverInterface {

    private static final Key<CachedValue<Map<String, DoctrineMetadataModel>>> XML_CACHE = new Key<>("DOCTRINE_MAPPING_DRIVER_FILE_CACHE_XML");
    private static final Key<CachedValue<Map<String, DoctrineMetadataModel>>> YAML_CACHE = new Key<>("DOCTRINE_MAPPING_DRIVER_FILE_CACHE_YAML");

    @NotNull
    private final DoctrineMappingDriverInterface delegate;

    @NotNull
    private final Key<CachedValue<Map<String, DoctrineMetadataModel>>> cacheKey;

    public DoctrineMappingDriverFileCacheDecorator(@NotNull DoctrineMappingDriverInterface delegate) {
        this.delegate = delegate;
        this.cacheKey = delegate instanceof DoctrineXmlMappingDriver ? XML_CACHE : YAML_CACHE;
    }

    @Nullable
    @Override
    public DoctrineMetadataModel getMetadata(@NotNull DoctrineMappingDriverArguments arguments) {
        PsiFile file = arguments.getPsiFile();

        Map<String, DoctrineMetadataModel> cache = CachedValuesManager.getCachedValue(file, cacheKey, () ->
            CachedValueProvider.Result.create(new HashMap<>(), file)
        );

        String className = arguments.getClassName();
        if (!cache.containsKey(className)) {
            cache.put(className, delegate.getMetadata(arguments));
        }

        return cache.get(className);
    }
}
