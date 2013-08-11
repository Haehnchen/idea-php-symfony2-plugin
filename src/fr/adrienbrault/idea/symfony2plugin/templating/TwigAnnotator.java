package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.asset.dic.AssetFile;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import org.jetbrains.annotations.NotNull;

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

    }

    private void annotateRoute(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {
        if(!TwigHelper.getAutocompletableRoutePattern().accepts(element)) {
            return;
        }

        Symfony2ProjectComponent symfony2ProjectComponent = element.getProject().getComponent(Symfony2ProjectComponent.class);
        Map<String,Route> routes = symfony2ProjectComponent.getRoutes();

        if(routes.containsKey(element.getText()))  {
            return;
        }

        holder.createWarningAnnotation(element, "Missing Route");
    }

    private void annotateTemplate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {

        if(!(TwigHelper.getTemplateFileReferenceTagPattern().accepts(element) || TwigHelper.getPrintBlockFunctionPattern("include").accepts(element))) {
            return;
        }

        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(element.getProject());
        String templateName = element.getText();
        if(twigFilesByName.containsKey(templateName))  {
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
            if (isKnownAssetFileOrFolder(element, templateName, "css", "less", "sass")) {
                return;
            }

            holder.createWarningAnnotation(element, "Missing asset");
        }

        if(TwigHelper.getAutocompletableAssetTag("javascripts").accepts(element)) {

            String templateName = element.getText();
            if (isKnownAssetFileOrFolder(element, templateName, "js", "dart", "coffee")) {
                return;
            }

            holder.createWarningAnnotation(element, "Missing asset");
        }


    }

    private boolean isKnownAssetFileOrFolder(PsiElement element, String templateName, String... fileTypes) {

        if(!templateName.endsWith("*")) {
            for (final AssetFile assetFile : new AssetDirectoryReader().setFilterExtension(fileTypes).setIncludeBundleDir(true).setProject(element.getProject()).getAssetFiles()) {
                if(assetFile.toString().equals(templateName)) {
                    return true;
                }
            }

            return false;
        }

        // {% javascripts '@SampleBundle/Resources/public/js/*' %}
        // {% javascripts 'assets/js/*' %}
        String pathName = templateName.substring(0, templateName.length() - 1);
        for (final AssetFile assetFile : new AssetDirectoryReader().setFilterExtension(fileTypes).setIncludeBundleDir(true).setProject(element.getProject()).getAssetFiles()) {
            if(assetFile.toString().startsWith(pathName)) {
                return true;
            }
        }

        return false;
    }

}