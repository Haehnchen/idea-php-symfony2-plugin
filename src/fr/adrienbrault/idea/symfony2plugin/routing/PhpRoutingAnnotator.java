package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collection;
import java.util.Map;

public class PhpRoutingAnnotator {

    public static void annotateRouteName(PsiElement target, @NotNull AnnotationHolder holder, final String routeName) {

        Symfony2ProjectComponent symfony2ProjectComponent = target.getProject().getComponent(Symfony2ProjectComponent.class);
        Map<String, Route> routes = symfony2ProjectComponent.getRoutes();

        if(routes.containsKey(routeName))  {
            return;
        }

        Collection fileCollection = FileBasedIndex.getInstance().getContainingFiles(RoutesStubIndex.KEY, routeName,  GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(target.getProject()), YAMLFileType.YML, XmlFileType.INSTANCE));
        if(fileCollection.size() > 0) {
            holder.createWeakWarningAnnotation(target, "Weak Route");
            return;
        }

        fileCollection = FileBasedIndex.getInstance().getContainingFiles(AnnotationRoutesStubIndex.KEY, routeName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(target.getProject()), PhpFileType.INSTANCE));
        if(fileCollection.size() > 0) {
            holder.createWeakWarningAnnotation(target, "Weak Route");
            return;
        }

        holder.createWarningAnnotation(target, "Missing Route");

    }

}