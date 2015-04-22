package fr.adrienbrault.idea.symfony2plugin.config.xml.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
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

    private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {

        PsiFile psiFile = holder.getFile();
        if(psiFile.getFileType() != XmlFileType.INSTANCE || !Symfony2ProjectComponent.isEnabled(psiFile.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        for (XmlTag xmlTag : ServiceActionUtil.getXmlContainerServiceDefinition(psiFile)) {
            visitService(xmlTag, holder);
        }

        this.lazyServiceCollector = null;

        return super.buildVisitor(holder, isOnTheFly);
    }

    protected void visitService(XmlTag xmlTag, @NotNull ProblemsHolder holder) {

        if(!ServiceActionUtil.isValidXmlParameterInspectionService(xmlTag)) {
            return;
        }

        final List<String> args = ServiceActionUtil.getXmlMissingArgumentTypes(xmlTag, false, getLazyServiceCollector(xmlTag));
        if (args == null) {
            return;
        }

        PsiElement childrenOfType = PsiElementUtils.getChildrenOfType(xmlTag, PlatformPatterns.psiElement(XmlTokenType.XML_NAME));
        if(childrenOfType == null) {
            return;
        }

        holder.registerProblem(childrenOfType, "Missing Argument", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new AddServiceXmlArgumentLocalQuickFix(args));
    }

    private ContainerCollectionResolver.LazyServiceCollector getLazyServiceCollector(XmlTag xmlTag) {

        if(this.lazyServiceCollector != null) {
            return this.lazyServiceCollector;
        }

        return this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(xmlTag.getProject());
    }

}
