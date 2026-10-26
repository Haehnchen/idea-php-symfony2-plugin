package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.form.dict.*;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionLookupVisitor;
import fr.adrienbrault.idea.symfony2plugin.form.visitor.FormOptionVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpAttributeIndex.PhpAttributeIndexer.AS_FORM_TYPE_ATTRIBUTE;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormOptionsUtil {
    /**
     * Symfony2 / 3 Form setting options per FormType via method
     */
    public static final String[] FORM_OPTION_METHODS = {
        "setDefaultOptions", "configureOptions"
    };

    /**
     * Methods which provides possible form options on first parameter
     */
    private static final String[] OPTIONS_VIA_METHOD_PARAMETER = {
        "setRequired", "setOptional", "setDefined", "setDefault",
        "setAllowedValues", "addAllowedValues", "setAllowedTypes", "addAllowedTypes"
    };

    /**
     * Find form extensions extends given form type
     *
     * @param formTypeNames Fqn class "Foo\Foo", "\Foo\Foo" or string value "foo";
     *                      should have all parents a FormType can have
     */
    @NotNull
    public static Collection<FormClass> getExtendedTypeClasses(@NotNull Project project, @NotNull String... formTypeNames) {

        // strip "\"
        List<String> formTypeNamesList = ContainerUtil.map(Arrays.asList(formTypeNames), s ->
            StringUtils.stripStart(s, "\\")
        );

        Collection<FormClass> extendedTypeClasses = new ArrayList<>();
        for(PhpClass phpClass: getFormTypeExtensionClassNames(project)) {
            for (String formExtendedType : FormUtil.getFormExtendedType(phpClass)) {
                if(formExtendedType != null && formTypeNamesList.contains(formExtendedType)) {
                    extendedTypeClasses.add(new FormClass(FormClassEnum.EXTENSION, phpClass, true));
                }
            }
        }

        return extendedTypeClasses;
    }

    @NotNull
    private static Set<PhpClass> getFormTypeExtensionClassNames(@NotNull Project project) {

        Set<PhpClass> phpClasses = new HashSet<>();

        // @TODO: should be same as interface?
        for (String s : ServiceXmlParserFactory.getInstance(project, FormExtensionServiceParser.class).getFormExtensions().keySet()) {
            ContainerUtil.addIfNotNull(
                phpClasses,
                PhpElementsUtil.getClass(project, s)
            );
        }

        for(PhpClass phpClass: PhpIndexUtil.getAllSubclasses(project, FormUtil.FORM_EXTENSION_INTERFACE)) {
            if(!FormUtil.isValidFormPhpClass(phpClass)) {
                continue;
            }

            phpClasses.add(phpClass);
        }

        return phpClasses;
    }

    @NotNull
    public static Map<String, FormOption> getFormExtensionKeys(@NotNull Project project, @NotNull String... formTypeNames) {

        Collection<FormClass> typeClasses = FormOptionsUtil.getExtendedTypeClasses(project, formTypeNames);
        Map<String, FormOption> extensionClassMap = new HashMap<>();

        for(FormClass extensionClass: typeClasses) {
            extensionClassMap.putAll(getDefaultOptions(extensionClass.getPhpClass(), extensionClass));
        }

        return extensionClassMap;
    }

    /**
     * finishView, buildView:
     * $this->vars
     */
    public static Set<String> getFormViewVars(Project project, String... formTypeNames) {

        Set<String> stringSet = new HashSet<>();

        Set<String> uniqueClass = new HashSet<>();
        List<PhpClass> phpClasses = new ArrayList<>();

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
                Method method = phpClass.findMethodByName(stringMethod);
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

    public static void visitFormOptions(@NotNull Project project, @NotNull String formTypeName, @NotNull FormOptionVisitor visitor) {
        visitFormOptions(formTypeName, new FormUtil.FormTypeCollector(project).collect(), 0, true, visitor);
    }

    private static void visitFormOptions(@NotNull String formTypeName, @NotNull FormUtil.FormTypeCollector collector, int depth, boolean withExtensions, @NotNull FormOptionVisitor visitor) {
        PhpClass phpClass = collector.getFormTypeToClass(formTypeName);
        if (phpClass == null || depth > 10) {
            return;
        }

        FormTypeClass formType = collector.getFormTypesMap().get(StringUtils.stripStart(phpClass.getFQN(), "\\"));
        if (formType != null && formType.getType() == FormTypeClass.Type.ATTRIBUTE) {
            visitAttributeFormOptions(phpClass, collector, depth, withExtensions, visitor);
        } else {
            visitPlainFormOptions(phpClass, formTypeName, collector, depth, withExtensions, visitor);
        }
    }

    private static void visitPlainFormOptions(@NotNull PhpClass phpClass, @NotNull String formTypeName, @NotNull FormUtil.FormTypeCollector collector, int depth, boolean withExtensions, @NotNull FormOptionVisitor visitor) {
        getDefaultOptions(phpClass, new FormClass(FormClassEnum.FORM_TYPE, phpClass, false), visitor);
        if (withExtensions) {
            visitExtensionOptions(phpClass.getProject(), formTypeName, visitor);
        }

        // recursive search for parent form types
        for (String formParent : FormUtil.getFormParentOfPhpClass(phpClass)) {
            visitFormOptions(formParent, collector, depth + 1, withExtensions, visitor);
        }
    }

    private static void visitAttributeFormOptions(@NotNull PhpClass phpClass, @NotNull FormUtil.FormTypeCollector collector, int depth, boolean withExtensions, @NotNull FormOptionVisitor visitor) {
        FormClass formClass = new FormClass(FormClassEnum.FORM_TYPE, phpClass, false);
        visitor.visit(phpClass, "data_class", formClass, FormOptionEnum.DEFAULT);
        ArrayCreationExpression defaults = FormAttributeUtil.getDefaultOptions(phpClass);
        if (defaults != null) {
            for (Map.Entry<String, PsiElement> entry : PhpElementsUtil.getArrayCreationKeyMap(defaults).entrySet()) {
                if (!"data_class".equals(entry.getKey())) {
                    visitor.visit(entry.getValue(), entry.getKey(), formClass, FormOptionEnum.DEFAULT);
                }
            }
        }

        if (withExtensions) {
            visitExtensionOptions(phpClass.getProject(), phpClass.getFQN(), visitor);
        }

        PhpClass parent = FormAttributeUtil.getParentFormType(phpClass);
        visitFormOptions(parent != null ? parent.getFQN() : FormAttributeUtil.DEFAULT_FORM_TYPE, collector, depth + 1, withExtensions, visitor);
    }

    private static void visitExtensionOptions(@NotNull Project project, @NotNull String formTypeName, @NotNull FormOptionVisitor visitor) {
        for (FormClass formClass : getExtendedTypeClasses(project, formTypeName)) {
            getDefaultOptions(formClass.getPhpClass(), formClass, visitor);
        }
    }

    private static void getFormDefaultKeys(@NotNull Project project, @NotNull String formTypeName, @NotNull FormOptionVisitor visitor) {
        visitFormOptions(formTypeName, new FormUtil.FormTypeCollector(project).collect(), 0, false, visitor);
    }

    @NotNull
    private static Map<String, FormOption> getDefaultOptions(@NotNull PhpClass phpClass, @NotNull FormClass formClass) {
        final Map<String, FormOption> options = new HashMap<>();

        getDefaultOptions(phpClass, formClass, (psiElement, option, formClass1, optionEnum) -> {
            // append REQUIRED, if we already know this value
            if(options.containsKey(option)) {
                FormOption formOption = options.get(option);

                formOption.addOptionEnum(optionEnum);
                formOption.addTarget(psiElement);
            } else {
                options.put(option, new FormOption(option, formClass1, optionEnum, psiElement));
            }
        });

        return options;
    }

    private static void getDefaultOptions(@NotNull PhpClass phpClass, @NotNull FormClass formClass, @NotNull FormOptionVisitor visitor) {
        getDefaultOptions(phpClass, formClass, visitor, 10);
    }

    private static void getDefaultOptions(@NotNull PhpClass phpClass, @NotNull FormClass formClass, @NotNull FormOptionVisitor visitor, int depth) {
        if(depth-- < 0) {
            return;
        }

        for(String methodName: FORM_OPTION_METHODS) {
            Method method = phpClass.findMethodByName(methodName);
            if(method == null) {
                continue;
            }

            Collection<MethodReference> tests = PsiTreeUtil.findChildrenOfType(method, MethodReference.class);
            for(MethodReference methodReference: tests) {
                if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, "setDefaults")) {
                    PsiElement[] parameters = methodReference.getParameters();
                    if(parameters.length > 0 && parameters[0] instanceof ArrayCreationExpression) {
                        for(Map.Entry<String, PsiElement> entry: PhpElementsUtil.getArrayCreationKeyMap((ArrayCreationExpression) parameters[0]).entrySet()) {
                            visitor.visit(entry.getValue(), entry.getKey(), formClass, FormOptionEnum.DEFAULT);
                        }
                    }

                } else {
                    // ->setRequired(['test', 'test2'])
                    // ->setRequired('test')
                    for(String currentMethod: OPTIONS_VIA_METHOD_PARAMETER) {
                        if(!PhpElementsUtil.isEqualMethodReferenceName(methodReference, currentMethod)) {
                            continue;
                        }

                        PsiElement[] parameters = methodReference.getParameters();
                        if(parameters.length == 0) {
                            continue;
                        }

                        if(parameters[0] instanceof ArrayCreationExpression) {
                            // ->setRequired(['test', 'test2'])
                            for (Map.Entry<String, PsiElement> entry : PhpElementsUtil.getArrayValuesAsMap((ArrayCreationExpression) parameters[0]).entrySet()) {
                                visitor.visit(entry.getValue(), entry.getKey(), formClass, FormOptionEnum.getEnum(currentMethod));
                            }
                        } else {
                            // ->setRequired('test')
                            String stringValue = PhpElementsUtil.getStringValue(parameters[0]);
                            if(stringValue != null) {
                                visitor.visit(parameters[0], stringValue, formClass, FormOptionEnum.getEnum(currentMethod));
                            }
                        }
                    }
                }

                Project project = phpClass.getProject();

                // support: parent::setDefaultOptions($resolver)
                // Symfony\Component\Form\Extension\Core\Type\FormType:setDefaultOptions
                if(PhpElementsUtil.isEqualMethodReferenceName(methodReference, methodName) && methodReference.getReferenceType() == PhpModifier.State.PARENT) {
                    PsiElement parentMethod = PhpElementsUtil.getPsiElementsBySignatureSingle(project, methodReference.getSignature());
                    if(parentMethod instanceof Method) {
                        PhpClass phpClassInner = ((Method) parentMethod).getContainingClass();
                        if(phpClassInner != null) {
                            // @TODO only use setDefaultOptions, recursive call get setDefaults again
                            getDefaultOptions(phpClassInner, formClass, visitor, depth);
                        }
                    }
                }
            }
        }
    }

    /**
     * Build completion lookup element for form options
     * Reformat class name to make it more readable
     *
     * @param formOption Extension or a default option
     * @return lookup element
     */
    private static LookupElement getOptionLookupElement(FormOption formOption) {
        String typeText = formOption.getFormClass().getPhpClass().getPresentableFQN();

        if(typeText.lastIndexOf("\\") != -1) {
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
    public static Collection<PsiElement> getFormExtensionsKeysTargets(@NotNull StringLiteralExpression psiElement, String... formTypes) {
        Map<String, FormOption> extensionKeys = FormOptionsUtil.getFormExtensionKeys(psiElement.getProject(), formTypes);
        String optionName = psiElement.getContents();

        if(StringUtils.isBlank(optionName) || !extensionKeys.containsKey(optionName)) {
            return Collections.emptyList();
        }

        return extensionKeys.get(optionName).getPsiTargets();
    }

    public static Collection<LookupElement> getFormExtensionKeysLookupElements(Project project, String... formTypes) {
        return FormOptionsUtil.getFormExtensionKeys(project, formTypes).values().stream()
            .map(FormOptionsUtil::getOptionLookupElement)
            .collect(Collectors.toCollection(ArrayList::new));
    }

    @NotNull
    public static Collection<PsiElement> getFormDefaultOptionTargets(@NotNull StringLiteralExpression element, @NotNull String formType) {
        final String value = element.getContents();
        if(StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }

        final Collection<PsiElement> psiElements = new ArrayList<>();

        FormOptionsUtil.getFormDefaultKeys(element.getProject(), formType, (psiElement, option, formClass, optionEnum) -> {
            if(option.equals(value)) {
                psiElements.add(psiElement);
            }
        });

        return psiElements;
    }

    @Deprecated
    @NotNull
    public static Collection<PsiElement> getDefaultOptionTargets(@NotNull StringLiteralExpression element, @NotNull String formType) {
        return getFormDefaultOptionTargets(element, formType);
    }

    @NotNull
    public static Collection<LookupElement> getDefaultOptionLookupElements(@NotNull Project project, @NotNull String formType) {
        Collection<LookupElement> lookupElements = new ArrayList<>();
        FormOptionsUtil.getFormDefaultKeys(project, formType, new FormOptionLookupVisitor(lookupElements));
        return lookupElements;
    }

    @Nullable
    public static String getTranslationFromScope(@NotNull ArrayCreationExpression arrayCreation) {
        // translation_domain in current array block
        String translationDomain = PhpElementsUtil.getArrayHashValue(arrayCreation, "translation_domain");
        if(translationDomain == null) {
            if (FormAttributeUtil.getOptionsAttribute(arrayCreation) != null) {
                PhpClass phpClass = getFormPhpClassFromContext(arrayCreation);
                return phpClass == null ? null : PhpElementsUtil.getStringValue(FormAttributeUtil.getDefaultOptionValue(phpClass, "translation_domain"));
            }

            // find on default options inside FormType
            translationDomain = PhpElementsUtil.getArrayKeyValueInsideSignature(arrayCreation, FormOptionsUtil.FORM_OPTION_METHODS, "setDefaults", "translation_domain");
        }

        return translationDomain;
    }

    /**
     * Find methodReferences in scope by its name and method scope and give string value of matching parameter
     *
     * $resolver->setDefault('data_class', User::class);
     */
    @NotNull
    public static Collection<String> getMethodReferenceStringParameter(@NotNull PsiElement scope, String[] scopeMethods, @NotNull String methodName, @NotNull String key) {
        PhpClass phpClass = PsiTreeUtil.getParentOfType(scope, PhpClass.class);
        if(phpClass == null) {
            return Collections.emptyList();
        }

        Collection<String> values = new HashSet<>();

        for (String scopeMethod : scopeMethods) {
            Method method = phpClass.findMethodByName(scopeMethod);
            if(method == null) {
                continue;
            }

            for (MethodReference methodReference : PhpElementsUtil.collectMethodReferencesInsideControlFlow(method, methodName)) {
                // our key to find
                String argument = PhpElementsUtil.getMethodReferenceStringValueParameter(methodReference, 0);
                if(!key.equals(argument)) {
                    continue;
                }

                // resolve value
                String value = PhpElementsUtil.getMethodReferenceStringValueParameter(methodReference, 1);
                if(StringUtils.isBlank(value)) {
                    continue;
                }

                values.add(value);
            }
        }

        return values;
    }

    /**
     * $resolver->setDefaults(['data_class' => User::class]);
     * $resolver->setDefault('data_class', User::class);
     */
    @Nullable
    public static PhpClass getFormPhpClassFromContext(@NotNull PsiElement psiElement) {
        PhpClass phpClass = psiElement instanceof PhpClass clazz ? clazz : PsiTreeUtil.getParentOfType(psiElement, PhpClass.class);
        if (phpClass != null && !phpClass.getAttributes(AS_FORM_TYPE_ATTRIBUTE).isEmpty()) {
            return phpClass;
        }

        return getPlainFormDataClass(psiElement);
    }

    @Nullable
    private static PhpClass getPlainFormDataClass(@NotNull PsiElement psiElement) {
        Collection<String> classes = new ArrayList<>();

        // $resolver->setDefaults(['data_class' => User::class]);
        String className = PhpElementsUtil.getArrayKeyValueInsideSignature(psiElement, FormOptionsUtil.FORM_OPTION_METHODS, "setDefaults", "data_class");
        if(className != null) {
            classes.add(className);
        }

        // $resolver->setDefault('data_class', User::class);
        classes.addAll(FormOptionsUtil.getMethodReferenceStringParameter(psiElement, FormOptionsUtil.FORM_OPTION_METHODS, "setDefault", "data_class"));

        // find first class
        Project project = psiElement.getProject();

        return classes.stream()
            .map(clazz -> PhpElementsUtil.getClassInterface(project, clazz))
            .filter(Objects::nonNull).findFirst()
            .orElse(null);
    }
}
