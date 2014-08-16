package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.navigation.ChooseByNameContributor;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class TemplateFileContributor implements ChooseByNameContributor {

    @NotNull
    @Override
    public String[] getNames(Project project, boolean b) {
        Map<String, VirtualFile> psiElements = TwigHelper.getTwigFilesByName(project);
        Set<String> sets = psiElements.keySet();
        return sets.toArray(new String[sets.size()]);
    }

    @NotNull
    @Override
    public NavigationItem[] getItemsByName(String templateName, String s2, Project project, boolean b) {

        PsiFile psiFile = TwigHelper.getTemplateFileByName(project, templateName);
        if(psiFile == null) {
            return new NavigationItem[0];
        }

        return new NavigationItemEx[]{
            new NavigationItemEx(psiFile, templateName, TwigIcons.TwigFileIcon, "Template", false)
        };

    }

}
