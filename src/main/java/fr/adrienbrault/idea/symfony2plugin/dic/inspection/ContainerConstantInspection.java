package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlText;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
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
            public void visitFile(@NotNull PsiFile psiFile) {
                if(psiFile instanceof XmlFile) {
                    xmlVisitor(holder, psiFile);
                } else if(psiFile instanceof YAMLFile) {
                    yamlVisitor(holder, psiFile);
                }
            }
        };
    }
    private void yamlVisitor(@NotNull ProblemsHolder holder, @NotNull PsiFile psiFile) {
        psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement psiElement) {
                if(psiElement instanceof YAMLScalar) {
                    String textValue = ((YAMLScalar) psiElement).getTextValue();
                    if(textValue.startsWith("!php/const:")) {
                        String constantName = textValue.substring(11);
                        if(StringUtils.isNotBlank(constantName) && ServiceContainerUtil.getTargetsForConstant(psiElement.getProject(), constantName).size() == 0) {
                            holder.registerProblem(psiElement, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                        }
                    }
                }

                super.visitElement(psiElement);
            }
        });
    }

    private void xmlVisitor(@NotNull ProblemsHolder holder, @NotNull PsiFile psiFile) {
        psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement psiElement) {
                if(!XmlHelper.getArgumentValueWithTypePattern("constant").accepts(psiElement)) {
                    super.visitElement(psiElement);
                    return;
                }

                PsiElement xmlText = psiElement.getParent();
                if(!(xmlText instanceof XmlText)) {
                    super.visitElement(psiElement);
                    return;
                }

                String value = ((XmlText) xmlText).getValue();
                if(StringUtils.isBlank(value)) {
                    super.visitElement(psiElement);
                    return;
                }

                if(ServiceContainerUtil.getTargetsForConstant(xmlText.getProject(), value).size() == 0) {
                    holder.registerProblem(xmlText, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }

                super.visitElement(psiElement);
            }
        });
    }
}
