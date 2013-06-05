package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.jetbrains.twig.TwigFile;
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
        this.annotateRouting(element, holder);
        this.annotateAssets(element, holder);
        this.annotateTemplate(element, holder);
        this.annotateAssetsTag(element, holder);
    }

    private void annotateRouting(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {
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
        if(!TwigHelper.isTemplateFileReferenceTag(element)) {
            return;
        }

        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(element.getProject());
        if(twigFilesByName.containsKey(element.getText()))  {
            return;
        }

        holder.createWarningAnnotation(element, "Missing Template");
    }

    private void annotateAssets(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {
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

            for (final AssetFile assetFile : new AssetDirectoryReader().setFilterExtension("css", "less", "sass").setIncludeBundleDir(true).setProject(element.getProject()).getAssetFiles()) {
                if(assetFile.toString().equals(element.getText())) {
                    return;
                }
            }

            holder.createWarningAnnotation(element, "Missing asset");

            return;
        }

        if(TwigHelper.getAutocompletableAssetTag("javascripts").accepts(element)) {

            for (final AssetFile assetFile : new AssetDirectoryReader().setFilterExtension("js", "dart", "coffee").setIncludeBundleDir(true).setProject(element.getProject()).getAssetFiles()) {
                if(assetFile.toString().equals(element.getText())) {
                    return;
                }
            }

            holder.createWarningAnnotation(element, "Missing asset");
        }


    }

}