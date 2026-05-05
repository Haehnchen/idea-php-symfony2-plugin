package fr.adrienbrault.idea.symfony2plugin.twig.variable.collector;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpClassFqnIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.cache.FileIndexCaches;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ServicesDefinitionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalEnum;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalVariable;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.globals.TwigGlobalsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigFileVariableCollectorParameter;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcher;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerGlobalVariableCollector implements TwigFileVariableCollector {
    private static final Key<CachedValue<Map<String, Set<String>>>> CACHE = new Key<>("TWIG_SERVICE_CONTAINER_GLOBALS");

    @Override
    public void collect(@NotNull TwigFileVariableCollectorParameter parameter, @NotNull Map<String, Set<String>> variables) {
        variables.putAll(getGlobals(parameter.getProject()));
    }

    @NotNull
    private static Map<String, Set<String>> getGlobals(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHE,
            () -> CachedValueProvider.Result.create(
                getGlobalsInner(project),
                FileIndexCaches.getModificationTrackerForIndexId(project, ServicesDefinitionStubIndex.KEY),
                FileIndexCaches.getModificationTrackerForIndexId(project, PhpClassFqnIndex.KEY),
                SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(project).getModificationTracker(SymfonyVarDirectoryWatcher.Scope.CONTAINER)
            ),
            false
        );
    }

    @NotNull
    private static Map<String, Set<String>> getGlobalsInner(@NotNull Project project) {
        Map<String, Set<String>> map = new HashMap<>();

        TwigGlobalsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigGlobalsServiceParser.class);
        for(Map.Entry<String, TwigGlobalVariable> globalVariableEntry: twigPathServiceParser.getTwigGlobals().entrySet()) {
            if(globalVariableEntry.getValue().getTwigGlobalEnum() == TwigGlobalEnum.SERVICE) {
                String serviceName = globalVariableEntry.getValue().getValue();
                PhpClass phpClass = ServiceUtil.getServiceClass(project, serviceName);
                if(phpClass != null) {
                    String key = globalVariableEntry.getKey();
                    map.putIfAbsent(key, new HashSet<>());
                    map.get(key).add("\\" + StringUtils.stripStart(phpClass.getFQN(), "\\"));
                }
            }
        }

        return toImmutableMap(map);
    }

    @NotNull
    private static Map<String, Set<String>> toImmutableMap(@NotNull Map<String, Set<String>> map) {
        Map<String, Set<String>> result = new HashMap<>();
        map.forEach((key, value) -> result.put(key, Collections.unmodifiableSet(value)));

        return Collections.unmodifiableMap(result);
    }
}
