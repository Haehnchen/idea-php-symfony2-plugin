package fr.adrienbrault.idea.symfony2plugin.routing.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

abstract public class AbstractPhpRouteInspection extends LocalInspectionTool {

    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(!Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        if(psiFile instanceof PhpFile) {
            psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if(element instanceof StringLiteralExpression) {
                        annotate(((StringLiteralExpression) element).getContents(), element, holder);
                    }
                    super.visitElement(element);
                }
            });
        }

        /* too slow
        if(psiFile instanceof TwigFile) {
            psiFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {

                    if(TwigHelper.getAutocompletableRoutePattern().accepts(element)) {
                        annotateRouteName(element, holder, element.getText());
                    }

                    super.visitElement(element);
                }
            });
        }*/

        return super.buildVisitor(holder, isOnTheFly);
    }

    public void annotate(String routeName, @NotNull final PsiElement element, @NotNull ProblemsHolder holder) {

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

    abstract protected void annotateRouteName(PsiElement target, @NotNull ProblemsHolder holder, final String routeName);

}
