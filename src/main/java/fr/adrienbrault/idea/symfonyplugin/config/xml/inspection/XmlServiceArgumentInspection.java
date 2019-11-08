package fr.adrienbrault.idea.symfonyplugin.config.xml.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfonyplugin.action.quickfix.AddServiceXmlArgumentLocalQuickFix;
import fr.adrienbrault.idea.symfonyplugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceArgumentInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new MyPsiElementVisitor(holder);
    }

    private static class MyPsiElementVisitor extends PsiElementVisitor {

        private final ProblemsHolder holder;
        private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitFile(PsiFile file) {
            for (XmlTag xmlTag : ServiceActionUtil.getXmlContainerServiceDefinition(file)) {
                visitService(xmlTag, holder);
            }

            this.lazyServiceCollector = null;

            super.visitFile(file);
        }

        private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(XmlTag xmlTag) {
            if(this.lazyServiceCollector != null) {
                return this.lazyServiceCollector;
            }

            return this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(xmlTag.getProject());
        }

        private void visitService(XmlTag xmlTag, @NotNull ProblemsHolder holder) {
            if(!ServiceActionUtil.isValidXmlParameterInspectionService(xmlTag)) {
                return;
            }

            final List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes(xmlTag, false, getLazyServiceCollector(xmlTag));
            if (args.size() == 0) {
                return;
            }

            PsiElement childrenOfType = PsiElementUtils.getChildrenOfType(xmlTag, PlatformPatterns.psiElement(XmlTokenType.XML_NAME));
            if(childrenOfType == null) {
                return;
            }

            holder.registerProblem(childrenOfType, "Missing argument", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new AddServiceXmlArgumentLocalQuickFix(args));
        }
    }
}
