package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClassEnum;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormExtensionServiceParser;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormOption;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FormOptionsUtil {

    private static final String EXTENDED_TYPE_METHOD = "getExtendedType";
    public static final String[] FORM_OPTION_METHODS = new String[]{"setDefaultOptions", "configureOptions"};

    public static List<FormClass> getExtendedTypeClasses(Project project, String... formTypeNames) {

        List<String> formTypeNamesList = Arrays.asList(formTypeNames);

        // get compiler container services
        Set<String> stringSet = ServiceXmlParserFactory.getInstance(project, FormExtensionServiceParser.class).getFormExtensions().keySet();

        List<FormClass> extendedTypeClasses = new ArrayList<FormClass>();

        for(String formClass: stringSet) {
            visitExtendedTypeMethod(formTypeNamesList, extendedTypeClasses, false, PhpElementsUtil.getClassMethod(project, formClass, EXTENDED_TYPE_METHOD));
        }

        // use form extension interface if service is empty
        for(PhpClass phpClass: PhpIndex.getInstance(project).getAllSubclasses("\\Symfony\\Component\\Form\\FormTypeExtensionInterface")) {
            if(!phpClass.isAbstract() && !phpClass.isInterface() && !PhpUnitUtil.isTestClass(phpClass)) {
                String className = phpClass.getPresentableFQN();
                if(className != null && !stringSet.contains(className)) {
                    visitExtendedTypeMethod(formTypeNamesList, extendedTypeClasses, true, PhpElementsUtil.getClassMethod(phpClass, EXTENDED_TYPE_METHOD));
                }
            }
        }

        return extendedTypeClasses;
    }

    private static void visitExtendedTypeMethod(List<String> formTypeNamesList, List<FormClass> extendedTypeClasses, boolean isWeak, @Nullable Method method) {
        if(method == null) {
            return;
        }

        // method without class, exit we need fqn class name
        PhpClass containingClass = method.getContainingClass();
        if(containingClass == null) {
            return;
        }

        PhpReturn phpReturn = PsiTreeUtil.findChildOfType(method, PhpReturn.class);
        if(phpReturn != null) {
            PhpPsiElement returnValue = phpReturn.getFirstPsiChild();
            if(returnValue instanceof StringLiteralExpression && formTypeNamesList.contains(((StringLiteralExpression) returnValue).getContents())) {
                extendedTypeClasses.add(new FormClass(FormClassEnum.EXTENSION, containingClass, isWeak));
            }
        }

    }

    public static Map<String, FormOption> getFormExtensionKeys(Project project, String... formTypeNames) {

        List<FormClass> typeClasses = FormOptionsUtil.getExtendedTypeClasses(project, formTypeNames);
        Map<String, FormOption> extensionClassMap = new HashMap<String, FormOption>();

        for(FormClass extensionClass: typeClasses) {
            extensionClassMap.putAll(getDefaultOptions(project, extensionClass.getPhpClass(), extensionClass));
        }

        return extensionClassMap;
    }

    /**
     * finishView, buildView:
     * $this->vars
     */
    public static Set<String> getFormViewVars(Project project, String... formTypeNames) {

        Set<String> stringSet = new HashSet<String>();

        Set<String> uniqueClass = new HashSet<String>();
        List<PhpClass> phpClasses = new ArrayList<PhpClass>();

        // attach core form phpclass
        // @TODO: add formtype itself
        PhpClass coreForm = FormUtil.getFormTypeToClass(project, "form");
        if(coreForm != null) {
            phpClasses.add(coreForm);
            uniqueClass.add(coreForm.getPresentableFQN());
        }

        // for extension can also provide vars
        for(FormOption entry: FormOptionsUtil.getFormExtensionKeys(project, formTypeNames).values()) {
            PhpClass phpClass = entry.getFormClass().getPhpClass();
            if(!uniqueClass.contains(phpClass.getPresentableFQN())) {
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

    /**
     * use getDefaultOptions
     */
    @Deprecated
    private static void attachOnDefaultOptions(@NotNull Project project, @NotNull Map<String, String> defaultValues, @NotNull PhpClass phpClass) {

        for(String methodName: FORM_OPTION_METHODS) {

            Method method = PhpElementsUtil.getClassMethod(phpClass, methodName);
            if(method == null) {
                continue;
            }

            for(MethodReference methodReference: PsiTreeUtil.findChildrenOfType(method, MethodReference.class)) {

                // instance check
                // methodReference.getSignature().equals("#M#C\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface.setDefaults")
                if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, "setDefaults")) {
                    PsiElement[] parameters = methodReference.getParameters();
                    if(parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                        for(String key: PhpElementsUtil.getArrayCreationKeys((ArrayCreationExpression) parameters[0])) {
                            String presentableFQN = phpClass.getPresentableFQN();
                            if(presentableFQN != null) {
                                defaultValues.put(key, presentableFQN);
                            }
                        }
                    }
                }

                // support: parent::setDefaultOptions($resolver)
                // Symfony\Component\Form\Extension\Core\Type\FormType:setDefaultOptions
                if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, methodName) && methodReference.getReferenceType() == PhpModifier.State.PARENT) {
                    PsiElement parentMethod = PhpElementsUtil.getPsiElementsBySignatureSingle(project, methodReference.getSignature());
                    if(parentMethod instanceof Method) {
                        PhpClass phpClassInner = ((Method) parentMethod).getContainingClass();
                        if(phpClassInner != null) {
                            attachOnDefaultOptions(project, defaultValues, phpClassInner);
                        }
                    }
                }
            }

        }
    }

    @NotNull
    private static Map<String, FormOption> getDefaultOptions(@NotNull Project project, @NotNull PhpClass phpClass, @NotNull FormClass formClass) {

        Map<String, FormOption> options = new HashMap<String, FormOption>();

        for(String methodName: new String[] {"setDefaultOptions", "configureOptions"}) {

            Method method = PhpElementsUtil.getClassMethod(phpClass, methodName);
            if(method == null) {
                continue;
            }

            Method setDefaultOptionsMethod =  PhpElementsUtil.getClassMethod(phpClass, "setDefaultOptions");
            if(setDefaultOptionsMethod == null) {
                return options;
            }

            Collection<MethodReference> tests = PsiTreeUtil.findChildrenOfType(setDefaultOptionsMethod, MethodReference.class);
            for(MethodReference methodReference: tests) {
                // instance check
                // methodReference.getSignature().equals("#M#C\\Symfony\\Component\\OptionsResolver\\OptionsResolverInterface.setDefaults")
                if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, "setDefaults")) {
                    PsiElement[] parameters = methodReference.getParameters();
                    if(parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                        for(String key: PhpElementsUtil.getArrayCreationKeys((ArrayCreationExpression) parameters[0])) {
                            options.put(key, new FormOption(key, formClass));
                        }
                    }

                }

                // support: parent::setDefaultOptions($resolver)
                // Symfony\Component\Form\Extension\Core\Type\FormType:setDefaultOptions
                if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, methodName) && methodReference.getReferenceType() == PhpModifier.State.PARENT) {
                    PsiElement parentMethod = PhpElementsUtil.getPsiElementsBySignatureSingle(project, methodReference.getSignature());
                    if(parentMethod instanceof Method) {
                        PhpClass phpClassInner = ((Method) parentMethod).getContainingClass();
                        if(phpClassInner != null) {
                            // @TODO only use setDefaultOptions, recursive call get setDefaults again
                            options.putAll(getDefaultOptions(project, phpClassInner, formClass));
                        }
                    }
                }

            }

        }

        return options;
    }

    @Deprecated
    private static void attachOnDefaultOptions(Project project, Map<String, String> defaultValues, String typeClass) {
        PhpClass phpClass = PhpElementsUtil.getClass(project, typeClass);
        if(phpClass != null) {
            attachOnDefaultOptions(project, defaultValues, phpClass);
        }
    }

    /**
     * Build completion lookup element for form options
     * Reformat class name to make it more readable
     *
     * @param formOption Extension or a default option
     * @return lookup element
     */
    public static LookupElement getOptionLookupElement(FormOption formOption) {

        String typeText = formOption.getFormClass().getPhpClass().getPresentableFQN();
        if(typeText != null && typeText.lastIndexOf("\\") != -1) {
            typeText = typeText.substring(typeText.lastIndexOf("\\") + 1);
            if(typeText.endsWith("Extension")) {
                typeText = typeText.substring(0, typeText.length() - 9);
            }
        }

        return LookupElementBuilder.create(formOption.getOption())
            .withTypeText(typeText, true)
            .withIcon(formOption.getFormClass().isWeak() ? Symfony2Icons.FORM_EXTENSION_WEAK : Symfony2Icons.FORM_EXTENSION);
    }

    @NotNull
    public static Collection<PsiElement> getFormExtensionsKeysTargets(StringLiteralExpression psiElement, String... formTypes) {
        Map<String, FormOption> test = FormOptionsUtil.getFormExtensionKeys(psiElement.getProject(), formTypes);
        String value = psiElement.getContents();

        if(!test.containsKey(value)) {
            return Collections.emptyList();
        }

        // @TODO: use core method find method
        String className = test.get(value).getFormClass().getPhpClass().getPresentableFQN();

        PsiElement[] psiElements = PhpElementsUtil.getPsiElementsBySignature(psiElement.getProject(), "#M#C\\" + className + ".setDefaultOptions");
        if(psiElements.length == 0) {
            return Collections.emptyList();
        }

        PsiElement keyValue = PhpElementsUtil.findArrayKeyValueInsideReference(psiElements[0], "setDefaults", value);
        if(keyValue != null) {
            return Arrays.asList(keyValue);
        }

        return Collections.emptyList();
    }

    public static Collection<LookupElement> getFormExtensionKeysLookupElements(Project project, String... formTypes) {
        Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for(FormOption formOption: FormOptionsUtil.getFormExtensionKeys(project, formTypes).values()) {
            lookupElements.add(FormOptionsUtil.getOptionLookupElement(formOption));
        }

        return lookupElements;
    }

    @NotNull
    public static Collection<PsiElement> getDefaultOptionTargets(StringLiteralExpression element, String formType) {

        Map<String, String> defaultOptions = FormOptionsUtil.getFormDefaultKeys(element.getProject(), formType);
        String value = element.getContents();

        if(!defaultOptions.containsKey(value)) {
            return Collections.emptySet();
        }

        String className = defaultOptions.get(value);

        // @TODO: use class core
        PsiElement[] psiElements = PhpElementsUtil.getPsiElementsBySignature(element.getProject(), "#M#C\\" + className + ".setDefaultOptions");
        if(psiElements.length == 0) {
            return Collections.emptySet();
        }

        PsiElement keyValue = PhpElementsUtil.findArrayKeyValueInsideReference(psiElements[0], "setDefaults", value);
        if(keyValue != null) {
            return Arrays.asList(psiElements);
        }

        return Collections.emptySet();
    }

    public static Collection<LookupElement> getDefaultOptionLookupElements(Project project, String formType) {

        Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

        for(Map.Entry<String, String> extension: FormOptionsUtil.getFormDefaultKeys(project, formType).entrySet()) {
            String typeText = extension.getValue();
            if(typeText.lastIndexOf("\\") != -1) {
                typeText = typeText.substring(typeText.lastIndexOf("\\") + 1);
            }

            if(typeText.endsWith("Type")) {
                typeText = typeText.substring(0, typeText.length() - 4);
            }

            lookupElements.add(LookupElementBuilder.create(extension.getKey())
                .withTypeText(typeText)
                .withIcon(Symfony2Icons.FORM_OPTION)
            );
        }

        return lookupElements;
    }

}
