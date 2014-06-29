package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpTypedElementImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.XmlServiceParser;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class FormUtil {

    final public static String ABSTRACT_FORM_INTERFACE = "\\Symfony\\Component\\Form\\FormTypeInterface";

    @Nullable
    public static PhpClass getFormTypeToClass(Project project,@Nullable String formType) {

        if(formType == null) {
            return null;
        }

        // formtype can also be a direct class name
        if(formType.contains("\\")) {
            PhpClass phpClass = PhpElementsUtil.getClass(PhpIndex.getInstance(project), formType);
            if(phpClass != null) {
                return phpClass;
            }
        }

        // find on registered formtype aliases
        FormTypeServiceParser formTypeServiceParser = ServiceXmlParserFactory.getInstance(project, FormTypeServiceParser.class);
        String serviceName = formTypeServiceParser.getFormTypeMap().getServiceName(formType);
        if(serviceName == null) {
            return null;
        }

        String serviceClass = ServiceXmlParserFactory.getInstance(project, XmlServiceParser.class).getServiceMap().getMap().get(serviceName);
        if (null == serviceClass) {
            return null;
        }

        List<ResolveResult> resolveResults = PhpElementsUtil.getClassInterfaceResolveResult(project, serviceClass);
        if(resolveResults.size() == 0) {
            return null;
        }

        PsiElement psiElement = resolveResults.iterator().next().getElement();

        if(psiElement instanceof PhpClass) {
           return (PhpClass) resolveResults.iterator().next().getElement();
        }

        return null;

    }

    public static MethodReference[] getFormBuilderTypes(Method method) {

        final ArrayList<MethodReference> methodReferences = new ArrayList<MethodReference>();

        final Symfony2InterfacesUtil symfony2InterfacesUtil = new Symfony2InterfacesUtil();
        PsiTreeUtil.collectElements(method, new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement psiElement) {

                if (psiElement instanceof MethodReference) {
                    String methodName = ((MethodReference) psiElement).getName();
                    if (methodName != null && (methodName.equals("add") || methodName.equals("create"))) {
                        if(symfony2InterfacesUtil.isFormBuilderFormTypeCall(psiElement)) {
                            methodReferences.add((MethodReference) psiElement);
                            return true;
                        }
                    }
                }

                return false;
            }
        });

        return methodReferences.toArray(new MethodReference[methodReferences.size()]);

    }

    /**
     * $form->get ..
     */
    @Nullable
    public static PhpClass resolveFormGetterCall(MethodReference methodReference) {

        // "$form"->get('field_name');
        PhpPsiElement variable = methodReference.getFirstPsiChild();
        if(!(variable instanceof Variable)) {
            return null;
        }

        // find "$form = $this->createForm" createView call
        PsiElement variableDecl = ((Variable) variable).resolve();
        if(variableDecl == null) {
            return null;
        }

        // $form = "$this->createForm(new Type(), $entity)";
        PsiElement assignmentExpression = variableDecl.getParent();
        if(!(assignmentExpression instanceof AssignmentExpression)) {
            return null;
        }

        // $form = "$this->"createForm(new Type(), $entity)";
        PhpPsiElement calledMethodReference = ((AssignmentExpression) assignmentExpression).getValue();
        if(!(calledMethodReference instanceof MethodReference)) {
            return null;
        }

        return getFormTypeClass((MethodReference) calledMethodReference);

    }

    public static PhpClass getFormTypeClass(@Nullable MethodReference calledMethodReference) {

        if(calledMethodReference == null) {
            return null;
        }

        if(new Symfony2InterfacesUtil().isCallTo(calledMethodReference, "\\Symfony\\Component\\Form\\FormFactory", "create")) {
            return null;
        }

        // $form = "$this->createForm("new Type()", $entity)";
        PsiElement formType = PsiElementUtils.getMethodParameterPsiElementAt(calledMethodReference, 0);

        return getFormTypeClassOnParameter(formType);
    }

    /**
     * Get form builder field for
     * $form->get('field');
     */
    @Nullable
    public static Method resolveFormGetterCallMethod(MethodReference methodReference) {
        PhpClass formPhpClass = FormUtil.resolveFormGetterCall(methodReference);
        if(formPhpClass == null) {
            return null;
        }

        Method method = PhpElementsUtil.getClassMethod(formPhpClass, "buildForm");
        if(method == null) {
            return null;
        }

        return method;
    }

    /**
     * Get form builder field for
     * $form->get('field', 'file');
     * $form->get('field', new FileType());
     */
    @Nullable
    public static PhpClass getFormTypeClassOnParameter(PsiElement psiElement) {

        if(psiElement instanceof StringLiteralExpression) {
            return getFormTypeToClass(psiElement.getProject(), ((StringLiteralExpression) psiElement).getContents());
        }

        if(psiElement instanceof PhpTypedElementImpl) {
            String typeName = ((PhpTypedElementImpl) psiElement).getType().toString();
            return getFormTypeToClass(psiElement.getProject(), typeName);
        }

        return null;
    }

    public static Set<String> getFormAliases(@NotNull PhpClass phpClass) {
        final Set<String> aliases = new HashSet<String>();

        if(!new Symfony2InterfacesUtil().isInstanceOf(phpClass, ABSTRACT_FORM_INTERFACE)) {
            return aliases;
        }

        Method method = PhpElementsUtil.getClassMethod(phpClass, "getName");
        if(method != null) {
            method.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if(PhpElementsUtil.getMethodReturnPattern().accepts(element)) {
                        String value = PhpElementsUtil.getStringValue(element);
                        if(value != null && StringUtils.isNotBlank(value)) {
                            aliases.add(value);
                        }
                    }
                    super.visitElement(element);
                }
            });
        }

        return aliases;

    }

    public static void attachFormAliasesCompletions(@NotNull PhpClass phpClass, @NotNull CompletionResultSet completionResultSet) {
        for(String alias: getFormAliases(phpClass)) {
            completionResultSet.addElement(LookupElementBuilder.create(alias).withIcon(Symfony2Icons.FORM_TYPE).withTypeText(phpClass.getPresentableFQN(), true));
        }
    }

}
