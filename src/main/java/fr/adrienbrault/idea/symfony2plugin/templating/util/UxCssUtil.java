package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.css.util.color.CssColor;
import com.intellij.lang.css.CSSLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.css.CssElementFactory;
import com.intellij.psi.css.CssTerm;
import com.intellij.psi.css.impl.util.CssColorPsiUtil;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.ColorIcon;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Color tokens of symfony/ux-css, read from the {@code CssReference::TOKENS} constant of {@code config/reference_css.php}.
 * <p>
 * Example: <code>{{ css({ color: 'blue.500', bg: 'primary/50', _hover: { color: '[#ff6600]' } }) }}</code>
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public final class UxCssUtil {
    private static final String CSS_REFERENCE = "\\Symfony\\UX\\Css\\CssReference";
    private static final String CSS_RUNTIME_SIGNATURE = "#M#C\\Symfony\\UX\\Css\\Twig\\CssRuntime.css";

    private static final Key<CachedValue<Map<String, CssValue>>> COLORS = new Key<>("SYMFONY_UX_CSS_COLORS");

    private static final Pattern TRANSPARENT = Pattern.compile("\\btransparent\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPACITY = Pattern.compile("(.+)/(\\d{1,3})");

    private UxCssUtil() {
    }

    /**
     * @return each color token by name, e.g. {@code blue.500 => #3b82f6}
     */
    @NotNull
    public static Map<String, CssValue> getColors(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(project, COLORS, () -> CachedValueProvider.Result.create(
            collectColors(project),
            PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)
        ), false);
    }

    public static boolean isCssFunction(@Nullable TwigExtension twigExtension) {
        return twigExtension != null && CSS_RUNTIME_SIGNATURE.equalsIgnoreCase(twigExtension.getSignature());
    }

    /**
     * Removes the {@code !important} flag of a css() hash value, e.g. {@code primary} for {@code primary!}.
     */
    @NotNull
    public static String stripImportant(@NotNull String styleValue) {
        return StringUtils.removeEnd(StringUtils.removeEnd(styleValue.trim(), "!important"), "!");
    }

    /**
     * Resolves a value written in a css() hash: a color token, a color token with an opacity, or a raw value between brackets.
     * <p>
     * Examples: {@code blue.500}, {@code primary/50}, {@code [#ff6600]}, {@code primary!}
     */
    @Nullable
    public static CssValue resolveColor(@NotNull Project project, @NotNull String styleValue) {
        String value = stripImportant(styleValue);

        if (value.startsWith("[") && value.endsWith("]")) {
            String raw = value.substring(1, value.length() - 1);
            Color color = parseColor(project, raw);

            return color != null ? new CssValue(raw, color) : null;
        }

        Integer opacity = null;
        Matcher matcher = OPACITY.matcher(value);
        if (matcher.matches()) {
            value = matcher.group(1);
            opacity = Integer.parseInt(matcher.group(2));
            if (opacity > 100) {
                return null;
            }
        }

        CssValue token = getColors(project).get(value);
        Color color = token != null ? token.color() : null;
        if (color == null) {
            return null;
        }

        if (opacity == null) {
            return token;
        }

        return new CssValue(token.value(), new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(color.getAlpha() * opacity / 100f)));
    }

    /**
     * Parses a CSS color with the CSS plugin, e.g. {@code #3b82f6}, {@code rgb(0 0 0 / 0)}, {@code oklch(62.3% 0.214 259.815)} or {@code white}.
     */
    @Nullable
    public static Color parseColor(@NotNull Project project, @NotNull String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        // the CSS plugin reads no color from the transparent keyword, e.g. in color-mix(in srgb, #f00 50%, transparent)
        String css = TRANSPARENT.matcher(value.trim()).replaceAll("rgb(0 0 0 / 0)");
        CssTerm term = CssElementFactory.getInstance(project).createTerm(css, CSSLanguage.INSTANCE);
        CssColor color = CssColorPsiUtil.getColor(term);

        return color != null ? CssColor.toJavaAwtColor(color) : null;
    }

    @NotNull
    public static Icon createColorIcon(int size, @NotNull Color color) {
        return JBUIScale.scaleIcon(new ColorIcon(size, 12, color, true));
    }

    @NotNull
    private static Map<String, CssValue> collectColors(@NotNull Project project) {
        Map<String, CssValue> colors = new HashMap<>();

        for (PhpClass phpClass : PhpIndex.getInstance(project).getClassesByFQN(CSS_REFERENCE)) {
            Field field = phpClass.findOwnFieldByName("TOKENS", true);
            if (field == null || !(field.getDefaultValue() instanceof ArrayCreationExpression tokens)) {
                continue;
            }

            if (PhpElementsUtil.getArrayKeyValueMapWithValueAsPsiElement(tokens).get("colors") instanceof ArrayCreationExpression colorTokens) {
                PhpElementsUtil.getArrayKeyValueMap(colorTokens).forEach((name, value) -> colors.put(name, new CssValue(value, parseColor(project, value))));
            }
        }

        return Collections.unmodifiableMap(colors);
    }

    /**
     * @param value the CSS value, e.g. {@code #3b82f6} or {@code currentColor}
     * @param color the parsed color, or {@code null} when the value is not a color the plugin can read
     */
    public record CssValue(@NotNull String value, @Nullable Color color) {
    }
}
