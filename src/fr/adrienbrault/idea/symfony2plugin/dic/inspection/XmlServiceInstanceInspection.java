package fr.adrienbrault.idea.symfony2plugin.dic.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.XmlServiceSuggestIntentionAction;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlServiceInstanceInspection extends LocalInspectionTool {
    @NotNull
    public PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, boolean isOnTheFly) {
        if (!Symfony2ProjectComponent.isEnabled(holder.getProject())) {
            return super.buildVisitor(holder, isOnTheFly);
        }

        return new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement psiElement) {
                if(XmlHelper.getArgumentServiceIdPattern().accepts(psiElement)) {
                    annotateServiceInstance(psiElement, holder);
                }

                super.visitElement(psiElement);
            }
        };
    }

    private void annotateServiceInstance(@NotNull PsiElement psiElement, @NotNull ProblemsHolder holder) {
        // search for parent service definition
        XmlTag currentXmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        XmlTag parentXmlTag = PsiTreeUtil.getParentOfType(currentXmlTag, XmlTag.class);
        if(parentXmlTag == null) {
            return;
        }

        String name = parentXmlTag.getName();
        if(name.equals("service")) {
            // service/argument[id]
            XmlAttribute classAttribute = parentXmlTag.getAttribute("class");
            if(classAttribute != null) {
                String serviceDefName = classAttribute.getValue();
                if(serviceDefName != null) {
                    PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefName);

                    // check type hint on constructor
                    if(phpClass != null) {
                        Method constructor = phpClass.getConstructor();
                        if(constructor != null) {
                            String serviceName = ((XmlAttributeValue) psiElement).getValue();
                            attachMethodInstances(psiElement, serviceName, constructor, XmlHelper.getArgumentIndex(currentXmlTag), holder);
                        }
                    }
                }
            }
        } else if (name.equals("call")) {

            // service/call/argument[id]

            XmlAttribute methodAttribute = parentXmlTag.getAttribute("method");
            if(methodAttribute != null) {
                String methodName = methodAttribute.getValue();
                XmlTag serviceTag = parentXmlTag.getParentTag();

                // get service class
                if(serviceTag != null && "service".equals(serviceTag.getName())) {
                    XmlAttribute classAttribute = serviceTag.getAttribute("class");
                    if(classAttribute != null) {
                        String serviceDefName = classAttribute.getValue();
                        if(serviceDefName != null) {
                            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefName);

                            // finally check method type hint
                            if(phpClass != null) {
                                Method method = phpClass.findMethodByName(methodName);
                                if(method != null) {
                                    String serviceName = ((XmlAttributeValue) psiElement).getValue();
                                    attachMethodInstances(psiElement, serviceName, method, XmlHelper.getArgumentIndex(currentXmlTag), holder);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void attachMethodInstances(@NotNull PsiElement target, @NotNull String serviceName, @NotNull Method method, int parameterIndex, @NotNull ProblemsHolder holder) {
        Parameter[] constructorParameter = method.getParameters();
        if(parameterIndex >= constructorParameter.length) {
            return;
        }

        String className = constructorParameter[parameterIndex].getDeclaredType().toString();
        PhpClass expectedClass = PhpElementsUtil.getClassInterface(method.getProject(), className);
        if(expectedClass == null) {
            return;
        }

        PhpClass serviceParameterClass = ServiceUtil.getResolvedClassDefinition(method.getProject(), serviceName);
        if(serviceParameterClass != null && !PhpElementsUtil.isInstanceOf(serviceParameterClass, expectedClass)) {
            holder.registerProblem(
                target,
                "Expect instance of: " + expectedClass.getPresentableFQN(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                new XmlServiceSuggestIntentionAction(expectedClass.getFQN(), target)
            );
        }
    }
}
