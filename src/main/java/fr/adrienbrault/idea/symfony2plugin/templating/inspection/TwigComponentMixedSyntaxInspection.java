package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
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
                PsiFile containingFile = element.getContainingFile();
                if (element.getNode().getElementType() == TwigTokenTypes.TAG_NAME
                    && "block".equals(element.getText())
                    && containingFile instanceof TwigFile
                    && TwigHtmlCompletionUtil.isInsideHtmlComponentTag(element, containingFile)) {

                    holder.registerProblem(
                        element,
                        "Cannot use Twig block syntax inside HTML component syntax. Use <twig:block name=\"...\"> instead.",
                        ProblemHighlightType.GENERIC_ERROR
                    );
                }

                super.visitElement(element);
            }
        };
    }
}
