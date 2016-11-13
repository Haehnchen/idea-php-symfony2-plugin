package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRoutingAnnotator;
import fr.adrienbrault.idea.symfony2plugin.templating.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyIntentionAction;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;

import java.util.List;
import java.util.Set;


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

        String text = psiElement.getText();
        if(StringUtils.isBlank(text)) {
            return;
        }

        // get domain on file scope or method parameter
        String domainName = TwigUtil.getPsiElementTranslationDomain(psiElement);

        if(!TranslationUtil.hasTranslationKey(psiElement.getProject(), text, domainName)) {
            Annotation annotationHolder = holder.createWarningAnnotation(psiElement, "Missing Translation");
            List<PsiFile> psiElements = TranslationUtil.getDomainPsiFiles(psiElement.getProject(), domainName);
            for(PsiFile psiFile: psiElements) {
                if(psiFile instanceof YAMLFile || TranslationUtil.isSupportedXlfFile(psiFile)) {
                    annotationHolder.registerFix(new TranslationKeyIntentionAction(psiFile, text));
                }
            }
        }

    }

    private void annotateTranslationDomain(@NotNull final PsiElement psiElement, @NotNull AnnotationHolder holder) {

        if(!TwigHelper.getTransDomainPattern().accepts(psiElement)) {
            return;
        }

        // @TODO: move to pattern, dont allow nested filters: eg "'form.tab.profile'|trans|desc('Interchange')"
        final PsiElement[] psiElementTrans = new PsiElement[1];
        PsiElementUtils.getPrevSiblingOnCallback(psiElement, psiElement1 -> {
            if(psiElement1.getNode().getElementType() == TwigTokenTypes.FILTER) {
                return false;
            } else {
                if(PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")).accepts(psiElement1)) {
                    psiElementTrans[0] = psiElement1;
                }
            }

            return true;
        });

        //PsiElement psiElementTrans = PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")));

        if(psiElementTrans[0] != null && TwigHelper.getTwigMethodString(psiElementTrans[0]) != null) {
            String text = psiElement.getText();
            if(StringUtils.isNotBlank(text) && !TranslationUtil.hasDomain(psiElement.getProject(), text)) {
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

        if(!(TwigHelper.getTemplateFileReferenceTagPattern().accepts(element) || TwigHelper.getPrintBlockFunctionPattern("include", "source").accepts(element)) || !TwigUtil.isValidTemplateString(element)) {
            return;
        }

        String templateName = element.getText();
        PsiElement[] psiElements = TwigHelper.getTemplatePsiElements(element.getProject(), templateName);

        if(psiElements.length > 0)  {
            return;
        }

        holder.createWarningAnnotation(element, "Missing Template");

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

        // custom assets
        if(templateName.startsWith("@") && templateName.length() > 1) {
            TwigNamedAssetsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(element.getProject(), TwigNamedAssetsServiceParser.class);
            Set<String> strings = twigPathServiceParser.getNamedAssets().keySet();
            if(strings.contains(templateName.substring(1))) {
                return true;
            }
        }

        return TwigHelper.resolveAssetsFiles(element.getProject(), templateName, fileTypes).size() > 0;
    }

}