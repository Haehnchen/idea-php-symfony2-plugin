package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHelper;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateFileContributor implements ChooseByNameContributor {
    @NotNull
    @Override
    public String[] getNames(Project project, boolean b) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return new String[0];
        }

        Collection<String> twigFileNames = TwigHelper.getTwigFileNames(project);
        return twigFileNames.toArray(new String[twigFileNames.size()]);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String templateName, String s2, Project project, boolean b) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return new NavigationItem[0];
        }

        return Arrays.stream(TwigHelper.getTemplatePsiElements(project, templateName))
            .map(psiFile ->
                new NavigationItemEx(psiFile, templateName, TwigIcons.TwigFileIcon, "Template", false)
            )
            .toArray(NavigationItemEx[]::new);
    }
}
