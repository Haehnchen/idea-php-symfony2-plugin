package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collection;
import java.util.Map;

public class PhpRouteWeakInspection extends AbstractPhpRouteInspection {

    protected void annotateRouteName(PsiElement target, @NotNull ProblemsHolder holder, final String routeName) {

        Symfony2ProjectComponent symfony2ProjectComponent = target.getProject().getComponent(Symfony2ProjectComponent.class);
        Map<String, Route> routes = symfony2ProjectComponent.getRoutes();

        if(routes.containsKey(routeName))  {
            return;
        }

        Collection fileCollection = FileBasedIndex.getInstance().getContainingFiles(RoutesStubIndex.KEY, routeName,  GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(target.getProject()), YAMLFileType.YML, XmlFileType.INSTANCE));
        if(fileCollection.size() > 0) {
            holder.registerProblem(target, "Weak Route", ProblemHighlightType.WEAK_WARNING);
            return;
        }

        fileCollection = FileBasedIndex.getInstance().getContainingFiles(AnnotationRoutesStubIndex.KEY, routeName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(target.getProject()), PhpFileType.INSTANCE));
        if(fileCollection.size() > 0) {
            holder.registerProblem(target, "Weak Route", ProblemHighlightType.WEAK_WARNING);
            return;
        }

    }

}
