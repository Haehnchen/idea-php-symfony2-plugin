package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection for Twig enum() and enum_cases() functions to validate that:
 * - The provided class name exists
 * - The class is actually an enum type
 *
 * Examples:
 * {{ enum('App\\SomeEnum') }} - valid if SomeEnum is an enum
 * {{ enum('App\\NotAnEnum') }} - warning if NotAnEnum exists but is not an enum
 * {{ enum('App\\MissingClass') }} - error if class doesn't exist
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigEnumFunctionInspection extends LocalInspectionTool {

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                // Fast pre-filter: only STRING_TEXT elements can be enum/enum_cases arguments
                if (element instanceof LeafPsiElement && element.getNode() == null || element.getNode().getElementType() != TwigTokenTypes.STRING_TEXT) {
                    super.visitElement(element);
                    return;
                }

                // enum('App\Config\SomeOption')
                // enum_cases('App\Config\SomeOption')
                if (TwigPattern.getPrintBlockOrTagFunctionPattern("enum", "enum_cases").accepts(element)) {
                    visitEnumFunction(element);
                }

                super.visitElement(element);
            }

            private void visitEnumFunction(PsiElement element) {
                String contents = element.getText();
                if (StringUtils.isBlank(contents)) {
                    return;
                }

                // Unescape backslashes: 'App\\Bike\\FooEnum' => 'App\Bike\FooEnum'
                String className = contents.replace("\\\\", "\\");

                PhpClass phpClass = PhpElementsUtil.getClassInterface(element.getProject(), className);

                if (phpClass == null) {
                    // Class doesn't exist
                    holder.registerProblem(
                        element,
                        "Missing class: " + className,
                        ProblemHighlightType.WARNING
                    );
                } else if (!phpClass.isEnum()) {
                    // Class exists but is not an enum
                    holder.registerProblem(
                        element,
                        "Class '" + phpClass.getName() + "' is not an enum",
                        ProblemHighlightType.WARNING
                    );
                }
            }
        };
    }
}
