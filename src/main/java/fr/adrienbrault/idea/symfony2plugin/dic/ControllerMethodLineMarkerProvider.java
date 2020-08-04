package fr.adrienbrault.idea.symfony2plugin.dic;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.util.ConstantFunction;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector;
import fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollectorParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ControllerMethodLineMarkerProvider implements LineMarkerProvider {

    private static final ExtensionPointName<ControllerActionGotoRelatedCollector> EP_NAME = new ExtensionPointName<>("fr.adrienbrault.idea.symfony2plugin.extension.ControllerActionGotoRelatedCollector");

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    @Nullable
    public LineMarkerInfo<?> collect(PsiElement psiElement) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement) || psiElement.getNode().getElementType() != PhpTokenTypes.IDENTIFIER) {
            return null;
        }

        PsiElement method = psiElement.getParent();
        if(!(method instanceof Method) || !((Method) method).getAccess().isPublic()) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = getGotoRelatedItems((Method) method);

        if(gotoRelatedItems.size() == 0) {
            return null;
        }

        // only one item dont need popover
        if(gotoRelatedItems.size() == 1) {

            GotoRelatedItem gotoRelatedItem = gotoRelatedItems.get(0);

            // hell: find any possible small icon
            Icon icon = null;
            if(gotoRelatedItem instanceof RelatedPopupGotoLineMarker.PopupGotoRelatedItem) {
                icon = ((RelatedPopupGotoLineMarker.PopupGotoRelatedItem) gotoRelatedItem).getSmallIcon();
            }

            if(icon == null) {
               icon = Symfony2Icons.SYMFONY_LINE_MARKER;
            }

            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(icon).
                setTargets(gotoRelatedItems.get(0).getElement());

            String customName = gotoRelatedItems.get(0).getCustomName();
            if(customName != null) {
                builder.setTooltipText(customName);
            }

            return builder.createLineMarkerInfo(psiElement);
        }

        return new LineMarkerInfo<>(
            psiElement,
            psiElement.getTextRange(),
            Symfony2Icons.SYMFONY_LINE_MARKER,
            6,
            new ConstantFunction<>("Related Files"),
            new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems),
            GutterIconRenderer.Alignment.RIGHT
        );
    }

    public static List<GotoRelatedItem> getGotoRelatedItems(Method method) {

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<>();

        ControllerActionGotoRelatedCollectorParameter parameter = new ControllerActionGotoRelatedCollectorParameter(method, gotoRelatedItems);
        for(ControllerActionGotoRelatedCollector extension : EP_NAME.getExtensions()) {
            extension.collectGotoRelatedItems(parameter);
        }

        return gotoRelatedItems;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> results) {

        for(PsiElement psiElement: psiElements) {
            LineMarkerInfo<?> lineMarkerInfo = collect(psiElement);
            if(lineMarkerInfo != null) {
                results.add(lineMarkerInfo);
            }
        }

    }

}
