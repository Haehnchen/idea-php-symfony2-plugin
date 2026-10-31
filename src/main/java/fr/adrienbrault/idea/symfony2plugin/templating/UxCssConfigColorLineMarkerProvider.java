package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ConstantFunction;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.ArrayHashElement;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.UxCssUtil.CssValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.YAMLScalar;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shows the color of a symfony/ux-css color token in the gutter of its configuration, in YAML or PHP.
 * <p>
 * Examples: <code>blue: { 500: '#3b82f6' }</code>, <code>primary: '{colors.blue.500}'</code>, <code>'primary' => '{colors.blue.500}'</code>
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public class UxCssConfigColorLineMarkerProvider extends LineMarkerProviderDescriptor {
    private static final Set<String> TOKEN_SECTIONS = Set.of("tokens", "semantic_tokens");
    private static final Pattern COLOR_REFERENCE = Pattern.compile("\\{colors\\.(.+)}");

    @Override
    public @NotNull String getName() {
        return "Symfony UX CSS color in config";
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

        PsiFile file = elements.getFirst().getContainingFile();
        if (file instanceof YAMLFile yamlFile) {
            collectYamlLineMarkers(yamlFile, elements, result);
        } else if (file instanceof PhpFile phpFile && StringUtil.contains(phpFile.getViewProvider().getContents(), UxCssConfigUtil.ROOT_KEY)) {
            collectPhpLineMarkers(phpFile, elements, result);
        }
    }

    private static void collectYamlLineMarkers(@NotNull YAMLFile file, @NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (UxCssConfigUtil.getYamlConfigs(file).isEmpty()) {
            return;
        }

        for (PsiElement element : elements) {
            // the marker goes on the leaf holding the value
            if (!(element.getParent() instanceof YAMLScalar scalar) || element != scalar.getFirstChild() || element.getFirstChild() != null) {
                continue;
            }

            if (scalar.getParent() instanceof YAMLKeyValue keyValue && isColorToken(getYamlConfigPath(keyValue))) {
                addLineMarker(element, scalar.getTextValue(), result);
            }
        }
    }

    private static void collectPhpLineMarkers(@NotNull PhpFile file, @NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        Set<ArrayCreationExpression> configs = new HashSet<>(UxCssConfigUtil.getPhpConfigs(file));
        if (configs.isEmpty()) {
            return;
        }

        for (PsiElement element : elements) {
            if (element.getParent() instanceof StringLiteralExpression string
                && element == string.getFirstChild()
                && element.getFirstChild() == null
                && isColorToken(getPhpConfigPath(string, configs))
            ) {
                addLineMarker(element, string.getContents(), result);
            }
        }
    }

    /**
     * @return the keys below {@code ux_css}, e.g. {@code [tokens, colors, blue, 500]}
     */
    @Nullable
    private static List<String> getYamlConfigPath(@NotNull YAMLKeyValue keyValue) {
        List<String> keys = YAMLUtil.getConfigFullNameParts(keyValue);
        if (!keys.isEmpty() && keys.getFirst().startsWith("when@")) {
            keys = keys.subList(1, keys.size());
        }

        return !keys.isEmpty() && UxCssConfigUtil.ROOT_KEY.equals(keys.getFirst()) ? keys.subList(1, keys.size()) : null;
    }

    /**
     * @return the keys between one of the {@code ux_css} config arrays and the string, e.g. {@code [tokens, colors, blue, 500]}
     */
    @Nullable
    private static List<String> getPhpConfigPath(@NotNull StringLiteralExpression string, @NotNull Set<ArrayCreationExpression> configs) {
        List<String> keys = new ArrayList<>();

        PsiElement value = string;
        while (PsiTreeUtil.getParentOfType(value, ArrayHashElement.class) instanceof ArrayHashElement hashElement && hashElement.getValue() == value) {
            PhpPsiElement key = hashElement.getKey();
            if (key == null || !(hashElement.getParent() instanceof ArrayCreationExpression array)) {
                return null;
            }

            keys.addFirst(key instanceof StringLiteralExpression keyString ? keyString.getContents() : key.getText());
            if (configs.contains(array)) {
                return keys;
            }

            value = array;
        }

        return null;
    }

    /**
     * Matches <code>tokens.colors.*</code> and <code>semantic_tokens.colors.*</code>.
     */
    private static boolean isColorToken(@Nullable List<String> keys) {
        return keys != null && keys.size() > 2 && TOKEN_SECTIONS.contains(keys.get(0)) && "colors".equals(keys.get(1));
    }

    private static void addLineMarker(@NotNull PsiElement element, @NotNull String text, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        Project project = element.getProject();
        String value = text.trim();
        Matcher reference = COLOR_REFERENCE.matcher(value);
        String token = reference.matches() ? reference.group(1) : null;

        CssValue color = token != null ? UxCssUtil.resolveColor(project, token) : parseRawColor(project, value);
        if (color == null || color.color() == null) {
            return;
        }

        String tooltip = token != null ? token + ": " + color.value() : color.value();
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

    @Nullable
    private static CssValue parseRawColor(@NotNull Project project, @NotNull String value) {
        Color color = UxCssUtil.parseColor(project, value);
        return color != null ? new CssValue(value, color) : null;
    }
}
