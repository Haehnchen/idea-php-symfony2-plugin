package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Function;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.intentions.ui.ServiceSuggestDialog;
import fr.adrienbrault.idea.symfony2plugin.intentions.xml.XmlServiceSuggestIntention;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

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
                            Method method = phpClass.findMethodByName(methodName);
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
        if(serviceParameterClass != null && !PhpElementsUtil.isInstanceOf(serviceParameterClass, expectedClass)) {
            holder.createWeakWarningAnnotation(target, "Expect instance of: " + expectedClass.getPresentableFQN())
                .registerFix(new MySuggestionIntentionAction(expectedClass, target));
        }

    }

    public static int getArgumentIndex(XmlTag xmlTag) {

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

    private static class MySuggestionIntentionAction extends PsiElementBaseIntentionAction {
        private final PhpClass expectedClass;
        private final PsiElement target;

        public MySuggestionIntentionAction(@NotNull PhpClass expectedClass, @NotNull PsiElement target) {
            this.expectedClass = expectedClass;
            this.target = target;
        }

        @Nls
        @NotNull
        @Override
        public String getFamilyName() {
            return "Symfony";
        }

        @Override
        public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
            Collection<ContainerService> suggestions = ServiceUtil.getServiceSuggestionForPhpClass(expectedClass, ContainerCollectionResolver.getServices(project));
            if(suggestions.size() == 0) {
                HintManager.getInstance().showErrorHint(editor, "No suggestion found");
                return;
            }

            XmlTag xmlTag = PsiTreeUtil.getParentOfType(target, XmlTag.class);
            if(xmlTag == null) {
                return;
            }

            ServiceSuggestDialog.create(ContainerUtil.map(suggestions, new Function<ContainerService, String>() {
                @Override
                public String fun(ContainerService containerService) {
                    return containerService.getName();
                }
            }), new XmlServiceSuggestIntention.MyInsertCallback(xmlTag));
        }

        @Override
        public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
            return true;
        }

        @NotNull
        @Override
        public String getText() {
            return "Symfony: Suggest Service";
        }
    }
}
