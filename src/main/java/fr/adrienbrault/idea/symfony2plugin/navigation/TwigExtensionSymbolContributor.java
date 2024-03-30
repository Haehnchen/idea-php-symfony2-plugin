package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributorEx;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FindSymbolParameters;
import com.intellij.util.indexing.IdFilter;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtensionSymbolContributor implements ChooseByNameContributorEx {
    @Override
    public void processNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter filter) {
        Project project = scope.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        for (Map<String, TwigExtension> extensionMap : Arrays.asList(TwigExtensionParser.getFilters(project), TwigExtensionParser.getFunctions(project))) {
            for (String twigFilter: extensionMap.keySet()) {
                processor.process(twigFilter);
            }
        }
    }

    @Override
    public void processElementsWithName(@NotNull String name, @NotNull Processor<? super NavigationItem> processor, @NotNull FindSymbolParameters parameters) {
        Project project = parameters.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return;
        }

        Set<PsiElement> given = new HashSet<>();
        for (Map<String, TwigExtension> extensionMap : Arrays.asList(TwigExtensionParser.getFilters(project), TwigExtensionParser.getFunctions(project))) {
            for (Map.Entry<String, TwigExtension> twigFunc: extensionMap.entrySet()) {
                if (twigFunc.getKey().equals(name)) {
                    TwigExtension twigExtension = twigFunc.getValue();
                    PsiElement extensionTarget = TwigExtensionParser.getExtensionTarget(project, twigExtension);
                    if (extensionTarget != null) {
                        processor.process(new NavigationItemEx(extensionTarget, name, TwigExtensionParser.getIcon(twigExtension.getTwigExtensionType()), "Twig:" + twigExtension.getTwigExtensionType()));
                    }
                }
            }
        }
    }
}
