package fr.adrienbrault.idea.symfony2plugin.templating.usages;

import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.patterns.ElementPattern;
import com.intellij.usages.PsiElementUsageTarget;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProviderEx;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

/**
 * Classifies Twig template usages by type (extends, include, embed, controller, component, etc.)
 * so IntelliJ's built-in UsageTypeGroupingRule shows them as named top-level groups.
 *
 * <p>The provider is entirely inert unless the Find Usages target is a {@link TwigFile},
 * so it never interferes with PHP, Java, or any other language's Find Usages grouping.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateUsageTypeProvider implements UsageTypeProviderEx {

    private static final UsageType EXTENDS    = new UsageType(() -> "extends");
    private static final UsageType INCLUDE    = new UsageType(() -> "include");
    private static final UsageType EMBED      = new UsageType(() -> "embed");
    private static final UsageType IMPORT     = new UsageType(() -> "import");
    private static final UsageType FROM       = new UsageType(() -> "from");
    private static final UsageType FORM_THEME = new UsageType(() -> "form_theme");
    private static final UsageType CONTROLLER = new UsageType(() -> "controller");
    private static final UsageType COMPONENT  = new UsageType(() -> "component");

    /** Required by {@link com.intellij.usages.impl.rules.UsageTypeProvider}; not called when the Ex variant is available. */
    @Override
    public @Nullable UsageType getUsageType(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public @Nullable UsageType getUsageType(@NotNull PsiElement element, @NotNull UsageTarget @NotNull [] targets) {
        // Only activate when doing Find Usages for a Twig template file
        if (!isTwigFileTarget(targets)) {
            return null;
        }

        // PHP controller: StringLiteralExpression / FunctionReference / method identifier
        if (element.getContainingFile() instanceof PhpFile) {
            return CONTROLLER;
        }

        // Twig extends: {% extends "..." %} or {% extends cond ? "..." : "..." %}
        // The element is the STRING_TEXT leaf inside the tag, not the TwigExtendsTag itself.
        if (hasPatternMatch(element, TwigPattern.getTemplateFileReferenceTagPattern("extends"))
            || hasPatternMatch(element, TwigPattern.getTagTernaryPattern(TwigElementTypes.EXTENDS_TAG))) {
            return EXTENDS;
        }

        // Twig component: <twig:ComponentName> XML tag
        if (element instanceof XmlTag xmlTag) {
            String name = xmlTag.getName();
            if (name != null && (name.startsWith("twig:") || "twig".equals(xmlTag.getNamespacePrefix()))) {
                return COMPONENT;
            }
        }

        // Twig component: {{ component('Name') }} / {% component Name %}
        if (hasPatternMatch(element, TwigPattern.getComponentPattern()) || isComponentTagValue(element)) {
            return COMPONENT;
        }

        // {% from ... %} and {% import ... %}
        if (hasPatternMatch(element, TwigPattern.getFromTemplateElementPattern())) {
            return FROM;
        }
        if (hasPatternMatch(element, TwigPattern.getTagNameParameterPattern(TwigElementTypes.IMPORT_TAG, "import"))) {
            return IMPORT;
        }

        // {% include ... %} / {{ include(...) }} / {{ source(...) }}
        if (hasTagName(element, "include")
            || hasPatternMatch(element, TwigPattern.getTagNameParameterPattern(TwigElementTypes.INCLUDE_TAG, "include"))
            || hasPatternMatch(element, TwigPattern.getIncludeTagArrayPattern())
            || hasPatternMatch(element, TwigPattern.getPrintBlockOrTagFunctionPattern("include", "source"))) {
            return INCLUDE;
        }

        // {% embed ... %}
        if (hasPatternMatch(element, TwigPattern.getEmbedPattern())) {
            return EMBED;
        }

        // {% form_theme form with "..." %}
        if (hasTagName(element, "form_theme")
            || hasPatternMatch(element, TwigPattern.getFormThemeFileTagPattern())) {
            return FORM_THEME;
        }

        return null;
    }

    private static boolean isTwigFileTarget(@NotNull UsageTarget @NotNull [] targets) {
        for (UsageTarget target : targets) {
            if (target instanceof PsiElementUsageTarget psiTarget
                    && psiTarget.getElement() instanceof TwigFile) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true for the component name element in {% component Name %}.
     */
    private static boolean isComponentTagValue(@NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent == null || parent.getNode() == null || parent.getNode().getElementType() != TwigElementTypes.TAG) {
            return false;
        }

        if (!hasTagName(parent, "component")) {
            return false;
        }

        PsiElement tagStart = parent.getFirstChild();
        if (tagStart == null) {
            return false;
        }

        PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(tagStart, TwigTokenTypes.TAG_NAME);
        if (tagName == null) {
            return false;
        }

        PsiElement componentValue = PsiElementUtils.getNextSiblingAndSkip(
            tagName,
            TwigTokenTypes.IDENTIFIER,
            TwigTokenTypes.STRING_TEXT,
            TwigTokenTypes.RESERVED_ID,
            TwigTokenTypes.SINGLE_QUOTE,
            TwigTokenTypes.DOUBLE_QUOTE
        );

        return componentValue == element;
    }

    private static boolean hasPatternMatch(@NotNull PsiElement element, @NotNull ElementPattern<? super PsiElement> pattern) {
        ArrayDeque<PsiElement> stack = new ArrayDeque<>();
        stack.push(element);

        while (!stack.isEmpty()) {
            PsiElement current = stack.pop();
            if (pattern.accepts(current)) {
                return true;
            }

            for (PsiElement child = current.getFirstChild(); child != null; child = child.getNextSibling()) {
                stack.push(child);
            }
        }

        return false;
    }

    private static boolean hasTagName(@NotNull PsiElement element, @NotNull String tagName) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNode() != null
                && child.getNode().getElementType() == TwigTokenTypes.TAG_NAME
                && tagName.equals(child.getText())) {
                return true;
            }
        }

        return false;
    }
}
