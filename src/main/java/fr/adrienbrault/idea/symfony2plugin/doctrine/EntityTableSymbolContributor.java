package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.navigation.NavigationItemExStateless;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntityTableSymbolContributor implements ChooseByNameContributorEx {
    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        Project project = scope.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (Pair<String, String> pair : DoctrineMetadataUtil.getTables(project)) {
            processor.process(pair.getFirst());
        }
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (Pair<String, String> table : DoctrineMetadataUtil.getTables(project)) {
            if (name.equals(table.getFirst())) {
                PhpClass phpClass = PhpElementsUtil.getClassInterface(project, table.getSecond());
                if (phpClass != null) {
                    processor.process(NavigationItemExStateless.create(phpClass, table.getFirst(), Symfony2Icons.DOCTRINE, "Entity Table", false));
                }
            }
        }
    }
}
