package fr.adrienbrault.idea.symfony2plugin.util.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcher;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyVarDirectoryWatcherKt;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceXmlParserFactory {
    private static final Key<CachedValue<Map<Class<?>, ServiceParserInterface>>> CACHED_PARSERS = new Key<>("SYMFONY_SERVICE_XML_PARSERS");

    @SuppressWarnings("unchecked")
    private static <T extends ServiceParserInterface> T getCachedParser(@NotNull Project project, Class<T> serviceParser) {
        Map<Class<?>, ServiceParserInterface> parsers = CachedValuesManager.getManager(project).getCachedValue(
            project,
            CACHED_PARSERS,
            () -> CachedValueProvider.Result.create(
                new ConcurrentHashMap<>(),
                SymfonyVarDirectoryWatcherKt.getSymfonyVarDirectoryWatcher(project)
                    .getModificationTracker(SymfonyVarDirectoryWatcher.Scope.CONTAINER)
            ),
            false
        );

        return (T) parsers.computeIfAbsent(serviceParser, parserClass -> buildParserUnchecked(project, parserClass));
    }

    @SuppressWarnings("unchecked")
    private static ServiceParserInterface buildParserUnchecked(@NotNull Project project, Class<?> serviceParser) {
        return buildParser(project, (Class<? extends ServiceParserInterface>) serviceParser);
    }

    private static <T extends ServiceParserInterface> T buildParser(@NotNull Project project, Class<T> serviceParser) {
        T parserInstance;
        try {
            parserInstance = serviceParser.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to instantiate service parser: " + serviceParser.getName(), e);
        }

        Collection<VirtualFile> settingsServiceFiles = Symfony2ProjectComponent.getContainerFiles(project);
        for (VirtualFile vf : new ArrayList<>(settingsServiceFiles).stream().sorted(Comparator.comparing(VirtualFile::getPath)).toList()) {
            if (vf == null || !vf.exists()) {
                continue;
            }

            try (InputStream inputStream = vf.getInputStream()) {
                parserInstance.parser(inputStream, vf, project);
            } catch (IOException ignored) {
            }
        }

        return parserInstance;
    }

    public static <T extends ServiceParserInterface> T getInstance(Project project, Class<T> serviceParser) {
        return getCachedParser(project, serviceParser);
    }
}
