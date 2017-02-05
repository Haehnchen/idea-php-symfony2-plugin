package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.AnnotationRoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpRouteInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject()) || !Settings.getInstance(psiFile.getProject()).phpAnnotateRoute) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if(element instanceof StringLiteralExpression) {
                    annotate((StringLiteralExpression) element, holder);
                }
                super.visitElement(element);
            }
        });


        return super.buildVisitor(holder, isOnTheFly);
    }

    public void annotate(@NotNull final StringLiteralExpression element, @NotNull ProblemsHolder holder) {

        String routeName = element.getContents();
        if(StringUtils.isBlank(routeName)) {
            return;
        }

        MethodMatcher.MethodMatchParameter methodMatchParameter = new MethodMatcher.StringParameterMatcher(element, 0)
            .withSignature(PhpRouteReferenceContributor.GENERATOR_SIGNATURES)
            .match();

        if(methodMatchParameter == null) {
            return;
        }

        annotateRouteName(element, holder, routeName);
    }

    public static void annotateRouteName(PsiElement target, @NotNull ProblemsHolder holder, final String routeName) {
        Map<String, Route> routes = RouteHelper.getCompiledRoutes(target.getProject());
        if(routes.containsKey(routeName))  {
            return;
        }

        Collection fileCollection = FileBasedIndex.getInstance().getContainingFiles(RoutesStubIndex.KEY, routeName,  GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(target.getProject()), YAMLFileType.YML, XmlFileType.INSTANCE));
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
