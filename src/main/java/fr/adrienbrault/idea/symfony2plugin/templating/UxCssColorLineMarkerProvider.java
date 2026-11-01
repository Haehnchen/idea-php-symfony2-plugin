package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ConstantFunction;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser.FunctionHash;
import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssUtil.CssValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Shows the color of a symfony/ux-css color token in the gutter, e.g. <code>{{ css({ color: 'blue.500' }) }}</code>.
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public class UxCssColorLineMarkerProvider extends LineMarkerProviderDescriptor {
    @Override
    public @NotNull String getName() {
        return "Symfony UX CSS color";
    }

    @Override
    public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (elements.isEmpty() || !Symfony2ProjectComponent.isEnabled(elements.getFirst())) {
            return;
        }

        Project project = elements.getFirst().getProject();
        Map<String, TwigExtension> functions = TwigExtensionParser.getFunctions(project);
        if (functions.values().stream().noneMatch(UxCssUtil::isCssFunction)) {
            return;
        }

        for (PsiElement element : elements) {
            ASTNode node = element.getNode();
            if (node == null || node.getElementType() != TwigTokenTypes.STRING_TEXT) {
                continue;
            }

            FunctionHash hash = TwigFunctionHashParser.parse(element);
            if (hash == null || hash.valueKey() == null || !UxCssUtil.isCssFunction(functions.get(hash.functionName()))) {
                continue;
            }

            CssValue color = UxCssUtil.resolveColor(project, element.getText());
            if (color == null || color.color() == null) {
                continue;
            }

            String tooltip = element.getText() + ": " + color.value();
            result.add(new LineMarkerInfo<>(
                element,
                element.getTextRange(),
                UxCssUtil.createColorIcon(12, color.color()),
                new ConstantFunction<>(tooltip),
                null,
                GutterIconRenderer.Alignment.LEFT,
                () -> tooltip
            ));
        }
    }
}
