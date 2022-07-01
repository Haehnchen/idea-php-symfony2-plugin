package fr.adrienbrault.idea.symfony2plugin.form;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
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

        for (PsiElement psiElement : psiElements) {
            if (PhpElementsUtil.getClassNamePattern().accepts(psiElement)) {
                attachFormDataClass(lineMarkerInfos, psiElement);
            }
        }
    }

    private void attachFormDataClass(@NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement leaf) {
        PsiElement phpClassContext = leaf.getContext();
        if(!(phpClassContext instanceof PhpClass) || !PhpElementsUtil.isInstanceOf((PhpClass) phpClassContext, "\\Symfony\\Component\\Form\\FormTypeInterface")) {
            return;
        }

        PhpClass formDataClass = FormTypeReferenceContributor.getFormPhpClassFromContext(leaf);
        if (formDataClass != null) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
                .setTargets(formDataClass)
                .setTooltipText("Navigate to data class");

            lineMarkerInfos.add(builder.createLineMarkerInfo(leaf));
        }
    }
}
