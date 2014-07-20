package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormExtensionClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormExtensionServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class FormOptionsUtil {

    public static List<FormExtensionClass> getExtendedTypeClasses(Project project, String... formTypeNames) {

        List<String> formTypeNamesList = Arrays.asList(formTypeNames);

        List<FormExtensionClass> extendedTypeClasses = new ArrayList<FormExtensionClass>();

        FormExtensionServiceParser formExtensionServiceParser = ServiceXmlParserFactory.getInstance(project, FormExtensionServiceParser.class);
        Set<String> stringSet = formExtensionServiceParser.getFormExtensions().keySet();

        for(String formClass: stringSet) {

            Method method = PhpElementsUtil.getClassMethod(project, formClass, "getExtendedType");
            if(method != null) {
                PhpClass containingClass = method.getContainingClass();
                if(containingClass != null) {
                    PhpReturn phpReturn = PsiTreeUtil.findChildOfType(method, PhpReturn.class);
                    if(phpReturn != null) {
                        PhpPsiElement returnValue = phpReturn.getFirstPsiChild();
                        if(returnValue instanceof StringLiteralExpression && formTypeNamesList.contains(((StringLiteralExpression) returnValue).getContents())) {
                            extendedTypeClasses.add(new FormExtensionClass(containingClass, false));
                        }

                    }

                }
            }
        }

        // @TODO: implement this
        /* for(PhpClass phpClass: PhpIndex.getInstance(project).getAllSubclasses("\\Symfony\\Component\\Form\\FormTypeExtensionInterface")) {
            if(!phpClass.isAbstract() && !phpClass.isInterface() && !PhpUnitUtil.isTestClass(phpClass)) {

                String fqn = phpClass.getPresentableFQN();
                if(!stringSet.contains(fqn)) {
                    extendedTypeClasses.add(new FormExtensionClass(phpClass, true));
                }

            }
        } */

        return extendedTypeClasses;
    }

    public static Map<String, String> getFormExtensionKeys(Project project, String... formTypeNames) {
        Map<String, String> extensionKeys = new HashMap<String, String>();
        List<FormExtensionClass> typeClasses = FormOptionsUtil.getExtendedTypeClasses(project, formTypeNames);

        for(FormExtensionClass extensionClass: typeClasses) {
            attachOnDefaultOptions(project, extensionKeys, extensionClass.getPhpClass());
        }

        return extensionKeys;
    }

    /**
     * finishView, buildView:
     * $this->vars
     */
    public static Set<String> getFormViewVars(Project project, String... formTypeNames) {

        Set<String> stringSet = new HashSet<String>();

        List<PhpClass> phpClasses = new ArrayList<PhpClass>();

        // attach core form phpclass
        // @TODO: add formtype itself
        PhpClass coreForm = FormUtil.getFormTypeToClass(project, "form");
        if(coreForm != null) {
            phpClasses.add(coreForm);
        }

        // for extension can also provide vars
        for(Map.Entry<String, String> entry: FormOptionsUtil.getFormExtensionKeys(project, formTypeNames).entrySet()) {
            PhpClass phpClass = PhpElementsUtil.getClassInterface(project, entry.getValue());
            if(phpClass != null) {
                phpClasses.add(phpClass);
            }
        }

        for(PhpClass phpClass: phpClasses) {
            for(String stringMethod: new String[] {"finishView", "buildView"} ) {
                Method method = PhpElementsUtil.getClassMethod(phpClass, stringMethod);
                if(method != null) {

                    // self method
                    getMethodVars(stringSet, method);

                    // allow parent::
                    // @TODO: provide global util method
                    for(ClassReference classReference : PsiTreeUtil.collectElementsOfType(method, ClassReference.class)) {
                        if("parent".equals(classReference.getName())) {
                            PsiElement methodReference = classReference.getContext();
                            if(methodReference instanceof MethodReference) {
                                PsiElement parentMethod = ((MethodReference) methodReference).resolve();
                                if(parentMethod instanceof Method) {
                                    getMethodVars(stringSet, (Method) parentMethod);
                                }
                            }

                        }

                    }
                }
            }

        }

        return stringSet;
    }

    private static void getMethodVars(Set<String> stringSet, Method method) {
        Collection<FieldReference> fieldReferences = PsiTreeUtil.collectElementsOfType(method, FieldReference.class);
        for(FieldReference fieldReference: fieldReferences) {
            PsiElement psiVar = PsiElementUtils.getChildrenOfType(fieldReference, PlatformPatterns.psiElement().withText("vars"));
            if(psiVar != null) {
                getFormViewVarsAttachKeys(stringSet, fieldReference);
            }

        }
    }

    /**
     * $this->vars['test']
     * $view->vars = array_replace($view->vars, array(...));
     */
    private static void getFormViewVarsAttachKeys(Set<String> stringSet, FieldReference fieldReference) {

        // $this->vars['test']
        PsiElement context = fieldReference.getContext();
        if(context instanceof ArrayAccessExpression) {
            ArrayIndex arrayIndex = PsiTreeUtil.findChildOfType(context, ArrayIndex.class);
            if(arrayIndex != null) {
                PsiElement psiElement = arrayIndex.getFirstChild();
                if(psiElement instanceof StringLiteralExpression) {
                    String contents = ((StringLiteralExpression) psiElement).getContents();
                    if(StringUtils.isNotBlank(contents)) {
                        stringSet.add(contents);
                    }
                }
            }
        }

        // array_replace($view->vars, array(...))
        if(context instanceof ParameterList) {
            PsiElement functionReference = context.getContext();
            if(functionReference instanceof FunctionReference && "array_replace".equals(((FunctionReference) functionReference).getName())) {
                PsiElement[] psiElements = ((ParameterList) context).getParameters();
                if(psiElements.length > 1) {
                    if(psiElements[1] instanceof ArrayCreationExpression) {
                        stringSet.addAll(PhpElementsUtil.getArrayCreationKeys((ArrayCreationExpression) psiElements[1]));
                    }
                }
            }
        }
    }

    public static Map<String, String> getFormDefaultKeys(Project project, String formTypeName) {
        return getFormDefaultKeys(project, formTypeName, new HashMap<String, String>(), new FormUtil.FormTypeCollector(project).collect(), 0);
    }

    private static Map<String, String> getFormDefaultKeys(Project project, String formTypeName, HashMap<String, String> defaultValues, FormUtil.FormTypeCollector collector, int depth) {

        PhpClass phpClass = collector.getFormTypeToClass(formTypeName);
        if(phpClass == null) {
            return defaultValues;
        }

        attachOnDefaultOptions(project, defaultValues, phpClass);

        // recursive search for parent form types
        PsiElement parentMethod = PhpElementsUtil.getClassMethod(phpClass, "getParent");
        if(parentMethod != null && depth < 10) {
            PhpReturn phpReturn = PsiTreeUtil.findChildOfType(parentMethod, PhpReturn.class);
            if(phpReturn != null) {
                PhpPsiElement returnValue = phpReturn.getFirstPsiChild();
                if(returnValue instanceof StringLiteralExpression) {
                    getFormDefaultKeys(project, ((StringLiteralExpression) returnValue).getContents(), defaultValues, collector, ++depth);
                }

            }
        }

        return defaultValues;
    }

    private static void attachOnDefaultOptions(Project project, Map<String, String> defaultValues, PhpClass phpClass) {

        Method setDefaultOptionsMethod =  PhpElementsUtil.getClassMethod(phpClass, "setDefaultOptions");
        if(setDefaultOptionsMethod == null) {
            return;
        }

        Collection<MethodReference> tests = PsiTreeUtil.findChildrenOfType(setDefaultOptionsMethod, MethodReference.class);
        for(MethodReference methodReference: tests) {
            // instance check
            // methodReference.getSignature().equals("#M#C\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface.setDefaults")
            if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, "setDefaults")) {
                PsiElement[] parameters = methodReference.getParameters();
                if(parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                    for(String key: PhpElementsUtil.getArrayCreationKeys((ArrayCreationExpression) parameters[0])) {
                        defaultValues.put(key, phpClass.getPresentableFQN());
                    }
                }

            }

            // support: parent::setDefaultOptions($resolver)
            // Symfony\Component\Form\Extension\Core\Type\FormType:setDefaultOptions
            if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, "setDefaultOptions") && methodReference.getReferenceType() == PhpModifier.State.PARENT) {
                PsiElement parentMethod = PhpElementsUtil.getPsiElementsBySignatureSingle(project, methodReference.getSignature());
                if(parentMethod instanceof Method) {
                    PhpClass phpClassInner = ((Method) parentMethod).getContainingClass();
                    if(phpClass != null) {
                        attachOnDefaultOptions(project, defaultValues, phpClassInner);
                    }
                }
            }

        }
    }

    @Deprecated
    private static void attachOnDefaultOptions(Project project, Map<String, String> defaultValues, String typeClass) {
        PhpClass phpClass = PhpElementsUtil.getClass(project, typeClass);
        if(phpClass != null) {
            attachOnDefaultOptions(project, defaultValues, phpClass);
        }
    }

}
