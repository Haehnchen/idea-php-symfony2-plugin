package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * Inspection for deprecated Twig extensions (tags, filters, and functions)
 *
 * Examples:
 * {% spaceless %} - deprecated tag
 * {{ value|deprecated_filter }} - deprecated filter
 * {{ deprecated_function() }} - deprecated function
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtensionDeprecatedInspection extends LocalInspectionTool {

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            Map<String, String> namedDeprecatedTokenParserTags = null;
            Set<String> deprecatedFilters = null;
            Set<String> deprecatedFunctions = null;

            @Override
            public void visitElement(@NotNull PsiElement element) {
                // {% tag %}
                if (element.getNode().getElementType() == TwigTokenTypes.TAG_NAME) {
                    visitTagTokenName(element);
                }

                // {{ value|filter }}
                // {% apply filter %}
                if (TwigPattern.getFilterPattern().accepts(element) || TwigPattern.getApplyFilterPattern().accepts(element)) {
                    visitFilter(element);
                }

                // {{ function() }}
                if (TwigPattern.getPrintBlockFunctionPattern().accepts(element)) {
                    visitFunction(element);
                }

                super.visitElement(element);
            }

            private void visitTagTokenName(PsiElement element) {
                String tagName = element.getText();

                if (StringUtils.isBlank(tagName)) {
                    return;
                }

                // {% endspaceless % }
                if (tagName.length() > 3 && tagName.startsWith("end")) {
                    tagName = tagName.substring(3);
                }

                if (namedDeprecatedTokenParserTags == null) {
                    namedDeprecatedTokenParserTags = TwigUtil.getNamedDeprecatedTokenParserTags(element.getProject());
                }

                if (namedDeprecatedTokenParserTags.containsKey(tagName)) {
                    String descriptionTemplate = namedDeprecatedTokenParserTags.get(tagName);

                    // "Deprecated" highlight is not visible, so we are going here for weak warning
                    // WEAK_WARNING would be match; but not really visible
                    holder.registerProblem(
                        element.getParent(),
                        descriptionTemplate != null ? descriptionTemplate : "Deprecated Twig tag",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    );
                }
            }

            private void visitFilter(PsiElement element) {
                String filterName = element.getText();

                if (StringUtils.isBlank(filterName)) {
                    return;
                }

                if (deprecatedFilters == null) {
                    deprecatedFilters = TwigUtil.getDeprecatedFilters(element.getProject());
                }

                if (deprecatedFilters.contains(filterName)) {
                    holder.registerProblem(
                        element,
                        "Deprecated Twig filter",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    );
                }
            }

            private void visitFunction(PsiElement element) {
                String functionName = element.getText();

                if (StringUtils.isBlank(functionName)) {
                    return;
                }

                if (deprecatedFunctions == null) {
                    deprecatedFunctions = TwigUtil.getDeprecatedFunctions(element.getProject());
                }

                if (deprecatedFunctions.contains(functionName)) {
                    holder.registerProblem(
                        element,
                        "Deprecated Twig function",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    );
                }
            }
        };
    }
}
