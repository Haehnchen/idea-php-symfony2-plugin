package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            @Override
            public void visitElement(PsiElement element) {
                // {% tag %}
                if (element.getNode().getElementType() == TwigTokenTypes.TAG_NAME) {
                    visitTagTokenName(element);
                }

                super.visitElement(element);
            }

            private void visitTagTokenName(PsiElement element) {
                String tagName = element.getText();
                if (StringUtils.isNotBlank(tagName)) {
                    // prevent adding multiple "deprecated" message, as we have alias classes
                    final boolean[] hasDeprecated = {false};

                    TwigUtil.visitTokenParsers(holder.getProject(), triple -> {
                        if (hasDeprecated[0]) {
                            return;
                        }

                        String currentTagName = triple.getFirst();
                        if(tagName.equalsIgnoreCase(currentTagName) || (tagName.toLowerCase().startsWith("end") && currentTagName.equalsIgnoreCase(tagName.substring(3)))) {
                            PhpClass phpClass = triple.getThird().getContainingClass();
                            if (phpClass != null && phpClass.isDeprecated()) {
                                String classDeprecatedMessage = getClassDeprecatedMessage(phpClass);

                                String message = classDeprecatedMessage != null
                                    ? classDeprecatedMessage
                                    : "Deprecated Tag usage";

                                hasDeprecated[0] = true;

                                // "Deprecated" highlight is not visible, so we are going here for weak warning
                                // WEAK_WARNING would be match; but not really visible
                                holder.registerProblem(element.getParent(), message, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                            }
                        }
                    });
                }
            }

            @Nullable
            private String getClassDeprecatedMessage(@NotNull PhpClass phpClass) {
                if (phpClass.isDeprecated()) {
                    PhpDocComment docComment = phpClass.getDocComment();
                    if (docComment != null) {
                        for (PhpDocTag deprecatedTag : docComment.getTagElementsByName("@deprecated")) {
                            // deprecatedTag.getValue provides a number !?
                            String tagValue = deprecatedTag.getText();
                            if (StringUtils.isNotBlank(tagValue)) {
                                String trim = tagValue.replace("@deprecated", "").trim();

                                if (StringUtils.isNotBlank(tagValue)) {
                                    return StringUtils.abbreviate("Deprecated: " + trim, 100);
                                }
                            }
                        }
                    }
                }

                return null;
            }
        };
    }
}
