package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpLineMarkerProvider implements LineMarkerProvider {
    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos) {
        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        for(PsiElement psiElement : psiElements) {
            attachRouteActions(lineMarkerInfos, psiElement);

            if(psiElement instanceof PhpFile) {
                RelatedItemLineMarkerInfo<PsiElement> lineMarker = FileResourceUtil.getFileImplementsLineMarker((PsiFile) psiElement);
                if(lineMarker != null) {
                    lineMarkerInfos.add(lineMarker);
                }
            }
        }
    }

    private void attachRouteActions(@NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement psiElement) {
        if (!(psiElement instanceof FunctionReference)) {
            return;
        }

        MethodReference methodCall = (MethodReference) psiElement;
        String controllerMethod = RouteHelper.getPhpController(methodCall);

        PsiElement[] methods = RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), controllerMethod);
        if(methods.length > 0) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
                    setTargets(methods).
                    setTooltipText("Navigate to action");

            lineMarkerInfos.add(builder.createLineMarkerInfo(methodCall.getParameters()[0].getFirstChild()));
        }
    }
}
