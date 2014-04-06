package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRoutingAnnotator;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyIntentionAction;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.List;
import java.util.Map;


public class TwigAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(element.getProject())) {
            return;
        }

        if(Settings.getInstance(element.getProject()).twigAnnotateRoute) {
            this.annotateRoute(element, holder);
        }

        if(Settings.getInstance(element.getProject()).twigAnnotateAsset) {
            this.annotateAsset(element, holder);
        }

        if(Settings.getInstance(element.getProject()).twigAnnotateAssetTags) {
            this.annotateAssetsTag(element, holder);
        }

        if(Settings.getInstance(element.getProject()).twigAnnotateTemplate) {
            this.annotateTemplate(element, holder);
        }

        if(Settings.getInstance(element.getProject()).twigAnnotateTranslation) {
            this.annotateTranslationKey(element, holder);
            this.annotateTranslationDomain(element, holder);
        }


    }

    private void annotateTranslationKey(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!TwigHelper.getTranslationPattern("trans", "transchoice").accepts(psiElement)) {
            return;
        }

        String domainName = TwigUtil.getPsiElementTranslationDomain(psiElement);
        if(TranslationUtil.getTranslationPsiElements(psiElement.getProject(), psiElement.getText(), domainName).length == 0) {

            Annotation annotationHolder = holder.createWarningAnnotation(psiElement, "Missing Translation");
            List<PsiFile> psiElements = TranslationUtil.getDomainPsiFiles(psiElement.getProject(), domainName);
            for(PsiElement psiFile: psiElements) {
                if(psiFile instanceof YAMLFile) {
                    annotationHolder.registerFix(new TranslationKeyIntentionAction((YAMLFile) psiFile, psiElement.getText()));
                }
            }
        }

    }

    private void annotateTranslationDomain(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!TwigHelper.getTransDomainPattern().accepts(psiElement)) {
            return;
        }

        PsiElement psiElementTrans = PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")));
        if(psiElementTrans != null && TwigHelper.getTwigMethodString(psiElementTrans) != null) {
            if(TranslationUtil.getDomainPsiFiles(psiElement.getProject(), psiElement.getText()).size() == 0) {
                holder.createWarningAnnotation(psiElement, "Missing Translation Domain");
            }
        }

    }

    private void annotateRoute(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!TwigHelper.getAutocompletableRoutePattern().accepts(element)) {
            return;
        }

        String text = element.getText();
        if(StringUtils.isBlank(text)) {
            return;
        }

        PhpRoutingAnnotator.annotateRouteName(element, holder, text);
    }

    private void annotateTemplate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!(TwigHelper.getTemplateFileReferenceTagPattern().accepts(element) || TwigHelper.getPrintBlockFunctionPattern("include", "source").accepts(element))) {
            return;
        }

        String templateName = element.getText();
        PsiElement[] psiElements = TwigHelper.getTemplatePsiElements(element.getProject(), templateName);

        if(psiElements.length > 0)  {
            return;
        }

        holder.createWarningAnnotation(element, "Missing Template");

        int test = templateName.indexOf("Bundle:");
        if(test == -1) {
            return;
        }

        holder.createWarningAnnotation(element, "Create Template")
            .registerFix(new PhpTemplateAnnotator.CreateTemplateFix(templateName));

    }

    private void annotateAsset(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {
        if(!TwigHelper.getAutocompletableAssetPattern().accepts(element)) {
            return;
        }

        for (final AssetFile assetFile : new AssetDirectoryReader().setProject(element.getProject()).getAssetFiles()) {
            if(assetFile.toString().equals(element.getText())) {
                return;
            }
        }

        holder.createWarningAnnotation(element, "Missing asset");
    }

    private void annotateAssetsTag(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(TwigHelper.getAutocompletableAssetTag("stylesheets").accepts(element)) {

            String templateName = element.getText();
            if (isKnownAssetFileOrFolder(element, templateName, TwigHelper.CSS_FILES_EXTENSIONS)) {
                return;
            }

            holder.createWarningAnnotation(element, "Missing asset");
        }

        if(TwigHelper.getAutocompletableAssetTag("javascripts").accepts(element)) {

            String templateName = element.getText();
            if (isKnownAssetFileOrFolder(element, templateName, TwigHelper.JS_FILES_EXTENSIONS)) {
                return;
            }

            holder.createWarningAnnotation(element, "Missing asset");
        }


    }

    private boolean isKnownAssetFileOrFolder(PsiElement element, String templateName, String... fileTypes) {
        return TwigHelper.resolveAssetsFiles(element.getProject(), templateName, fileTypes).size() > 0;
    }

}