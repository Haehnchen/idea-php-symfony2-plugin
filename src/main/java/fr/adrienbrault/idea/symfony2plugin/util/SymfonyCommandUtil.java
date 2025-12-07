package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandTestRunLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandUtil {
    private static final Key<CachedValue<Map<String, String>>> SYMFONY_COMMAND_NAME_MAP = new Key<>("SYMFONY_COMMAND_NAME_MAP");

    @NotNull
    public static Collection<SymfonyCommand> getCommands(@NotNull Project project) {
        Map<String, String> cachedValue = CachedValuesManager.getManager(project).getCachedValue(
            project,
            SYMFONY_COMMAND_NAME_MAP,
            () -> {
                Map<String, String> symfonyCommands = new HashMap<>();

                // Traditional Command subclasses
                for (PhpClass phpClass : PhpIndexUtil.getAllSubclasses(project, "\\Symfony\\Component\\Console\\Command\\Command")) {
                    if (PhpElementsUtil.isTestClass(phpClass)) {
                        continue;
                    }

                    for (String commandName : SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass)) {
                        symfonyCommands.put(commandName, phpClass.getFQN());
                    }
                }

                // AsCommand attributes from index
                for (PhpClass phpClass : PhpAttributeIndexUtil.getClassesWithAttribute(project, "\\Symfony\\Component\\Console\\Attribute\\AsCommand")) {
                    if (PhpElementsUtil.isTestClass(phpClass)) {
                        continue;
                    }

                    // Extract command names from the class (using existing method)
                    for (String commandName : SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass)) {
                        symfonyCommands.put(commandName, phpClass.getFQN());
                    }
                }

                return CachedValueProvider.Result.create(symfonyCommands, PsiModificationTracker.MODIFICATION_COUNT);
            },
            false
        );

        return cachedValue.entrySet().stream()
            .map(entry -> new SymfonyCommand(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }
}
