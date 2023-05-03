package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * {% ta<caret>g %}
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

            @Override
            public void visitElement(@NotNull PsiElement element) {
                // {% tag %}
                if (element.getNode().getElementType() == TwigTokenTypes.TAG_NAME) {
                    visitTagTokenName(element);
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
                    // "Deprecated" highlight is not visible, so we are going here for weak warning
                    // WEAK_WARNING would be match; but not really visible

                    holder.registerProblem(
                        element.getParent(),
                        namedDeprecatedTokenParserTags.getOrDefault(tagName, "Deprecated Tag usage"),
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    );
                }
            }
        };
    }
}
