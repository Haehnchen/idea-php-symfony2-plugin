package fr.adrienbrault.idea.symfony2plugin.templating.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

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
        Collection<String> templateNames = new HashSet<>();

        if (phpAttribute.getArguments().isEmpty()) {
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
            extracted(phpAttribute, holder, templateNames);
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

        Collection<String> templateNames = new HashSet<>();

        @Nullable String matcher = AnnotationUtil.getPropertyValueOrDefault(phpDocTag, "template");
        if (matcher != null) {
            templateNames.add(matcher);
        } else {

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
            extracted(phpDocTag, holder, templateNames);
        }
    }

    private void extracted(@NotNull PsiElement target, @NotNull ProblemsHolder holder, @NotNull Collection<String> templateNames) {
        if(templateNames.size() == 0) {
            return;
        }

        for (String templateName : templateNames) {
            if (TwigUtil.getTemplateFiles(holder.getProject(), templateName).size() > 0) {
                return;
            }
        }

        // find html target, as this this our first priority for end users condition
        String templateName = ContainerUtil.find(templateNames, s -> s.toLowerCase().endsWith(".html.twig"));

        // fallback on first item
        if(templateName == null) {
            templateName = templateNames.iterator().next();
        }

        Collection<LocalQuickFix> quickFixes = new ArrayList<>();
        quickFixes.add(new TemplateCreateByNameLocalQuickFix(templateName));

        if (StringUtils.isNotBlank(templateName)) {
            quickFixes.add(new TemplateGuessTypoQuickFix(templateName));
        }

        holder.registerProblem(target, "Twig: Missing Template", quickFixes.toArray(new LocalQuickFix[0]));
    }
}
