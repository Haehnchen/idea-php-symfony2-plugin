package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpAttribute;
import com.jetbrains.php.lang.psi.elements.PhpAttributesList;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import de.espend.idea.php.annotation.dict.PhpDocTagAnnotation;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpPsiAttributesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateExistsAnnotationPhpAttributeLocalInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (element instanceof PhpDocTag) {
                    annotate((PhpDocTag) element, holder);
                }

                if (element instanceof PhpAttribute) {
                    String fqn = ((PhpAttribute) element).getFQN();
                    if (fqn != null && PhpElementsUtil.isInstanceOf(element.getProject(), fqn, TwigUtil.TEMPLATE_ANNOTATION_CLASS)) {
                        annotate((PhpAttribute) element, holder);
                    }
                }

                super.visitElement(element);
            }
        };
    }

    private void annotate(@NotNull PhpAttribute phpAttribute, @NotNull ProblemsHolder holder) {
        LinkedHashSet<String> templateNames = new LinkedHashSet<>();


        boolean isEmptyTemplateAndGuess = phpAttribute.getArguments().isEmpty();
        if (isEmptyTemplateAndGuess) {
            PsiElement phpAttributesList = phpAttribute.getParent();
            if (phpAttributesList instanceof PhpAttributesList) {
                PsiElement method = phpAttributesList.getParent();
                if (method instanceof Method) {
                    templateNames.addAll(Arrays.asList(TwigUtil.getControllerMethodShortcut((Method) method)));
                }
            }
        } else {
            String attributeDefaultValue = PhpPsiAttributesUtil.getAttributeValueByNameAsStringWithDefaultParameterFallback(phpAttribute, "template");
            if (attributeDefaultValue != null) {
                templateNames.add(attributeDefaultValue);
            }
        }

        if(!templateNames.isEmpty()) {
            attachProblemForMissingTemplatesWithSuggestions(phpAttribute, holder, templateNames, isEmptyTemplateAndGuess);
        }
    }

    private void annotate(@NotNull PhpDocTag phpDocTag, @NotNull ProblemsHolder holder) {
        if(!Symfony2ProjectComponent.isEnabled(phpDocTag.getProject())) {
            return;
        }

        PhpDocTagAnnotation phpDocAnnotationContainer = AnnotationUtil.getPhpDocAnnotationContainer(phpDocTag);
        if (phpDocAnnotationContainer == null || !PhpElementsUtil.isEqualClassName(phpDocAnnotationContainer.getPhpClass(), TwigUtil.TEMPLATE_ANNOTATION_CLASS)) {
            return;
        }

        PhpPsiElement phpDocAttrList = phpDocTag.getFirstPsiChild();
        if(phpDocAttrList == null) {
            return;
        }

        LinkedHashSet<String> templateNames = new LinkedHashSet<>();

        boolean isEmptyTemplateAndGuess = false;

        @Nullable String matcher = AnnotationUtil.getPropertyValueOrDefault(phpDocTag, "template");
        if (matcher != null) {
            templateNames.add(matcher);
        } else {
            isEmptyTemplateAndGuess = true;

            // find template name on last method
            PhpDocComment docComment = PsiTreeUtil.getParentOfType(phpDocTag, PhpDocComment.class);
            if(null == docComment) {
                return;
            }

            Method method = PsiTreeUtil.getNextSiblingOfType(docComment, Method.class);
            if(null == method) {
                return;
            }

            templateNames.addAll(Arrays.asList(TwigUtil.getControllerMethodShortcut(method)));
        }

        if(!templateNames.isEmpty()) {
            attachProblemForMissingTemplatesWithSuggestions(phpDocTag, holder, templateNames, isEmptyTemplateAndGuess);
        }
    }

    private void attachProblemForMissingTemplatesWithSuggestions(@NotNull PsiElement target, @NotNull ProblemsHolder holder, @NotNull LinkedHashSet<String> templateNames, boolean isEmptyTemplateAndGuess) {
        if(templateNames.isEmpty()) {
            return;
        }

        for (String templateName : templateNames) {
            if (!TwigUtil.getTemplateFiles(holder.getProject(), templateName).isEmpty()) {
                return;
            }
        }

        // find html target, as this this our first priority for end users condition
        // or fallback on first item
        String[] templates = templateNames.stream()
            .filter(s -> s.toLowerCase().endsWith(".html.twig")).toArray(String[]::new);

        Collection<LocalQuickFix> quickFixes = new ArrayList<>();
        quickFixes.add(new TemplateCreateByNameLocalQuickFix(templates));

        if (!isEmptyTemplateAndGuess && templates.length > 0) {
            // use first as underscore is higher priority and common way by framework bundle
            quickFixes.add(new TemplateGuessTypoQuickFix(templates[0]));
        }

        holder.registerProblem(target, "Twig: Missing Template", quickFixes.toArray(new LocalQuickFix[0]));
    }
}
