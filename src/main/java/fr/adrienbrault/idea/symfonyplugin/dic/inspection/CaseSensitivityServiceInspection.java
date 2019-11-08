package fr.adrienbrault.idea.symfonyplugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlAttributeValue;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfonyplugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfonyplugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfonyplugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfonyplugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfonyplugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfonyplugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class CaseSensitivityServiceInspection extends LocalInspectionTool {

    public static final String SYMFONY_LOWERCASE_LETTERS_FOR_SERVICE = "Symfony: lowercase letters for service and parameter";

    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if(!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitFile(PsiFile psiFile) {
                if(psiFile.getFileType() == PhpFileType.INSTANCE) {
                    phpVisitor(holder, psiFile);
                } else if(psiFile.getFileType() == YAMLFileType.YML) {
                    yamlVisitor(holder, psiFile);
                } else if(psiFile.getFileType() == XmlFileType.INSTANCE) {
                    xmlVisitor(holder, psiFile);
                }
            }
        };
    }

    private void yamlVisitor(final @NotNull ProblemsHolder holder, @NotNull PsiFile psiFile) {

        // usage in service arguments or every other service condition
        psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement psiElement) {

                // @TODO: support key itself
                if (YamlElementPatternHelper.getServiceDefinition().accepts(psiElement) && YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement)) {
                    // @foo, @=foo, @?foo
                    String serviceText = PsiElementUtils.trimQuote(psiElement.getText());
                    if (isValidService(serviceText)) {
                        String serviceName = YamlHelper.trimSpecialSyntaxServiceName(serviceText);

                        // dont mark "@", "@?", "@@" escaping and expressions
                        if (StringUtils.isNotBlank(serviceName) && !serviceName.equals(serviceName.toLowerCase()) && !YamlHelper.isClassServiceId(serviceName)) {
                            holder.registerProblem(psiElement, SYMFONY_LOWERCASE_LETTERS_FOR_SERVICE, ProblemHighlightType.WEAK_WARNING);
                        }
                    }
                }

                super.visitElement(psiElement);
            }
        });

        // services and parameter
        YamlHelper.processKeysAfterRoot(psiFile, yamlKeyValue -> {
            String keyText = yamlKeyValue.getKeyText();
            if(StringUtils.isNotBlank(keyText) && !keyText.equals(keyText.toLowerCase()) && !YamlHelper.isClassServiceId(keyText)) {
                PsiElement firstChild = yamlKeyValue.getFirstChild();
                if(firstChild != null) {
                    holder.registerProblem(firstChild, SYMFONY_LOWERCASE_LETTERS_FOR_SERVICE, ProblemHighlightType.WEAK_WARNING);
                }
            }

            return false;
        }, "services", "parameters");
    }

    private void phpVisitor(final @NotNull ProblemsHolder holder, @NotNull PsiFile psiFile) {

        psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                PsiElement parent = element.getParent();
                if(!(parent instanceof StringLiteralExpression)) {
                    super.visitElement(element);
                    return;
                }

                MethodReference methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element);
                if (methodReference != null && PhpElementsUtil.isMethodReferenceInstanceOf(methodReference, ServiceContainerUtil.SERVICE_GET_SIGNATURES)) {
                    String serviceName = ((StringLiteralExpression) parent).getContents();
                    if(StringUtils.isNotBlank(serviceName) && !serviceName.equals(serviceName.toLowerCase())) {
                        holder.registerProblem(element, SYMFONY_LOWERCASE_LETTERS_FOR_SERVICE, ProblemHighlightType.WEAK_WARNING);
                    }
                }

                super.visitElement(element);
            }
        });
    }

    private void xmlVisitor(final @NotNull ProblemsHolder holder, @NotNull PsiFile psiFile) {
        psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement psiElement) {
                if(psiElement instanceof XmlAttributeValue && (XmlHelper.getArgumentServiceIdPattern().accepts(psiElement) || XmlHelper.getServiceIdAttributePattern().accepts(psiElement))) {
                    String serviceName = ((XmlAttributeValue) psiElement).getValue();
                    if(StringUtils.isNotBlank(serviceName) && !serviceName.equals(serviceName.toLowerCase()) && !YamlHelper.isClassServiceId(serviceName)) {
                        holder.registerProblem(psiElement, SYMFONY_LOWERCASE_LETTERS_FOR_SERVICE, ProblemHighlightType.WEAK_WARNING);
                    }
                }

                super.visitElement(psiElement);
            }
        });
    }

    private boolean isValidService(@NotNull String serviceName) {
        if(serviceName.length() < 2 || !serviceName.startsWith("@")) {
            return false;
        }

        // expression
        return !serviceName.startsWith("@=");
    }

}
