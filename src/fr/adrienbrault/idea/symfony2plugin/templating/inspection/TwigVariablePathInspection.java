package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigVariablePathInspection extends LocalInspectionTool {

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new MyPsiRecursiveElementVisitor(holder);
    }

    private static class MyPsiRecursiveElementVisitor extends PsiElementVisitor {

        @NotNull
        private final ProblemsHolder holder;

        MyPsiRecursiveElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(PsiElement element) {
            if(TwigHelper.getTypeCompletionPattern().accepts(element)) {
                visit(element);
            }
            super.visitElement(element);
        }

        private void visit(@NotNull PsiElement element) {

            String[] beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(element);
            if(beforeLeaf.length == 0) {
                return;
            }

            Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(element, beforeLeaf);
            if(types.size() == 0) {
                return;
            }

            Symfony2InterfacesUtil symfony2InterfacesUtil = null;

            for(TwigTypeContainer twigTypeContainer: types) {
                PhpNamedElement phpNamedElement = twigTypeContainer.getPhpNamedElement();
                if(phpNamedElement == null) {
                    continue;
                }

                if(symfony2InterfacesUtil == null) {
                    symfony2InterfacesUtil = new Symfony2InterfacesUtil();
                }

                if(isWeakPhpClass(symfony2InterfacesUtil, phpNamedElement)) {
                    return;
                }

                String text = element.getText();
                if(TwigTypeResolveUtil.getTwigPhpNameTargets(phpNamedElement, text).size() > 0) {
                    return;
                }
            }

            this.holder.registerProblem(element, "Field or method not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }

        private boolean isWeakPhpClass(Symfony2InterfacesUtil symfony2InterfacesUtil, PhpNamedElement phpNamedElement) {
            return phpNamedElement instanceof PhpClass && (
                 symfony2InterfacesUtil.isInstanceOf((PhpClass) phpNamedElement, "ArrayAccess") ||
                 symfony2InterfacesUtil.isInstanceOf((PhpClass) phpNamedElement, "Iterator")
            );
        }
    }
}
