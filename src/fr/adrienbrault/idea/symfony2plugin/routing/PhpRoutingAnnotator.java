package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collection;
import java.util.Map;


public class PhpRoutingAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(element.getProject()) || !Settings.getInstance(element.getProject()).phpAnnotateRoute) {
            return;
        }

        MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element);
        if (methodReference == null || !new Symfony2InterfacesUtil().isUrlGeneratorGenerateCall(methodReference) || element.getContext() == null) {
            return;
        }

        // @TODO fully replace with pattern and think of:
        // $this->generateUrl('foo', array('foo' => 'foo'));
        ParameterBag parameterBag = PsiElementUtils.getCurrentParameterIndex(element.getContext());
        if(parameterBag == null || parameterBag.getIndex() != 0) {
            return;
        }


        final String routeName = Symfony2InterfacesUtil.getFirstArgumentStringValue(methodReference);
        if(routeName == null) {
            return;
        }

        annotateRouteName(element, holder, routeName);

    }

    public static void annotateRouteName(PsiElement target, @NotNull AnnotationHolder holder, final String routeName) {

        Symfony2ProjectComponent symfony2ProjectComponent = target.getProject().getComponent(Symfony2ProjectComponent.class);
        Map<String, Route> routes = symfony2ProjectComponent.getRoutes();

        if(routes.containsKey(routeName))  {
            return;
        }

        Collection fileCollection = FileBasedIndex.getInstance().getContainingFiles(YamlRoutesStubIndex.KEY, routeName,  GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(target.getProject()), YAMLFileType.YML));
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