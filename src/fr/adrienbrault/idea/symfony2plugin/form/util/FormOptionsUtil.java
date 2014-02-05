package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormExtensionServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class FormOptionsUtil {

    public static ArrayList<String> getExtendedTypeClasses(Project project, String... formTypeNames) {

        List<String> formTypeNamesList = Arrays.asList(formTypeNames);

        ArrayList<String> extendedTypeClasses = new ArrayList<String>();

        FormExtensionServiceParser formExtensionServiceParser = ServiceXmlParserFactory.getInstance(project, FormExtensionServiceParser.class);
        for(String formClass: formExtensionServiceParser.getFormExtensions().keySet()) {

            PsiElement psiElements[] = PhpElementsUtil.getPsiElementsBySignature(project, "#M#C\\" + formClass + ".getExtendedType");
            for(PsiElement psiElement: psiElements) {
                PhpReturn phpReturn = PsiTreeUtil.findChildOfType(psiElement, PhpReturn.class);
                if(phpReturn != null) {
                    PhpPsiElement returnValue = phpReturn.getFirstPsiChild();
                    if(returnValue instanceof StringLiteralExpression && formTypeNamesList.contains(((StringLiteralExpression) returnValue).getContents())) {
                        extendedTypeClasses.add(formClass);
                    }

                }
            }
        }

        return extendedTypeClasses;
    }

    public static HashMap<String, String> getFormExtensionKeys(Project project, String... formTypeNames) {
        HashMap<String, String> extensionKeys = new HashMap<String, String>();
        ArrayList<String> typeClasses = FormOptionsUtil.getExtendedTypeClasses(project, formTypeNames);

        for(String typeClass: typeClasses) {
            attachOnDefaultOptions(project, extensionKeys, typeClass);
        }

        return extensionKeys;
    }

    /**
     * finishView, buildView:
     * $this->vars
     */
    public static Set<String> getFormViewVars(Project project, String... formTypeNames) {

        Set<String> stringSet = new HashSet<String>();

        ArrayList<PhpClass> phpClasses = new ArrayList<PhpClass>();

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
                    Collection<FieldReference> fieldReferences = PsiTreeUtil.collectElementsOfType(method, FieldReference.class);
                    for(FieldReference fieldReference: fieldReferences) {
                        PsiElement psiVar = PsiElementUtils.getChildrenOfType(fieldReference, PlatformPatterns.psiElement().withText("vars"));
                        if(psiVar != null) {
                            getFormViewVarsAttachKeys(stringSet, fieldReference);
                        }

                    }
                }
            }

        }

        return stringSet;
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

    public static HashMap<String, String> getFormDefaultKeys(Project project, String formTypeName) {
        return getFormDefaultKeys(project, formTypeName, new HashMap<String, String>(), 0);
    }

    private static HashMap<String, String> getFormDefaultKeys(Project project, String formTypeName, HashMap<String, String> defaultValues, int depth) {

        PhpClass phpClass = FormUtil.getFormTypeToClass(project, formTypeName);
        if(phpClass == null) {
            return defaultValues;
        }

        String typeClass = phpClass.getPresentableFQN();
        attachOnDefaultOptions(project, defaultValues, typeClass);

        // recursive search for parent form types
        PsiElement getParent =  PhpElementsUtil.getPsiElementsBySignatureSingle(project, "#M#C\\" + phpClass.getPresentableFQN() + ".getParent");
        if(getParent != null && depth < 10) {
            PhpReturn phpReturn = PsiTreeUtil.findChildOfType(getParent, PhpReturn.class);
            if(phpReturn != null) {
                PhpPsiElement returnValue = phpReturn.getFirstPsiChild();
                if(returnValue instanceof StringLiteralExpression) {
                    getFormDefaultKeys(project, ((StringLiteralExpression) returnValue).getContents(), defaultValues, depth++);
                }

            }
        }


        return defaultValues;
    }

    private static void attachOnDefaultOptions(Project project, HashMap<String, String> defaultValues, String typeClass) {

        PsiElement setDefaultOptions =  PhpElementsUtil.getPsiElementsBySignatureSingle(project, "#M#C\\" + typeClass + ".setDefaultOptions");
        if(setDefaultOptions == null) {
            return;
        }

        Collection<MethodReference> tests = PsiTreeUtil.findChildrenOfType(setDefaultOptions, MethodReference.class);
        for(MethodReference methodReference: tests) {
            // instance check
            // methodReference.getSignature().equals("#M#C\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface.setDefaults")
            if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, "setDefaults")) {
                PsiElement[] parameters = methodReference.getParameters();
                if(parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                    for(String key: PhpElementsUtil.getArrayCreationKeys((ArrayCreationExpression) parameters[0])) {
                        defaultValues.put(key, typeClass);
                    }
                }

            }

            // support: parent::setDefaultOptions($resolver)
            // Symfony\Component\Form\Extension\Core\Type\FormType:setDefaultOptions
            if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, "setDefaultOptions") && methodReference.getReferenceType() == PhpModifier.State.PARENT) {
                PsiElement parentMethod = PhpElementsUtil.getPsiElementsBySignatureSingle(project, methodReference.getSignature());
                if(parentMethod instanceof Method) {
                    PhpClass phpClass = ((Method) parentMethod).getContainingClass();
                    if(phpClass != null) {
                        attachOnDefaultOptions(project, defaultValues, phpClass.getPresentableFQN());
                    }
                }
            }

        }
    }

}
