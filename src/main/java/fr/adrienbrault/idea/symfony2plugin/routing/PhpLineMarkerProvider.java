package fr.adrienbrault.idea.symfony2plugin.routing;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.UxUtil;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
            attachUxComponents(lineMarkerInfos, psiElement);

            if(psiElement instanceof PhpFile) {
                RelatedItemLineMarkerInfo<PsiElement> lineMarker = FileResourceUtil.getFileImplementsLineMarker((PsiFile) psiElement);
                if(lineMarker != null) {
                    lineMarkerInfos.add(lineMarker);
                }
            }
        }
    }

    private void attachUxComponents(@NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement leaf) {
        if (leaf.getNode().getElementType() != PhpTokenTypes.IDENTIFIER) {
            return;
        }

        if (leaf.getParent() instanceof PhpClass phpClass) {
            Collection<String> templates = new ArrayList<>();

            UxUtil.visitAsTwigComponent(phpClass, t -> {
                String template = t.getThird();
                templates.add(Objects.requireNonNullElseGet(template, () -> "components/" + t.getFirst() + ".html.twig"));
            });

            Collection<PsiFile> files = new HashSet<>();

            for (String template : templates) {
                files.addAll(TwigUtil.getTemplatePsiElements(phpClass.getProject(), template));
            }

            if (files.size() > 0) {
                NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_LINE_MARKER)
                    .setTargets(files)
                    .setTooltipText("Navigate to UX Component template");

                // leaf elements are only allowed to attach
                ASTNode nameNode = phpClass.getNameNode();
                if (nameNode != null) {
                    lineMarkerInfos.add(builder.createLineMarkerInfo(nameNode.getPsi()));
                }
            }
        }
    }

    private void attachRouteActions(@NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement leaf) {
        if (leaf.getNode().getElementType() != PhpTokenTypes.IDENTIFIER) {
            return;
        }

        PsiElement psiElement = leaf.getParent();
        if (!(psiElement instanceof MethodReference methodCall) || !"controller".equalsIgnoreCase(((MethodReference) psiElement).getName()) || !PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) psiElement, "\\Symfony\\Component\\Routing\\Loader\\Configurator\\Traits\\RouteTrait", "controller")) {
            return;
        }

        PsiElement[] methods = RouteHelper.getPhpController(methodCall);
        if(methods.length > 0) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
                    setTargets(methods).
                    setTooltipText("Navigate to action");

            // leaf elements are only allowed to attach; search "controller" psi element
            ASTNode nameNode = ((MethodReference) psiElement).getNameNode();
            if (nameNode != null) {
                lineMarkerInfos.add(builder.createLineMarkerInfo(nameNode.getPsi()));
            }
        }
    }
}
