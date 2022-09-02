package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.lang.lexer.PhpTokenTypes;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class PhpSerializerLineMarkerProvider implements LineMarkerProvider {
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
            if (psiElement.getNode().getElementType() == PhpTokenTypes.IDENTIFIER && PhpElementsUtil.getClassNamePattern().accepts(psiElement)) {
                attachSerializerActions(lineMarkerInfos, psiElement);
            }
        }
    }

    private void attachSerializerActions(@NotNull Collection<? super LineMarkerInfo<?>> lineMarkerInfos, @NotNull PsiElement leaf) {
        PsiElement phpClassContext = leaf.getContext();
        if(!(phpClassContext instanceof PhpClass)) {
            return;
        }

        String fqn = ((PhpClass) phpClassContext).getFQN();

        Project project = leaf.getProject();
        if (SerializerUtil.hasClassTargetForSerializer(project, fqn)) {
            NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.SYMFONY_LINE_MARKER)
                .setTooltipText("Navigate to Serializer")
                .setTargets(NotNullLazyValue.lazy(() -> SerializerUtil.getClassTargetForSerializer(project, fqn)));

            lineMarkerInfos.add(builder.createLineMarkerInfo(leaf));
        }
    }
}
