package fr.adrienbrault.idea.symfony2plugin.ux;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.navigation.NavigationItemExStateless;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class UxComponentSymbolContributor implements ChooseByNameContributorEx {
    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        Project project = scope.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (UxUtil.TwigComponent allComponentName : UxUtil.getAllComponentNames(project)) {
            processor.process(allComponentName.name());
        }
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (PhpClass component : UxUtil.getTwigComponentPhpClasses(project, name)) {
            processor.process(NavigationItemExStateless.create(component, name, component.getIcon(), "TwigComponent (" + component.getName() + ")", false));
        }

        for (PsiFile component : UxUtil.getComponentTemplates(project, name)) {
            processor.process(NavigationItemExStateless.create(component, name, TwigIcons.TwigFileIcon, "TwigComponent (" + component.getName() + ")", false));
        }
    }
}
