package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.impl.MethodImpl;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;

import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class TwigControllerLineMarkerProvider extends RelatedItemLineMarkerProvider {

    protected void collectNavigationMarkers(@NotNull PsiElement psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !(psiElement instanceof TwigFile)) {
            return;
        }

        Method method = TwigUtil.findTwigFileController((TwigFile) psiElement);
        if(method == null) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
            setTargets(method).
            setTooltipText("Navigate to controller");

        result.add(builder.createLineMarkerInfo(psiElement));

    }

}
