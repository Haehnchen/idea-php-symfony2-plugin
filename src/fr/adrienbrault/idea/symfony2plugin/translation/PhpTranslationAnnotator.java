package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.IncorrectOperationException;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PhpTranslationAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {

        if (!Symfony2ProjectComponent.isEnabled(psiElement)
            || !Settings.getInstance(psiElement.getProject()).phpAnnotateTranslation
            || !(psiElement instanceof StringLiteralExpression)
            || !(psiElement.getContext() instanceof ParameterList)
        ) {
            return;
        }

        ParameterList parameterList = (ParameterList) psiElement.getContext();

        if (parameterList == null || !(parameterList.getContext() instanceof MethodReference)) {
            return;
        }

        MethodReference method = (MethodReference) parameterList.getContext();
        Symfony2InterfacesUtil interfacesUtil = new Symfony2InterfacesUtil();
        if (!interfacesUtil.isTranslatorCall(method)) {
            return;
        }

        int domainParameter = 2;
        if(method.getName().equals("transChoice")) {
            domainParameter = 3;
        }

        ParameterBag currentIndex = PsiElementUtils.getCurrentParameterIndex(psiElement);
        if(currentIndex == null) {
            return;
        }

        if(currentIndex.getIndex() == domainParameter) {
            annotateTranslationDomain((StringLiteralExpression) psiElement, holder);
            return;
        }

        if(currentIndex.getIndex() == 0) {
            PsiElement domainElement = PsiElementUtils.getMethodParameterPsiElementAt(parameterList, domainParameter);

            // only support string values
            if(domainElement instanceof StringLiteralExpression) {
                String domain = PsiElementUtils.getMethodParameterAt(parameterList, domainParameter);

                if(domain == null) {
                    domain = "messages";
                }

                annotateTranslationKey((StringLiteralExpression) psiElement, domain, holder);

            } else if (domainElement == null) {
                annotateTranslationKey((StringLiteralExpression) psiElement, "messages", holder);
            }

        }

    }

    private void annotateTranslationKey(StringLiteralExpression psiElement, String domainName, @NotNull AnnotationHolder holder) {

        if(psiElement.getContents().length() > 0 && TranslationUtil.getTranslationPsiElements(psiElement.getProject(), psiElement.getContents(), domainName).length == 0) {

            Annotation annotationHolder = holder.createWarningAnnotation(psiElement, "Missing Translation");
            PsiElement[] psiElements = TranslationUtil.getDomainFilePsiElements(psiElement.getProject(), domainName);
            for(PsiElement psiFile: psiElements) {
                if(psiFile instanceof YAMLFile) {
                    annotationHolder.registerFix(new TranslationKeyIntentionAction((YAMLFile) psiFile, psiElement.getContents()));
                }
            }
        }

    }

    private void annotateTranslationDomain(StringLiteralExpression psiElement, @NotNull AnnotationHolder holder) {

        if(psiElement.getContents().length() > 0 && TranslationUtil.getDomainFilePsiElements(psiElement.getProject(), psiElement.getContents()).length == 0) {
            holder.createWarningAnnotation(psiElement, "Missing Translation Domain");
        }

    }

}