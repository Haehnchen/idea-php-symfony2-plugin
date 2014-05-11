package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlRoutesStubIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collection;
import java.util.Map;

public class PhpRouteMissingInspection extends AbstractPhpRouteInspection {

    protected void annotateRouteName(PsiElement target, @NotNull ProblemsHolder holder, final String routeName) {

        Symfony2ProjectComponent symfony2ProjectComponent = target.getProject().getComponent(Symfony2ProjectComponent.class);
        Map<String, Route> routes = symfony2ProjectComponent.getRoutes();

        if(routes.containsKey(routeName))  {
            return;
        }

        Collection fileCollection = FileBasedIndex.getInstance().getContainingFiles(YamlRoutesStubIndex.KEY, routeName,  GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(target.getProject()), YAMLFileType.YML));
        if(fileCollection.size() > 0) {
            return;
        }

        fileCollection = FileBasedIndex.getInstance().getContainingFiles(AnnotationRoutesStubIndex.KEY, routeName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(target.getProject()), PhpFileType.INSTANCE));
        if(fileCollection.size() > 0) {
            return;
        }

        holder.registerProblem(target, "Missing Route", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);

    }

}
