package fr.adrienbrault.idea.symfony2plugin.translation;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ParameterBag;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.List;


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


        String keyName = psiElement.getContents();

        // should not annotate "foo$bar"
        // @TODO: regular expression to only notice translation keys and not possible text values
        if(keyName.contains("$")) {
            return;
        }

        // dont annotate non goto available keys
        if(TranslationUtil.hasTranslationKey(psiElement.getProject(), keyName, domainName)) {
            return;
        }

        // search for possible domain targets and provide translation key creation fix
        if(psiElement.getContents().length() > 0 && TranslationUtil.getTranslationPsiElements(psiElement.getProject(), psiElement.getContents(), domainName).length == 0) {

            Annotation annotationHolder = holder.createWarningAnnotation(psiElement, "Missing Translation");
            List<PsiFile> psiElements = TranslationUtil.getDomainPsiFiles(psiElement.getProject(), domainName);
            for(PsiElement psiFile: psiElements) {
                if(psiFile instanceof YAMLFile) {
                    annotationHolder.registerFix(new TranslationKeyIntentionAction((YAMLFile) psiFile, psiElement.getContents()));
                }
            }
        }

    }

    private void annotateTranslationDomain(StringLiteralExpression psiElement, @NotNull AnnotationHolder holder) {

        if(psiElement.getContents().length() > 0 && TranslationUtil.getDomainPsiFiles(psiElement.getProject(), psiElement.getContents()).size() == 0) {
            holder.createWarningAnnotation(psiElement, "Missing Translation Domain");
        }

    }

}