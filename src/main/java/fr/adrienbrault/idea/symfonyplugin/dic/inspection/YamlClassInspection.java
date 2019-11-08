package fr.adrienbrault.idea.symfonyplugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.action.quickfix.CorrectClassNameCasingYamlLocalQuickFix;
import fr.adrienbrault.idea.symfonyplugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfonyplugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * Check if class exists
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlClassInspection extends LocalInspectionTool {

    public static final String MESSAGE_WRONG_CASING = "Wrong class casing";
    public static final String MESSAGE_MISSING_CLASS = "Missing class";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement psiElement) {
                if ((YamlElementPatternHelper.getSingleLineScalarKey("class", "factory_class").accepts(psiElement) || YamlElementPatternHelper.getParameterClassPattern().accepts(psiElement)) && YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement)) {
                    // foobar.foo:
                    //   class: Foobar\Foo
                    invoke(psiElement, holder);
                } else if (psiElement.getNode().getElementType() == YAMLTokenTypes.SCALAR_KEY && YamlElementPatternHelper.getServiceIdKeyValuePattern().accepts(psiElement.getParent())) {
                    // Foobar\Foo: ~
                    String text = PsiElementUtils.getText(psiElement);
                    if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text) && text.contains("\\")) {
                        PsiElement yamlKeyValue = psiElement.getParent();
                        if (yamlKeyValue instanceof YAMLKeyValue && YamlHelper.getYamlKeyValue((YAMLKeyValue) yamlKeyValue, "resource") == null && YamlHelper.getYamlKeyValue((YAMLKeyValue) yamlKeyValue, "exclude") == null) {
                            invoke(psiElement, holder);
                        }
                    }
                }

                super.visitElement(psiElement);
            }
        };
    }

    private void invoke(@NotNull final PsiElement psiElement, @NotNull ProblemsHolder holder) {
        String className = PsiElementUtils.getText(psiElement);

        if (YamlHelper.isValidParameterName(className)) {
            String resolvedParameter = ContainerCollectionResolver.resolveParameter(psiElement.getProject(), className);
            if (resolvedParameter != null && PhpIndex.getInstance(psiElement.getProject()).getAnyByFQN(resolvedParameter).size() > 0) {
                return;
            }
        }

        PhpClass foundClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), className);
        if (foundClass == null) {
            holder.registerProblem(psiElement, MESSAGE_MISSING_CLASS, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        } else if (!foundClass.getPresentableFQN().equals(className)) {
            holder.registerProblem(psiElement, MESSAGE_WRONG_CASING, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new CorrectClassNameCasingYamlLocalQuickFix(foundClass.getPresentableFQN()));
        }
    }
}
