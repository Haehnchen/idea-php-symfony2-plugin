package fr.adrienbrault.idea.symfony2plugin.config.xml.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTokenType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil;
import fr.adrienbrault.idea.symfony2plugin.action.quickfix.AddServiceXmlArgumentLocalQuickFix;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
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
        private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> serviceCollector;

        MyPsiElementVisitor(@NotNull ProblemsHolder holder) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            if (element instanceof XmlTag xmlTag) {
                visitService(xmlTag, holder, this.createLazyServiceCollector());
            }

            super.visitElement(element);
        }

        private void visitService(XmlTag xmlTag, @NotNull ProblemsHolder holder, @NotNull NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> lazyServiceCollector) {
            if(!ServiceActionUtil.isValidXmlParameterInspectionService(xmlTag)) {
                return;
            }

            final List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes(xmlTag, false, lazyServiceCollector.get());
            if (args.isEmpty()) {
                return;
            }

            PsiElement childrenOfType = PsiElementUtils.getChildrenOfType(xmlTag, PlatformPatterns.psiElement(XmlTokenType.XML_NAME));
            if(childrenOfType == null) {
                return;
            }

            holder.registerProblem(childrenOfType, "Missing argument", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new AddServiceXmlArgumentLocalQuickFix(args));
        }

        private NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> createLazyServiceCollector() {
            if (this.serviceCollector == null) {
                this.serviceCollector = NotNullLazyValue.lazy(() -> new ContainerCollectionResolver.LazyServiceCollector(holder.getProject()));
            }

            return this.serviceCollector;
        }
    }
}
