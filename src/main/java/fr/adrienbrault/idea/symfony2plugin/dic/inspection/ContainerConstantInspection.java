package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.Language;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.xml.XmlText;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLScalar;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ContainerConstantInspection extends LocalInspectionTool {

    public static final String MESSAGE = "Symfony: constant not found";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                Language language = element.getLanguage();

                if (language == YAMLLanguage.INSTANCE) {
                    if (element instanceof YAMLScalar yamlScalar) {
                        visitYamlElement(yamlScalar, holder);
                    }
                } else if(language == XMLLanguage.INSTANCE) {
                    visitXmlElement(element, holder);
                }

                super.visitElement(element);
            }
        };
    }
    private void visitYamlElement(@NotNull YAMLScalar psiElement, @NotNull ProblemsHolder holder) {
        String textValue = ((YAMLScalar) psiElement).getTextValue();
        if(textValue.startsWith("!php/const:")) {
            String constantName = textValue.substring(11);
            if(StringUtils.isNotBlank(constantName) && ServiceContainerUtil.getTargetsForConstant(psiElement.getProject(), constantName).size() == 0) {
                holder.registerProblem(psiElement, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
            }
        }
    }

    private void visitXmlElement(@NotNull PsiElement psiElement, @NotNull ProblemsHolder holder) {
        if(!XmlHelper.getArgumentValueWithTypePattern("constant").accepts(psiElement)) {
            return;
        }

        PsiElement xmlText = psiElement.getParent();
        if(!(xmlText instanceof XmlText)) {
            return;
        }

        String value = ((XmlText) xmlText).getValue();
        if(StringUtils.isBlank(value)) {
            return;
        }

        if(ServiceContainerUtil.getTargetsForConstant(xmlText.getProject(), value).size() == 0) {
            holder.registerProblem(xmlText, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
        }
    }
}
