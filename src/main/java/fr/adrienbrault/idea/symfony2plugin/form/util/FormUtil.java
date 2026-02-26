package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.util.*;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpAccessVariableInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpReturnInstruction;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpYieldInstruction;
import com.jetbrains.php.lang.PhpLanguage;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.doctrine.metadata.util.DoctrineMetadataUtil;
import fr.adrienbrault.idea.symfony2plugin.form.FormTypeLookup;
import fr.adrienbrault.idea.symfony2plugin.form.dict.EnumFormTypeSource;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpIndexUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import kotlin.Pair;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FormUtil {
    private static final Key<CachedValue<Collection<String[]>>> FORM_EXTENSION_TYPES = new Key<>("SYMFONY_FORM_EXTENSION_TYPES");

    final public static String ABSTRACT_FORM_INTERFACE = "\\Symfony\\Component\\Form\\FormTypeInterface";
    final public static String FORM_EXTENSION_INTERFACE = "\\Symfony\\Component\\Form\\FormTypeExtensionInterface";

    public static final MethodMatcher.CallToSignature[] PHP_FORM_BUILDER_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormBuilderInterface", "add"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormBuilderInterface", "create"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormInterface", "add"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormInterface", "create")
    };

    public static final MethodMatcher.CallToSignature[] FORM_FACTORY_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\Controller", "createForm"),
        // Symfony 3.3 / 3.4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\ControllerTrait", "createForm"),
        // Symfony 4
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "createForm"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "create"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormFactory", "createBuilder"),
    };

    public static final MethodMatcher.CallToSignature[] PHP_FORM_NAMED_BUILDER_SIGNATURES = new MethodMatcher.CallToSignature[] {
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "createNamedBuilder"),
        new MethodMatcher.CallToSignature("\\Symfony\\Component\\Form\\FormFactoryInterface", "createNamed"),
    };

    @Nullable
    public static PhpClass getFormTypeToClass(Project project, @Nullable String formType) {
        return new FormTypeCollector(project).collect().getFormTypeToClass(formType);
    }

    public static Collection<LookupElement> getFormTypeLookupElements(@NotNull Project project) {

        Collection<LookupElement> lookupElements = new ArrayList<>();

        FormUtil.FormTypeCollector collector = new FormUtil.FormTypeCollector(project).collect();

        for(Map.Entry<String, FormTypeClass> entry: collector.getFormTypesMap().entrySet()) {
            String name = entry.getValue().getName();
            String typeText = entry.getValue().getPhpClassName();

            PhpClass phpClass = entry.getValue().getPhpClass(project);
            if(phpClass != null) {
                typeText = phpClass.getName();
            }

            FormTypeLookup formTypeLookup = new FormTypeLookup(typeText, name);
            if(entry.getValue().getSource() == EnumFormTypeSource.INDEX) {
                formTypeLookup.withWeak(true);
            }

            lookupElements.add(formTypeLookup);
        }

        return lookupElements;
    }

    public static MethodReference[] getFormBuilderTypes(@NotNull Method method) {
        PsiElementFilter filter = methodReference -> {
            if (methodReference instanceof MethodReference) {
                String methodName = ((MethodReference) methodReference).getName();
                if (methodName != null && (methodName.equals("add") || methodName.equals("create"))) {
                    return PhpElementsUtil.isMethodReferenceInstanceOf((MethodReference) methodReference, FormUtil.PHP_FORM_BUILDER_SIGNATURES);
                }
            }

            return false;
        };

        Collection<PsiElement> methodReferences = new HashSet<>(Arrays.asList(PsiTreeUtil.collectElements(method, filter)));

        // some code flow detection for sub methods
        if ("buildForm".equals(method.getName())) {
            for (Parameter parameter : method.getParameters()) {
                boolean isFormBuilder = parameter.getType().getTypes().stream()
                    .noneMatch(s -> StringUtils.stripStart(s, "\\").equalsIgnoreCase("Symfony\\Component\\Form\\FormBuilderInterface"));

                if (isFormBuilder) {
                    continue;
                }

                String text = parameter.getName();

                Collection<PsiElement> variables = new ArrayList<>();

                PhpControlFlowUtil.processFlow(method.getControlFlow(), new PhpInstructionProcessor() {
                    @Override
                    public boolean processAccessVariableInstruction(PhpAccessVariableInstruction instruction) {
                        if (text.contentEquals(instruction.getVariableName())) {
                            variables.add(instruction.getAnchor());
                        }

                        return super.processAccessVariableInstruction(instruction);
                    }
                });

                for (PsiElement psiElement : variables) {
                    PsiElement parameterList = psiElement.getParent();
                    if (!(parameterList instanceof ParameterList)) {
                        continue;
                    }
                    PsiElement methodReference = parameterList.getParent();
                    if (!(methodReference instanceof MethodReference)) {
                        continue;
                    }

                    PsiElement resolve = ((MethodReference) methodReference).resolve();
                    if (!(resolve instanceof Method)) {
                        continue;
                    }

                    Collections.addAll(methodReferences, PsiTreeUtil.collectElements(resolve, filter));
                }
            }
        }

        return methodReferences.stream().map(psiElement -> (MethodReference) psiElement).toArray(MethodReference[]::new);
    }

    /**
     * $form->get ..
     */
    @Nullable
    private static PhpClass resolveFormGetterCall(MethodReference methodReference) {

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

    private static PhpClass getFormTypeClass(@Nullable MethodReference calledMethodReference) {
        if(calledMethodReference == null || !PhpElementsUtil.isMethodReferenceInstanceOf(calledMethodReference, "\\Symfony\\Component\\Form\\FormFactory", "create")) {
            return null;
        }

        // $form = "$this->createForm("new Type()", $entity)";
        PsiElement formType = PsiElementUtils.getMethodParameterPsiElementAt(calledMethodReference, 0);
        if(formType == null) {
            return null;
        }

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

        Method method = formPhpClass.findMethodByName("buildForm");
        if (method == null) {
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
    public static PhpClass getFormTypeClassOnParameter(@NotNull PsiElement psiElement) {

        if (psiElement instanceof StringLiteralExpression) {
            return getFormTypeToClass(psiElement.getProject(), ((StringLiteralExpression) psiElement).getContents());
        }

        if (psiElement instanceof ClassConstantReference) {
            return PhpElementsUtil.getClassConstantPhpClass((ClassConstantReference) psiElement);
        }

        if (psiElement instanceof PhpTypedElement) {
            String typeName = ((PhpTypedElement) psiElement).getType().toString();
            return getFormTypeToClass(psiElement.getProject(), typeName);
        }

        return null;
    }

    @NotNull
    public static Collection<String> getFormAliases(@NotNull PhpClass phpClass) {
        // check class implements form interface
        if(!PhpElementsUtil.isInstanceOf(phpClass, ABSTRACT_FORM_INTERFACE)) {
            return Collections.emptySet();
        }

        String className = FormUtil.getFormNameOfPhpClass(phpClass);
        if(className == null) {
            return Collections.emptySet();
        }

        return Collections.singleton(className);
    }

    public static void attachFormAliasesCompletions(@NotNull PhpClass phpClass, @NotNull CompletionResultSet completionResultSet) {
        for(String alias: getFormAliases(phpClass)) {
            completionResultSet.addElement(LookupElementBuilder.create(alias).withIcon(Symfony2Icons.FORM_TYPE).withTypeText(phpClass.getPresentableFQN(), true));
        }
    }

    /**
     * acme_demo.form.type.gender:
     *  class: espend\Form\TypeBundle\Form\FooType
     *  tags:
     *   - { name: form.type, alias: foo_type_alias  }
     *   - { name: foo  }
     */
    @NotNull
    public static Map<String, Set<String>> getTags(@NotNull YAMLFile yamlFile) {
        Map<String, Set<String>> map = new HashMap<>();

        for(YAMLKeyValue yamlServiceKeyValue : YamlHelper.getQualifiedKeyValuesInFile(yamlFile, "services")) {
            String serviceName = yamlServiceKeyValue.getName();
            Set<String> serviceTagMap = YamlHelper.collectServiceTags(yamlServiceKeyValue);
            if(!serviceTagMap.isEmpty()) {
                map.put(serviceName, serviceTagMap);
            }
        }

        return map;
    }

    public static Map<String, Set<String>> getTags(@NotNull XmlFile psiFile) {
        Map<String, Set<String>> map = new HashMap<>();

        XmlDocumentImpl document = PsiTreeUtil.getChildOfType(psiFile, XmlDocumentImpl.class);
        if(document == null) {
            return map;
        }

        /*
         * <services>
         *   <service id="espend_form.foo_type" class="%espend_form.foo_type.class%">
         *     <tag name="form.type" alias="foo_type_alias" />
         *   </service>
         * </services>
         */

        XmlTag[] xmlTags = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
        if(xmlTags == null) {
            return map;
        }

        for(XmlTag xmlTag: xmlTags) {
            if(xmlTag.getName().equals("container")) {
                for(XmlTag servicesTag: xmlTag.getSubTags()) {
                    if(servicesTag.getName().equals("services")) {
                        for(XmlTag serviceTag: servicesTag.getSubTags()) {
                            XmlAttribute attrValue = serviceTag.getAttribute("id");
                            if(attrValue != null) {

                                // <service id="foo.bar" class="Class\Name">
                                String serviceNameId = attrValue.getValue();
                                if(serviceNameId != null) {

                                    Set<String> serviceTags = getTags(serviceTag);
                                    if(!serviceTags.isEmpty()) {
                                        map.put(serviceNameId, serviceTags);
                                    }
                                }

                            }
                        }
                    }
                }
            }
        }

        return map;
    }

    public static Set<String> getTags(XmlTag serviceTag) {

        Set<String> tags = new HashSet<>();

        for(XmlTag serviceSubTag: serviceTag.getSubTags()) {
            if("tag".equals(serviceSubTag.getName())) {
                XmlAttribute attribute = serviceSubTag.getAttribute("name");
                if(attribute != null) {
                    String tagName = attribute.getValue();
                    if(StringUtils.isNotBlank(tagName)) {
                        tags.add(tagName);
                    }
                }

            }
        }

        return tags;
    }

    @NotNull
    public static Map<String, FormTypeClass> getFormTypeClasses(@NotNull Project project) {
        Collection<String[]> nameAndClass = CachedValuesManager.getManager(project).getCachedValue(
            project,
            FORM_EXTENSION_TYPES,
            new CachedValueProvider<>() {
                @Override
                public @NotNull Result<Collection<String[]>> compute() {
                    Collection<String[]> items = new ArrayList<>();

                    for (PhpClass phpClass : PhpIndexUtil.getAllSubclasses(project, ABSTRACT_FORM_INTERFACE)) {
                        if (!isValidFormPhpClass(phpClass)) {
                            continue;
                        }

                        String name = FormUtil.getFormNameOfPhpClass(phpClass);
                        if (name == null) {
                            continue;
                        }

                        items.add(new String[]{name, phpClass.getFQN()});

                    }

                    return Result.create(items, PsiModificationTracker.getInstance(project).forLanguage(PhpLanguage.INSTANCE));
                }
            },
            false
        );

        Map<String, FormTypeClass> map = new HashMap<>();

        for (String[] item : nameAndClass) {
            map.put(item[0], new FormTypeClass(item[0], item[1], EnumFormTypeSource.INDEX));
        }

        return map;
    }

    public static boolean isValidFormPhpClass(PhpClass phpClass) {
        return !(phpClass.isAbstract() || phpClass.isInterface() || PhpElementsUtil.isTestClass(phpClass));
    }

    public static class FormTypeCollector {

        private final Map<String, FormTypeClass> formTypesMap;
        private final Project project;

        public FormTypeCollector(Project project) {
            this.project = project;
            this.formTypesMap = new HashMap<>();
        }

        public FormTypeCollector collect() {

            // on indexer, compiler wins...
            formTypesMap.putAll(FormUtil.getFormTypeClasses(project));

            // find on registered formtype aliases on compiled container
            FormTypeServiceParser formTypeServiceParser = ServiceXmlParserFactory.getInstance(project, FormTypeServiceParser.class);
            for(Map.Entry<String, String> entry: formTypeServiceParser.getFormTypeMap().getMap().entrySet()) {
                String formTypeName = entry.getValue();
                formTypesMap.put(formTypeName, new FormTypeClass(formTypeName, entry.getKey(), EnumFormTypeSource.COMPILER));
            }

            return this;
        }

        @Nullable
        public PhpClass getFormTypeToClass(@Nullable String formType) {

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

            return this.getFormTypeClass(formType);

        }

        @Nullable
        public PhpClass getFormTypeClass(String formTypeName) {

            // find on registered formtype aliases on compiled container

            FormTypeClass serviceName = this.formTypesMap.get(formTypeName);

            // compiled container resolve
            if(serviceName != null) {
                PhpClass phpClass = serviceName.getPhpClass(project);
                if (phpClass != null) {
                    return phpClass;
                }
            }

            // on indexer
            Map<String, FormTypeClass> forms = FormUtil.getFormTypeClasses(project);
            if(!forms.containsKey(formTypeName)) {
                return null;
            }

            return forms.get(formTypeName).getPhpClass(project);
        }

        public Map<String, FormTypeClass> getFormTypesMap() {
            return formTypesMap;
        }
    }

    /**
     * Replace "hidden" with HiddenType:class
     * @throws Exception
     */
    public static void replaceFormStringAliasWithClassConstant(@NotNull StringLiteralExpression psiElement) throws Exception {
        String contents = psiElement.getContents();
        if(StringUtils.isBlank(contents)) {
            throw new Exception("Empty content");
        }

        PhpClass phpClass = getFormTypeToClass(psiElement.getProject(), contents);
        if(phpClass == null) {
            throw new Exception("No class found");
        }

        PhpElementsUtil.replaceElementWithClassConstant(phpClass, psiElement);
    }

    /**
     * Finds form parent by "getParent" method
     *
     * Concatenation "__NAMESPACE__.'\Foo', "Foo::class" and string 'foo' supported
     */
    @NotNull
    public static Collection<String> getFormParentOfPhpClass(@NotNull PhpClass phpClass) {
        Method getParent = phpClass.findMethodByName("getParent");
        if(getParent == null) {
            return Collections.emptyList();
        }

        Collection<String> parents = new HashSet<>();

        for (PsiElement phpReturnArgument : PhpElementsUtil.collectPhpReturnArgumentsInsideControlFlow(getParent)) {
            if(phpReturnArgument instanceof TernaryExpression ternaryExpression) {
                // true ? 'foobar' : Foo::class
                parents.addAll(PhpElementsUtil.getTernaryExpressionConditionStrings(ternaryExpression));
            } else if(phpReturnArgument instanceof BinaryExpression binaryExpression && PsiElementAssertUtil.isNotNullAndIsElementType(phpReturnArgument, PhpElementTypes.CONCATENATION_EXPRESSION)) {
                // Symfony core: __NAMESPACE__.'\Foo'
                PsiElement leftOperand = binaryExpression.getLeftOperand();
                ConstantReference constantReference = PsiElementAssertUtil.getInstanceOfOrNull(leftOperand, ConstantReference.class);
                if(constantReference == null || !"__NAMESPACE__".equals(constantReference.getName())) {
                    continue;
                }

                StringLiteralExpression stringValue = PsiElementAssertUtil.getInstanceOfOrNull(binaryExpression.getRightOperand(), StringLiteralExpression.class);
                if(stringValue == null) {
                    continue;
                }

                String contents = stringValue.getContents();
                if(StringUtils.isBlank(contents)) {
                    continue;
                }

                parents.add(StringUtils.strip(phpClass.getNamespaceName(), "\\") + contents);
            }

            // fallback try to resolve string value
            String contents = PhpElementsUtil.getStringValue(phpReturnArgument);
            if(StringUtils.isNotBlank(contents)) {
                parents.add(contents);
            }
        }

        return parents;
    }

    /**
     * Finds form name by "getName" method
     *
     * Symfony < 2.8
     * 'foo_bar'
     *
     * Symfony 2.8
     * "$this->getName()" -> "$this->getBlockPrefix()" -> return 'datetime';
     * "UserProfileType" => "user_profile" and namespace removal
     *
     * Symfony 3.0
     * Use class name: Foo\Class
     *
     */
    @Nullable
    public static String getFormNameOfPhpClass(@NotNull PhpClass phpClass) {
        Method method = phpClass.findOwnMethodByName("getName");

        // @TODO: think of interface switches

        // method not found so use class fqn
        if(method == null) {
            return StringUtils.stripStart(phpClass.getFQN(), "\\");
        }

        for (PsiElement firstPsiChild : PhpElementsUtil.collectPhpReturnArgumentsInsideControlFlow(method)) {
            // $this->getBlockPrefix()
            if(firstPsiChild instanceof MethodReference methodReference) {
                PhpExpression classReference = methodReference.getClassReference();
                if(classReference != null && "this".equals(classReference.getName())) {
                    String name = methodReference.getName();
                    if("getBlockPrefix".equals(name)) {
                        if(phpClass.findOwnMethodByName("getBlockPrefix") != null) {
                            return PhpElementsUtil.getMethodReturnAsString(phpClass, name);
                        } else {
                            // method has no custom overwrite; rebuild expression here:
                            // FooBarType -> foo_bar
                            String className = phpClass.getName();

                            // strip Type and type
                            if(className.toLowerCase().endsWith("type") && className.length() > 4) {
                                className = className.substring(0, className.length() - 4);
                            }

                            return fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore(className);
                        }
                    }
                }

                continue;
            }

            // string value fallback
            String stringValue = PhpElementsUtil.getStringValue(firstPsiChild);
            if(stringValue != null) {
                return stringValue;
            }
        }


        return null;
    }

    /**
     * Get getExtendedType and getExtendedTypes (Symfony >= 4.2) as string
     *
     * "return Foo::class;"
     * "return 'foobar';"
     * "return [Foo::class, FooBar::class];"
     * "return true === true ? FileType::class : Form:class;"
     * "yield Foo::class;"
     */
    @NotNull
    public static Collection<String> getFormExtendedType(@NotNull PhpClass phpClass) {
        Collection<String> types = new HashSet<>();

        // public function getExtendedType() { return FileType::class; }
        // public function getExtendedType() { return true === true ? FileType::class : Form:class; }
        Method extendedType = phpClass.findMethodByName("getExtendedType");
        if(extendedType != null) {
            for (PsiElement phpReturnsArgument : PhpElementsUtil.collectPhpReturnArgumentsInsideControlFlow(extendedType)) {
                // true ? 'foo' : 'foo'
                if(phpReturnsArgument instanceof TernaryExpression ternaryExpression) {
                    types.addAll(PhpElementsUtil.getTernaryExpressionConditionStrings(ternaryExpression));
                }

                String stringValue = PhpElementsUtil.getStringValue(phpReturnsArgument);
                if(stringValue != null) {
                    types.add(stringValue);
                }
            }
        }

        // Symfony 4.2: Support improved form type extensions:
        // public static function getExtendedTypes(): iterable:
        // https://symfony.com/blog/new-in-symfony-4-2-improved-form-type-extensions
        Method extendedTypes = phpClass.findMethodByName("getExtendedTypes");
        if (extendedTypes != null) {
            Collection<ArrayCreationExpression> phpReturnsArray = new ArrayList<>();
            Collection<PsiElement> yieldArguments = new ArrayList<>();

            PhpControlFlowUtil.processFlow(extendedTypes.getControlFlow(), new PhpInstructionProcessor() {
                @Override
                public boolean processYieldInstruction(PhpYieldInstruction instruction) {
                    PsiElement argument = instruction.getArgument();
                    if (argument != null) {
                        yieldArguments.add(argument);
                    }

                    return super.processYieldInstruction(instruction);
                }

                @Override
                public boolean processReturnInstruction(PhpReturnInstruction instruction) {
                    if (instruction.getArgument() instanceof ArrayCreationExpression arrayCreationExpression) {
                        phpReturnsArray.add(arrayCreationExpression);
                    }

                    return super.processReturnInstruction(instruction);
                }
            });

            // [Foo::class, FooBar::class]
            for (ArrayCreationExpression phpReturnArray : phpReturnsArray) {
                Collection<PsiElement> arrayValues = PhpPsiUtil.getChildren(phpReturnArray, psiElement ->
                    psiElement.getNode().getElementType() == PhpElementTypes.ARRAY_VALUE
                );

                for (PsiElement child : arrayValues) {
                    String stringValue = PhpElementsUtil.getStringValue(child.getFirstChild());
                    if (stringValue != null) {
                        types.add(stringValue);
                    }
                }
            }

            // yield Foo::class
            for (PsiElement yieldArgument : yieldArguments) {
                String stringValue = PhpElementsUtil.getStringValue(yieldArgument);
                if (stringValue != null) {
                    types.add(stringValue);
                }
            }
        }

        return types;
    }

    /**
     * Find form php class scope: FormType or FormExtension
     */
    @Nullable
    public static String getFormTypeClassFromScope(@NotNull PsiElement psiElement) {
        Method methodScope = PsiTreeUtil.getParentOfType(psiElement, Method.class);

        if(methodScope != null) {
            PhpClass phpClass = methodScope.getContainingClass();
            if(phpClass != null && (PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Form\\FormTypeInterface") || PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Form\\FormExtensionInterface"))) {
                return phpClass.getFQN();
            }
        }

        return null;
    }

    /**
     * public function getParent() { return ChoiceType::class; }
     */
    @NotNull
    public static Collection<String> getFormTypeParentFromOptionResolverScope(@NotNull PsiElement psiElement) {
        Method methodScope = PsiTreeUtil.getParentOfType(psiElement, Method.class);

        if (methodScope != null && Arrays.stream(FormOptionsUtil.FORM_OPTION_METHODS).anyMatch(s -> s.equalsIgnoreCase(methodScope.getName()))) {
            PhpClass phpClass = methodScope.getContainingClass();
            if (phpClass != null) {
                return PhpElementsUtil.getMethodReturnAsStrings(phpClass, "getParent");
            }
        }

        return Collections.emptySet();
    }

    /**
     *
     * public function buildForm(FormBuilderInterface $builder, array $options) {
     *   $options['<caret>'];
     * }
     *
     * public function getParent() { return ChoiceType::class; }
     */
    @NotNull
    public static Collection<String> getFormTypeParentFromFormTypeImplementation(@NotNull PsiElement psiElement) {
        Method methodScope = PsiTreeUtil.getParentOfType(psiElement, Method.class);

        if (methodScope != null) {
            PhpClass phpClass = methodScope.getContainingClass();
            if (phpClass != null && (PhpElementsUtil.isInstanceOf(phpClass, "\\Symfony\\Component\\Form\\FormTypeInterface"))) {
                return PhpElementsUtil.getMethodReturnAsStrings(phpClass, "getParent");
            }
        }

        return Collections.emptyList();
    }

    public static Pair<String, Map<String, String>> getGuessedFormFieldParameters(@NotNull PhpIndex phpIndex, @NotNull Project project, @NotNull String key, @NotNull PhpNamedElement phpNamedElement) {
        // method / function
        PhpType phpType = null;
        if (phpNamedElement instanceof Function function) {
            PsiElement parameter = function.getParameter(0);
            if (parameter instanceof PhpTypedElement phpTypedElement) {
                phpType = phpIndex.completeType(project, phpTypedElement.getType(), new HashSet<>());
            }
        } else {
            // properties
            phpType = phpIndex.completeType(project, phpNamedElement.getType(), new HashSet<>());
        }

        if (phpType == null) {
            return new Pair<>("\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType", null);
        }

        String typeClass = null;
        Map<String, String> options = new HashMap<>();

        PhpClass phpClass = PhpElementsUtil.getClassFromPhpTypeSetArrayClean(project, phpType.getTypes()).stream().findFirst().orElse(null);

        if (phpClass != null && PhpElementsUtil.isInstanceOf(phpClass, "\\DateTimeImmutable")) {
            typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType";
            options.put("input", "'datetime_immutable'");
        } else if (phpClass != null && PhpElementsUtil.isInstanceOf(phpClass, "\\DateTimeInterface")) {
            typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\DateTimeType";
        } else if (phpClass != null && phpClass.isEnum()) {
            typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\EnumType";
            options.put("class", phpClass.getFQN());
        } else if (phpClass != null && !DoctrineMetadataUtil.findMetadataFiles(project, StringUtils.stripStart(phpClass.getFQN(), "\\")).isEmpty()) {
            typeClass = "\\Symfony\\Bridge\\Doctrine\\Form\\Type\\EntityType";
            options.put("class", phpClass.getFQN());
        } else if (phpType.isConvertibleFrom(project, PhpType.from(PhpType.INT))) {
            typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\IntegerType";
        } else if (phpType.isConvertibleFrom(project, PhpType.from(PhpType.FLOAT))) {
            typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\NumberType";
        } else if (phpType.isConvertibleFrom(project, PhpType.from(PhpType.ARRAY))) {
            typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\ChoiceType";
            options.put("choices", "[]");
        } else if (phpType.isConvertibleFrom(project, PhpType.from(PhpType.STRING))) {
            typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextType";

            String lowerCase = key.toLowerCase();
            if (lowerCase.contains("description") || lowerCase.contains("note") || lowerCase.contains("beschreibung") || lowerCase.contains("comment")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TextAreaType";
            } else if (lowerCase.contains("mail")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\EmailType";
            } else if (lowerCase.contains("password")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\PasswordType";
            } else if (lowerCase.contains("url")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\UrlType";
            } else if (lowerCase.contains("language")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\LanguageType";
            } else if (lowerCase.equals("uuid")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\UuidType";
            } else if (lowerCase.equals("ulid")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\UlidType";
            } else if (lowerCase.contains("country")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\CountryType";
            } else if (lowerCase.contains("currency")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\MoneyType";
            } else if (lowerCase.contains("telephone") || lowerCase.contains("phone") || lowerCase.contains("mobile")) {
                typeClass = "\\Symfony\\Component\\Form\\Extension\\Core\\Type\\TelType";
            }
        }

        return new Pair<>(typeClass, options);
    }


    /**
     * Get all form class which extend the given class by any type e.g. parent, extendTypes
     */
    @NotNull
    public static Collection<PhpClass> getParentAndExtendingTypes(@NotNull PhpClass phpClass, @NotNull FormUtil.FormTypeCollector collector) {
        Collection<PhpClass> parentClasses = new HashSet<>();
        visitParentFormsRecursive(phpClass, collector, parentClasses, 10);

        // current class and all parent class need to visit for extending
        Set<String> classesToVisitForExtends = new HashSet<>() {{
            add(phpClass.getFQN());
            addAll(parentClasses.stream().map(PhpNamedElement::getFQN).collect(Collectors.toSet()));
        }};

        for (FormClass formClass : FormOptionsUtil.getExtendedTypeClasses(phpClass.getProject(), classesToVisitForExtends.toArray(String[]::new))) {
            parentClasses.add(formClass.getPhpClass());
        }

        return parentClasses;
    }

    /**
     * Visit recursive all parent forms
     */
    private static void visitParentFormsRecursive(@NotNull PhpClass phpClass, @NotNull FormUtil.FormTypeCollector collector, @NotNull Collection<PhpClass> parentClasses, int depth) {
        Collection<PhpClass> scopedClasses = new HashSet<>();

        for (String formParent : FormUtil.getFormParentOfPhpClass(phpClass)) {
            PhpClass formPhpClass = collector.getFormTypeToClass(formParent);
            if (formPhpClass != null) {
                scopedClasses.add(formPhpClass);
            }
        }

        for (PhpClass nextScopeClass : scopedClasses) {
            depth--;
            if (depth > 0) {
                visitParentFormsRecursive(nextScopeClass, collector, parentClasses, depth);
            }
        }

        parentClasses.addAll(scopedClasses);
    }
}
