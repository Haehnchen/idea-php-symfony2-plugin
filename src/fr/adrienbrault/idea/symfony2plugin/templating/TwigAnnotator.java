package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


public class TwigAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull final PsiElement element, @NotNull AnnotationHolder holder) {
        this.annotateRouting(element, holder);

        // getAutocompletableTemplatePattern are buggy need to fix first
        //this.annotateTemplate(element, holder);
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
        if(!TwigHelper.getAutocompletableTemplatePattern().accepts(element)) {
            return;
        }

        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(element.getProject());
        if(twigFilesByName.containsKey(element.getText()))  {
            return;
        }

        holder.createWarningAnnotation(element, "Missing Template");
    }

}