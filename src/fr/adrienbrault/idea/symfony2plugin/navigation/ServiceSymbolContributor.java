package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ServiceIndexUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ServiceSymbolContributor implements ChooseByNameContributor {

    @NotNull
    @Override
    public String[] getNames(Project project, boolean b) {
        Collection<String> services = ContainerCollectionResolver.getServiceNames(project);
        return ArrayUtil.toStringArray(services);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String serviceName, String s2, Project project, boolean b) {

        List<NavigationItem> navigationItems = new ArrayList<NavigationItem>();

        for(PsiElement psiElement: ServiceIndexUtil.findServiceDefinitions(project, serviceName)) {
            if(psiElement instanceof NavigationItem) {
                navigationItems.add(new NavigationItemEx(psiElement, serviceName, Symfony2Icons.SERVICE, "Service"));
            }
        }

        return navigationItems.toArray(new NavigationItem[navigationItems.size()]);
    }

}
