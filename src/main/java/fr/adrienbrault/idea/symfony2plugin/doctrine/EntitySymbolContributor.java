package fr.adrienbrault.idea.symfony2plugin.doctrine;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.navigation.NavigationItemExStateless;
import fr.adrienbrault.idea.symfony2plugin.util.dict.DoctrineModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class EntitySymbolContributor implements ChooseByNameContributorEx {
    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        Project project = scope.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (DoctrineModel modelClass : EntityHelper.getModelClasses(project)) {
            processor.process(modelClass.getPhpClass().getName());
        }
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (PhpClass phpClass : EntityHelper.getModelClasses(project).stream().map((Function<DoctrineModel, PhpClass>) DoctrineModel::getPhpClass).collect(Collectors.toSet())) {
            if (name.equals(phpClass.getName())) {
                processor.process(NavigationItemExStateless.create(phpClass, name, Symfony2Icons.DOCTRINE, "Entity", false));
            }
        }
    }
}
