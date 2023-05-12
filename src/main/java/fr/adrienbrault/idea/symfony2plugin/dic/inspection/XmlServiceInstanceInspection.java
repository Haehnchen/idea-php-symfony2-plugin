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
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.apache.commons.lang.StringUtils;
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
            private ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector;

            @Override
            public void visitElement(@NotNull PsiElement psiElement) {
                if(XmlHelper.getArgumentServiceIdPattern().accepts(psiElement)) {
                    if (this.lazyServiceCollector == null) {
                        this.lazyServiceCollector = new ContainerCollectionResolver.LazyServiceCollector(holder.getProject());
                    }

                    annotateServiceInstance(psiElement, holder, this.lazyServiceCollector);
                }

                super.visitElement(psiElement);
            }
        };
    }

    private void annotateServiceInstance(@NotNull PsiElement psiElement, @NotNull ProblemsHolder holder, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        // search for parent service definition
        XmlTag currentXmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        XmlTag parentXmlTag = PsiTreeUtil.getParentOfType(currentXmlTag, XmlTag.class);
        if(parentXmlTag == null) {
            return;
        }

        String name = parentXmlTag.getName();
        if(name.equals("service")) {
            // service/argument[id]
            String serviceDefName = XmlHelper.getClassFromServiceDefinition(parentXmlTag);
            if(serviceDefName != null) {
                PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceDefName, lazyServiceCollector);

                // check type hint on constructor
                if(phpClass != null) {
                    Method constructor = phpClass.getConstructor();
                    if(constructor != null) {
                        String serviceName = ((XmlAttributeValue) psiElement).getValue();
                        if(StringUtils.isNotBlank(serviceName)) {
                            attachMethodInstances(psiElement, serviceName, constructor, XmlHelper.getArgumentIndex(currentXmlTag, constructor), holder, lazyServiceCollector);
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
                    String serviceDefName = XmlHelper.getClassFromServiceDefinition(serviceTag);
                    if(serviceDefName != null) {
                        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceDefName, lazyServiceCollector);

                        // finally check method type hint
                        if(phpClass != null) {
                            Method method = phpClass.findMethodByName(methodName);
                            if(method != null) {
                                String serviceName = ((XmlAttributeValue) psiElement).getValue();
                                if(StringUtils.isNotBlank(serviceName)) {
                                    attachMethodInstances(psiElement, serviceName, method, XmlHelper.getArgumentIndex(currentXmlTag, method), holder, lazyServiceCollector);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void attachMethodInstances(@NotNull PsiElement target, @NotNull String serviceName, @NotNull Method method, int parameterIndex, @NotNull ProblemsHolder holder, ContainerCollectionResolver.@NotNull LazyServiceCollector lazyServiceCollector) {
        Parameter[] constructorParameter = method.getParameters();
        if(parameterIndex >= constructorParameter.length) {
            return;
        }

        String className = constructorParameter[parameterIndex].getDeclaredType().toString();
        PhpClass expectedClass = PhpElementsUtil.getClassInterface(holder.getProject(), className);
        if(expectedClass == null) {
            return;
        }

        PhpClass serviceParameterClass = ServiceUtil.getResolvedClassDefinition(holder.getProject(), serviceName, lazyServiceCollector);
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
