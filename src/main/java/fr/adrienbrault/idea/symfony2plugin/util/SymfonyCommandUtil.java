package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.command.SymfonyCommandTestRunLineMarkerProvider;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

                for (PhpClass phpClass : PhpIndexUtil.getAllSubclasses(project, "\\Symfony\\Component\\Console\\Command\\Command")) {
                    if (PhpElementsUtil.isTestClass(phpClass)) {
                        continue;
                    }

                    for (String commandName : SymfonyCommandTestRunLineMarkerProvider.getCommandNameFromClass(phpClass)) {
                        symfonyCommands.put(commandName, phpClass.getFQN());
                    }
                }

                return CachedValueProvider.Result.create(symfonyCommands, PsiModificationTracker.MODIFICATION_COUNT);
            },
            false
        );

        Collection<SymfonyCommand> symfonyCommands = new ArrayList<>();
        for (Map.Entry<String, String> entry : cachedValue.entrySet()) {
            Collection<PhpClass> anyByFQN = PhpIndex.getInstance(project).getAnyByFQN(entry.getValue());
            if (anyByFQN.isEmpty()) {
                continue;
            }

            symfonyCommands.add(new SymfonyCommand(entry.getKey(), anyByFQN.iterator().next()));
        }

        return symfonyCommands;
    }
}
