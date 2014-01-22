package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class TwigControllerLineMarkerProvider extends RelatedItemLineMarkerProvider {

    protected void collectNavigationMarkers(@NotNull PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return;
        }

        if(psiElement instanceof TwigFile) {
            attachController((TwigFile) psiElement, result);
            return;
        }

        if (TwigHelper.getBlockTagPattern().accepts(psiElement)) {
            this.attachBlockImplementations(psiElement, result);
        }

    }

    private void attachController(TwigFile psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {
        Method method = TwigUtil.findTwigFileController((TwigFile) psiElement);
        if(method == null) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
            setTargets(method).
            setTooltipText("Navigate to controller");

        result.add(builder.createLineMarkerInfo(psiElement));
    }

    private void attachBlockImplementations(PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        PsiElement[] blocks = TwigTemplateGoToDeclarationHandler.getBlockGoTo(psiElement);
        if(blocks.length == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.OVERRIDEN).
            setTargets(blocks).
            setTooltipText("Navigate to block");

        result.add(builder.createLineMarkerInfo(psiElement));

    }

}
