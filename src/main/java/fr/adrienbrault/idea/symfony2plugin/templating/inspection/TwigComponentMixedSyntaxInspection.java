package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigHtmlCompletionUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Detects mixing of Twig block syntax and HTML component syntax in Symfony UX Twig Components.
 *
 * Invalid: using {@code {% block name %}} inside {@code <twig:Component>}
 * <pre>
 * {@code
 * <twig:Card>
 *     {% block footer %}...{% endblock %}   <-- ERROR
 * </twig:Card>
 * }
 * </pre>
 *
 * Valid:
 * <pre>
 * {@code
 * <twig:Card>
 *     <twig:block name="footer">...</twig:block>
 * </twig:Card>
 * }
 * </pre>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see <a href="https://symfony.com/bundles/ux-twig-component/current/index.html#nested-components">Symfony UX Twig Components</a>
 */
public class TwigComponentMixedSyntaxInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element.getNode() == null) {
                    super.visitElement(element);
                    return;
                }

                // Detect {% block name %} inside <twig:Component>
                if (element.getNode().getElementType() == TwigTokenTypes.TAG_NAME
                    && "block".equals(element.getText())
                    && isInsideHtmlComponentContext(element)) {

                    holder.registerProblem(
                        element,
                        "Cannot use Twig block syntax inside HTML component syntax. Use <twig:block name=\"...\"> instead.",
                        ProblemHighlightType.GENERIC_ERROR
                    );
                }

                super.visitElement(element);
            }

            /**
             * Checks if the given Twig element is inside an HTML-syntax Twig component tag
             * ({@code <twig:ComponentName>...</twig:ComponentName>}) by inspecting the
             * HTML view via the ViewProvider.
             */
            private boolean isInsideHtmlComponentContext(@NotNull PsiElement element) {
                PsiFile containingFile = element.getContainingFile();
                if (!(containingFile instanceof TwigFile)) {
                    return false;
                }

                // Use the ViewProvider to find the corresponding element in the HTML language view
                int offset = element.getTextOffset();
                PsiElement htmlElement = containingFile.getViewProvider().findElementAt(offset, HTMLLanguage.INSTANCE);
                if (htmlElement == null) {
                    // Try one character back in case caret is at boundary
                    htmlElement = containingFile.getViewProvider().findElementAt(Math.max(0, offset - 1), HTMLLanguage.INSTANCE);
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
