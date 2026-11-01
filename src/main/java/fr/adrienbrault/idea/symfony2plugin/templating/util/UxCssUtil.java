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
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigFunctionHashParser.FunctionHash;
import fr.adrienbrault.idea.symfony2plugin.util.PhpDocArrayShapeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokens of symfony/ux-css, read from the {@code CssReference::TOKENS} constant of {@code config/reference_css.php}.
 * <p>
 * Example: <code>{{ css({ color: 'blue.500', bg: 'primary/50', _hover: { color: '[#ff6600]' } }) }}</code>
 *
 * @author Hugo Alliaume <hugo@alliau.me>
 */
public final class UxCssUtil {
    private static final String CSS_REFERENCE = "\\Symfony\\UX\\Css\\CssReference";
    private static final String CSS_RUNTIME_SIGNATURE = "#M#C\\Symfony\\UX\\Css\\Twig\\CssRuntime.css";

    private static final String COLORS_CATEGORY = "colors";

    private static final Key<CachedValue<Map<String, Map<String, CssValue>>>> TOKENS = new Key<>("SYMFONY_UX_CSS_TOKENS");

    private static final Pattern TOKEN_TYPE = Pattern.compile("([A-Z][A-Za-z]*)Token\\d*");
    private static final Pattern TRANSPARENT = Pattern.compile("\\btransparent\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPACITY = Pattern.compile("(.+)/(\\d{1,3})");

    private UxCssUtil() {
    }

    /**
     * @return the value of each token, by category and name, e.g. {@code spacing => md => 1rem}, with the parsed color of the color tokens
     */
    @NotNull
    public static Map<String, Map<String, CssValue>> getTokens(@NotNull Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(project, TOKENS, () -> CachedValueProvider.Result.create(
            collectTokens(project),
            PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE)
        ), false);
    }

    public static boolean isCssFunction(@Nullable TwigExtension twigExtension) {
        return twigExtension != null && CSS_RUNTIME_SIGNATURE.equalsIgnoreCase(twigExtension.getSignature());
    }

    /**
     * @param functions the PHP functions behind the Twig function of the hash
     * @return the value of each token the hash value accepts, e.g. {@code md => 1rem} for {@code p}, or nothing when it is not a css() hash
     */
    @NotNull
    public static Map<String, CssValue> getTokenValues(@NotNull Project project, @NotNull TwigExtension twigExtension, @NotNull Collection<Function> functions, @NotNull FunctionHash hash) {
        String category = isCssFunction(twigExtension) ? getTokenCategory(functions, hash) : null;

        return category != null ? getTokens(project).getOrDefault(category, Map.of()) : Map.of();
    }

    /**
     * Reads the token category of a css() hash value from the type of its key in {@code config/reference_css.php},
     * e.g. {@code spacing} for <code>p?: SpacingToken|...</code>.
     */
    @Nullable
    public static String getTokenCategory(@NotNull Project project, @NotNull TwigExtension twigExtension, @NotNull FunctionHash hash) {
        return getTokenCategory(TwigExtensionParser.getPhpFunctions(project, twigExtension), hash);
    }

    @Nullable
    private static String getTokenCategory(@NotNull Collection<Function> functions, @NotNull FunctionHash hash) {
        String valueKey = hash.valueKey();
        if (valueKey == null) {
            return null;
        }

        List<String> keyPath = hash.keyPath();
        for (Function function : functions) {
            String category = getTokenCategory(function, hash.argumentIndex(), keyPath, valueKey);

            // a conditional value, e.g. color: { base: '...' }
            if (category == null && !keyPath.isEmpty()) {
                category = getTokenCategory(function, hash.argumentIndex(), keyPath.subList(0, keyPath.size() - 1), keyPath.getLast());
            }

            if (category != null) {
                return category;
            }
        }

        return null;
    }

    @Nullable
    private static String getTokenCategory(@NotNull Function function, int parameterIndex, @NotNull List<String> keyPath, @NotNull String key) {
        for (String typeName : PhpDocArrayShapeUtil.getParameterValueTypeNames(function, parameterIndex, keyPath, key)) {
            Matcher matcher = TOKEN_TYPE.matcher(typeName);
            if (matcher.matches()) {
                return StringUtils.uncapitalize(matcher.group(1));
            }
        }

        return null;
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

        CssValue token = getTokens(project).getOrDefault(COLORS_CATEGORY, Map.of()).get(value);
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
    private static Map<String, Map<String, CssValue>> collectTokens(@NotNull Project project) {
        Map<String, Map<String, CssValue>> tokens = new HashMap<>();

        for (PhpClass phpClass : PhpIndex.getInstance(project).getClassesByFQN(CSS_REFERENCE)) {
            Field field = phpClass.findOwnFieldByName("TOKENS", true);
            if (field == null || !(field.getDefaultValue() instanceof ArrayCreationExpression categories)) {
                continue;
            }

            PhpElementsUtil.getArrayKeyValueMapWithValueAsPsiElement(categories).forEach((category, values) -> {
                if (values instanceof ArrayCreationExpression arrayCreationExpression) {
                    Map<String, CssValue> categoryTokens = tokens.computeIfAbsent(category, k -> new HashMap<>());
                    PhpElementsUtil.getArrayKeyValueMap(arrayCreationExpression).forEach((name, value) ->
                        categoryTokens.put(name, new CssValue(value, COLORS_CATEGORY.equals(category) ? parseColor(project, value) : null))
                    );
                }
            });
        }

        return Collections.unmodifiableMap(tokens);
    }

    /**
     * @param value the CSS value, e.g. {@code #3b82f6} or {@code currentColor}
     * @param color the parsed color, or {@code null} when the value is not a color the plugin can read
     */
    public record CssValue(@NotNull String value, @Nullable Color color) {
    }
}
