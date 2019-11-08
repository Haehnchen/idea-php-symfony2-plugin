package fr.adrienbrault.idea.symfonyplugin.navigation;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.util.ArrayUtil;
import fr.adrienbrault.idea.symfonyplugin.Symfony2Icons;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.util.SymfonyCommandUtil;
import fr.adrienbrault.idea.symfonyplugin.util.dict.SymfonyCommand;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class SymfonyCommandSymbolContributor implements ChooseByNameContributor {

    @NotNull
    @Override
    public String[] getNames(Project project, boolean b) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return new String[0];
        }

        Set<String> routeNames = new HashSet<>();
        for (SymfonyCommand symfonyCommand : SymfonyCommandUtil.getCommands(project)) {
            routeNames.add(symfonyCommand.getName());
        }

        return ArrayUtil.toStringArray(routeNames);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String name, String s2, Project project, boolean b) {
        if(!Symfony2ProjectComponent.isEnabled(project)) {
            return new NavigationItem[0];
        }

        List<NavigationItem> navigationItems = new ArrayList<>();

        for (SymfonyCommand symfonyCommand : SymfonyCommandUtil.getCommands(project)) {
            if(symfonyCommand.getName().equals(name)) {
                navigationItems.add(new NavigationItemEx(symfonyCommand.getPsiElement(), name, Symfony2Icons.SYMFONY, "Command"));
            }
        }

        return navigationItems.toArray(new NavigationItem[navigationItems.size()]);
    }

}
