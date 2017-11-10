package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

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

        Set<String> sets = TwigHelper.getTwigFilesByName(project).keySet();
        return sets.toArray(new String[sets.size()]);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String templateName, String s2, Project project, boolean b) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return new NavigationItem[0];
        }

        Collection<NavigationItemEx> items = new ArrayList<>();

        for (PsiFile psiFile : TwigHelper.getTemplatePsiElements(project, templateName)) {
            items.add(new NavigationItemEx(psiFile, templateName, TwigIcons.TwigFileIcon, "Template", false));
        }

        return items.toArray(new NavigationItemEx[items.size()]);
    }
}
