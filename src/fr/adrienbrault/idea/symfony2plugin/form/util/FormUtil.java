package fr.adrienbrault.idea.symfony2plugin.form.util;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.PhpTypedElementImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2InterfacesUtil;
import fr.adrienbrault.idea.symfony2plugin.form.FormTypeLookup;
import fr.adrienbrault.idea.symfony2plugin.form.dict.EnumFormTypeSource;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeClass;
import fr.adrienbrault.idea.symfony2plugin.form.dict.FormTypeServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.*;


public class FormUtil {

    final public static String ABSTRACT_FORM_INTERFACE = "\\Symfony\\Component\\Form\\FormTypeInterface";
    final public static String FORM_EXTENSION_INTERFACE = "\\Symfony\\Component\\Form\\FormTypeExtensionInterface";

    @Nullable
    public static PhpClass getFormTypeToClass(Project project, @Nullable String formType) {
        return new FormTypeCollector(project).collect().getFormTypeToClass(formType);
    }

    public static Collection<LookupElement> getFormTypeLookupElements(Project project) {

        Collection<LookupElement> lookupElements = new ArrayList<>();

        FormUtil.FormTypeCollector collector = new FormUtil.FormTypeCollector(project).collect();

        for(Map.Entry<String, FormTypeClass> entry: collector.getFormTypesMap().entrySet()) {
            String name = entry.getValue().getName();
            String typeText = entry.getValue().getPhpClassName();

            PhpClass phpClass = entry.getValue().getPhpClass();
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

    public static MethodReference[] getFormBuilderTypes(Method method) {

        final List<MethodReference> methodReferences = new ArrayList<>();

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

        if(psiElement instanceof StringLiteralExpression) {
            return getFormTypeToClass(psiElement.getProject(), ((StringLiteralExpression) psiElement).getContents());
        }

        if(psiElement instanceof PhpTypedElementImpl) {
            String typeName = ((PhpTypedElementImpl) psiElement).getType().toString();
            return getFormTypeToClass(psiElement.getProject(), typeName);
        }

        if(psiElement instanceof ClassConstantReference) {
            return PhpElementsUtil.getClassConstantPhpClass((ClassConstantReference) psiElement);
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
            if(serviceTagMap != null && serviceTagMap.size() > 0) {
                map.put(serviceName, serviceTagMap);
            }
        }

        return map;
    }

    public static Map<String, Set<String>> getTags(XmlFile psiFile) {

        Map<String, Set<String>> map = new HashMap<>();

        XmlDocumentImpl document = PsiTreeUtil.getChildOfType(psiFile, XmlDocumentImpl.class);
        if(document == null) {
            return map;
        }

        /**
         * <services>
         *   <service id="espend_form.foo_type" class="%espend_form.foo_type.class%">
         *     <tag name="form.type" alias="foo_type_alias" />
         *   </service>
         * </services>
         */

        XmlTag xmlTags[] = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
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
                                    if(serviceTags.size() > 0) {
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
                    if(tagName != null && StringUtils.isNotBlank(tagName)) {
                        tags.add(tagName);
                    }
                }

            }
        }

        return tags;
    }

    @NotNull
    public static Map<String, FormTypeClass> getFormTypeClasses(@NotNull Project project) {

        Map<String, FormTypeClass> map = new HashMap<>();

        for(PhpClass phpClass: PhpIndex.getInstance(project).getAllSubclasses(ABSTRACT_FORM_INTERFACE)) {
            if(!isValidFormPhpClass(phpClass)) {
                continue;
            }

            String name = FormUtil.getFormNameOfPhpClass(phpClass);
            if (name == null) {
                continue;
            }

            map.put(name, new FormTypeClass(name, phpClass, EnumFormTypeSource.INDEX));
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
        public FormTypeClass getFormType(String formTypeName) {

            if(this.formTypesMap.containsKey(formTypeName)) {
                return this.formTypesMap.get(formTypeName);
            }

            return null;
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

            return forms.get(formTypeName).getPhpClass();
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
    @Nullable
    public static String getFormParentOfPhpClass(@NotNull PhpClass phpClass) {
        Method getParent = phpClass.findMethodByName("getParent");
        if(getParent == null) {
            return null;
        }

        for (PhpReturn phpReturn : PsiTreeUtil.collectElementsOfType(getParent, PhpReturn.class)) {
            PhpPsiElement firstPsiChild = phpReturn.getFirstPsiChild();
            if(firstPsiChild instanceof StringLiteralExpression) {
                String contents = ((StringLiteralExpression) firstPsiChild).getContents();
                if(StringUtils.isNotBlank(contents)) {
                    return contents;
                }
                continue;
            }

            // Foo::class
            if(firstPsiChild instanceof ClassConstantReference) {
                return PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) firstPsiChild);
            }

            if(!(firstPsiChild instanceof BinaryExpression) || !PsiElementAssertUtil.isNotNullAndIsElementType(firstPsiChild, PhpElementTypes.CONCATENATION_EXPRESSION)) {
                continue;
            }

            PsiElement leftOperand = ((BinaryExpression) firstPsiChild).getLeftOperand();
            ConstantReference constantReference = PsiElementAssertUtil.getInstanceOfOrNull(leftOperand, ConstantReference.class);
            if(constantReference == null || !"__NAMESPACE__".equals(constantReference.getName())) {
                continue;
            }

            StringLiteralExpression stringValue = PsiElementAssertUtil.getInstanceOfOrNull(((BinaryExpression) firstPsiChild).getRightOperand(), StringLiteralExpression.class);
            if(stringValue == null) {
                continue;
            }

            String contents = stringValue.getContents();
            if(StringUtils.isBlank(contents)) {
                continue;
            }

            return StringUtils.strip(phpClass.getNamespaceName(), "\\") + contents;
        }

        return null;
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
            String fqn = phpClass.getFQN();
            return fqn != null ? StringUtils.stripStart(fqn, "\\") : null;
        }

        for (PhpReturn phpReturn : PsiTreeUtil.collectElementsOfType(method, PhpReturn.class)) {
            PhpPsiElement firstPsiChild = phpReturn.getFirstPsiChild();

            // $this->getBlockPrefix()
            if(firstPsiChild instanceof MethodReference) {
                PhpExpression classReference = ((MethodReference) firstPsiChild).getClassReference();
                if(classReference != null && "this".equals(classReference.getName())) {
                    String name = firstPsiChild.getName();
                    if(name != null && "getBlockPrefix".equals(name)) {
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
     * Get getExtendedType as string
     *
     * 'Foo::class' and string 'foo' supported
     */
    @Nullable
    public static String getFormExtendedType(@NotNull PhpClass phpClass) {
        Method getParent = phpClass.findMethodByName(FormOptionsUtil.EXTENDED_TYPE_METHOD);
        if(getParent == null) {
            return null;
        }

        for (PhpReturn phpReturn : PsiTreeUtil.collectElementsOfType(getParent, PhpReturn.class)) {
            PhpPsiElement firstPsiChild = phpReturn.getFirstPsiChild();

            // Foo::class
            if(firstPsiChild instanceof ClassConstantReference) {
                return PhpElementsUtil.getClassConstantPhpFqn((ClassConstantReference) firstPsiChild);
            }

            String stringValue = PhpElementsUtil.getStringValue(firstPsiChild);
            if(stringValue != null) {
                return stringValue;
            }
        }

        return null;
    }
}
