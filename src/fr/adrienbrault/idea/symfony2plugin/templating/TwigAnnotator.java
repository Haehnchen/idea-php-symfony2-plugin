package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.indexing.FileBasedIndex;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRoutingAnnotator;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.YamlTranslationStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyIntentionAndQuickFixAction;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
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

        this.annotateTranslationKey(element, holder);
    }

    private void annotateTranslationKey(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {
        if(!TwigHelper.getTranslationPattern("trans", "transchoice").accepts(psiElement)) {
            return;
        }

        String text = psiElement.getText();
        if(StringUtils.isBlank(text)) {
            return;
        }

        // get domain on file scope or method parameter
        String domainName = TwigUtil.getPsiElementTranslationDomain(psiElement);

        // inspection will take care of complete unknown key
        if(!TranslationUtil.hasTranslationKey(psiElement.getProject(), text, domainName)) {
            return;
        }

        holder.createInfoAnnotation(psiElement, "Create translation key")
            .registerFix(new TranslationKeyIntentionAndQuickFixAction(text, domainName, new MyKeyDomainNotExistingCollector()));
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

    /**
     * Collect all domain files that are not providing the given key
     * Known VirtualFiles are filtered out based on the index
     */
    private static class MyKeyDomainNotExistingCollector implements TranslationKeyIntentionAndQuickFixAction.DomainCollector {
        @NotNull
        @Override
        public Collection<PsiFile> collect(@NotNull Project project, @NotNull String key, @NotNull String domain) {
            return TranslationUtil.getDomainPsiFiles(project, domain).stream()
                .filter(psiFile -> !isDomainAndKeyInPsi(psiFile, key, domain))
                .collect(Collectors.toList());
        }

        private boolean isDomainAndKeyInPsi(@NotNull PsiFile psiFile, @NotNull String key, @NotNull String domain) {
            List<Set<String>> values = FileBasedIndex.getInstance()
                .getValues(YamlTranslationStubIndex.KEY, domain, GlobalSearchScope.fileScope(psiFile));

            for (Set<String> value : values) {
                if(value.contains(key)) {
                    return true;
                }
            }

            return false;
        }
    }
}