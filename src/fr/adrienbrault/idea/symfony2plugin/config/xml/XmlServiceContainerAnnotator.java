package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.NotNull;

public class XmlServiceContainerAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement.getProject())) {
            return;
        }

        annotateServiceInstance(psiElement, holder);

    }

    private void annotateServiceInstance(@NotNull PsiElement psiElement, @NotNull AnnotationHolder holder) {

        if(!XmlHelper.getArgumentServiceIdPattern().accepts(psiElement)) {
            return;
        }

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
                PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefName);

                // check type hint on constructor
                if(phpClass != null) {
                    Method constructor = phpClass.getConstructor();
                    if(constructor != null) {
                        String serviceName = ((XmlAttributeValue) psiElement).getValue();
                        attachMethodInstances(psiElement, serviceName, constructor, getArgumentIndex(currentXmlTag), holder);
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
                        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefName);

                        // finally check method type hint
                        if(phpClass != null) {
                            Method method = PhpElementsUtil.getClassMethod(phpClass, methodName);
                            if(method != null) {
                                String serviceName = ((XmlAttributeValue) psiElement).getValue();
                                attachMethodInstances(psiElement, serviceName, method, getArgumentIndex(currentXmlTag), holder);
                            }
                        }

                    }
                }

            }
        }
    }

    private void attachMethodInstances(PsiElement target, String serviceName, Method method, int parameterIndex, @NotNull AnnotationHolder holder) {

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
        if(serviceParameterClass != null && !new Symfony2InterfacesUtil().isInstanceOf(serviceParameterClass, expectedClass)) {
            holder.createWeakWarningAnnotation(target, "Expect instance of: " + expectedClass.getPresentableFQN());
        }

    }

    private int getArgumentIndex(XmlTag xmlTag) {

        PsiElement psiElement = xmlTag;
        int index = 0;

        while (psiElement != null) {
            psiElement = psiElement.getPrevSibling();
            if(psiElement instanceof XmlTag && "argument".equalsIgnoreCase(((XmlTag) psiElement).getName())) {
                index++;
            }
        }

        return index;
    }

}
