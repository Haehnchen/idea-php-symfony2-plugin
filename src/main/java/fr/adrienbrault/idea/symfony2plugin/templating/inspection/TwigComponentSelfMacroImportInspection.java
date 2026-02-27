package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.ASTNode;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Detects invalid {@code {% from _self import ... %}} usage inside Symfony UX Twig Components.
 *
 * Inside a component context, {@code _self} does not refer to the current template, so macro
 * imports via {@code _self} will silently fail at runtime.
 *
 * Invalid:
 * <pre>
 * {@code
 * <twig:Alert>
 *     {% from _self import message_formatter %}   <-- ERROR
 * </twig:Alert>
 * }
 * </pre>
 *
 * Valid:
 * <pre>
 * {@code
 * <twig:Alert>
 *     {% from 'path/to/template.html.twig' import message_formatter %}
 * </twig:Alert>
 * }
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see <a href="https://symfony.com/bundles/ux-twig-component/current/index.html#using-macros-in-components">Symfony UX Twig Components - Macros</a>
 */
public class TwigComponentSelfMacroImportInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                ASTNode node = element.getNode();
                if (node == null) {
                    super.visitElement(element);
                    return;
                }

                // Match the RESERVED_ID token (_self) that appears in an import tag
                if (node.getElementType() == TwigTokenTypes.RESERVED_ID
                    && "_self".equals(element.getText())
                    && isInsideFromImportTag(element)
                    && isInsideComponentContext(element)) {

                    holder.registerProblem(
                        element,
                        "Cannot use '_self' to import macros inside a Twig component. Use the full template path instead.",
                        ProblemHighlightType.GENERIC_ERROR
                    );
                }

                super.visitElement(element);
            }

            /**
             * Returns true if this {@code _self} token is the template source in a
             * {@code {% from _self import ... %}} statement.
             */
            private boolean isInsideFromImportTag(@NotNull PsiElement element) {
                PsiElement parent = element.getParent();
                if (parent == null) {
                    return false;
                }
                ASTNode parentNode = parent.getNode();
                return parentNode != null && parentNode.getElementType() == TwigElementTypes.IMPORT_TAG;
            }

            /**
             * Returns true when the element is inside a Twig component context:
             * either an HTML-syntax component ({@code <twig:Name>}) or a
             * Twig-syntax component ({@code {% component 'Name' %}}).
             */
            private boolean isInsideComponentContext(@NotNull PsiElement element) {
                PsiFile containingFile = element.getContainingFile();
                if (!(containingFile instanceof TwigFile)) {
                    return false;
                }

                // Check for HTML component context via the HTML language view
                if (isInsideHtmlComponentTag(element, containingFile)) {
                    return true;
                }

                // Check for Twig {% component %} tag context
                return isInsideTwigComponentTag(element);
            }

            private boolean isInsideHtmlComponentTag(@NotNull PsiElement element, @NotNull PsiFile twigFile) {
                int offset = element.getTextOffset();

                PsiElement htmlElement = twigFile.getViewProvider().findElementAt(offset, HTMLLanguage.INSTANCE);
                if (htmlElement == null) {
                    htmlElement = twigFile.getViewProvider().findElementAt(Math.max(0, offset - 1), HTMLLanguage.INSTANCE);
                }
                if (htmlElement == null) {
                    return false;
                }

                XmlTag parentTag = PsiTreeUtil.getParentOfType(htmlElement, XmlTag.class);
                while (parentTag != null) {
                    if (isTwigComponentTag(parentTag)) {
                        return true;
                    }
                    parentTag = PsiTreeUtil.getParentOfType(parentTag, XmlTag.class);
                }

                return false;
            }

            /**
             * Checks if the element is inside a {@code {% component '...' %}...{% endcomponent %}} block
             * by counting occurrences of component/endcomponent tags in the text before the element.
             */
            private boolean isInsideTwigComponentTag(@NotNull PsiElement element) {
                PsiFile file = element.getContainingFile();
                if (file == null) {
                    return false;
                }

                String fileText = file.getText();
                int offset = element.getTextOffset();
                if (offset <= 0 || offset > fileText.length()) {
                    return false;
                }

                String textBefore = fileText.substring(0, offset);

                // Count open and close component tags before the element position
                int openCount = countTagOccurrences(textBefore, "component");
                int closeCount = countTagOccurrences(textBefore, "endcomponent");

                return openCount > closeCount;
            }

            private int countTagOccurrences(@NotNull String text, @NotNull String tagName) {
                int count = 0;
                int index = 0;
                // Match {%- component or {% component (optional whitespace/dash)
                String pattern = "{%";
                while ((index = text.indexOf(pattern, index)) != -1) {
                    int remaining = index + pattern.length();
                    // Skip optional whitespace and dash
                    while (remaining < text.length() && (text.charAt(remaining) == ' ' || text.charAt(remaining) == '\t' || text.charAt(remaining) == '-')) {
                        remaining++;
                    }
                    // Check if the tag name matches
                    if (text.startsWith(tagName, remaining)) {
                        int afterTag = remaining + tagName.length();
                        // Must be followed by whitespace, -, or %}
                        if (afterTag >= text.length() || Character.isWhitespace(text.charAt(afterTag))
                            || text.charAt(afterTag) == '-' || text.charAt(afterTag) == '%') {
                            count++;
                        }
                    }
                    index++;
                }
                return count;
            }

            /**
             * Returns true if the given XML tag is a {@code <twig:SomeName>} component tag
             * (but NOT {@code <twig:block>}).
             */
            private boolean isTwigComponentTag(@NotNull XmlTag tag) {
                if (TwigHtmlCompletionUtil.isTwigBlockTag(tag)) {
                    return false;
                }
                String name = tag.getName();
                return name.startsWith("twig:") || "twig".equals(tag.getNamespacePrefix());
            }
        };
    }
}
