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
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.*;

import java.util.*;


public class FormUtil {

    final public static String ABSTRACT_FORM_INTERFACE = "\\Symfony\\Component\\Form\\FormTypeInterface";
    final public static String FORM_EXTENSION_INTERFACE = "\\Symfony\\Component\\Form\\FormTypeExtensionInterface";

    @Nullable
    public static PhpClass getFormTypeToClass(Project project, @Nullable String formType) {
        return new FormTypeCollector(project).collect().getFormTypeToClass(formType);
    }

    public static Collection<LookupElement> getFormTypeLookupElements(Project project) {

        Collection<LookupElement> lookupElements = new ArrayList<LookupElement>();

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

        final List<MethodReference> methodReferences = new ArrayList<MethodReference>();

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
            PhpExpression classReference = ((ClassConstantReference) psiElement).getClassReference();
            if(classReference instanceof PhpReference) {
                String typeName = ((PhpReference) classReference).getFQN();
                if(typeName != null && StringUtils.isNotBlank(typeName)) {
                    return PhpElementsUtil.getClassInterface(psiElement.getProject(), typeName);
                }
            }
        }

        return null;
    }

    @NotNull
    public static Collection<String> getFormAliases(@NotNull PhpClass phpClass) {
        // check class implements form interface
        if(!new Symfony2InterfacesUtil().isInstanceOf(phpClass, ABSTRACT_FORM_INTERFACE)) {
            return Collections.emptySet();
        }

        return PhpElementsUtil.getMethodReturnAsStrings(phpClass, "getName");
    }

    public static void attachFormAliasesCompletions(@NotNull PhpClass phpClass, @NotNull CompletionResultSet completionResultSet) {
        for(String alias: getFormAliases(phpClass)) {
            completionResultSet.addElement(LookupElementBuilder.create(alias).withIcon(Symfony2Icons.FORM_TYPE).withTypeText(phpClass.getPresentableFQN(), true));
        }
    }

    public static Map<String, Set<String>> getTags(YAMLFile psiFile) {

        Map<String, Set<String>> map = new HashMap<String, Set<String>>();

        YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
        if(yamlDocument == null) {
            return map;
        }

        // get services or parameter key
        YAMLKeyValue[] yamlKeys = PsiTreeUtil.getChildrenOfType(yamlDocument, YAMLKeyValue.class);
        if(yamlKeys == null) {
            return map;
        }

        /**
         * acme_demo.form.type.gender:
         * class: espend\Form\TypeBundle\Form\FooType
         * tags:
         *   - { name: form.type, alias: foo_type_alias  }
         *   - { name: foo  }
         */

        for(YAMLKeyValue yamlKeyValue : yamlKeys) {
            String yamlConfigKey = yamlKeyValue.getName();
            if(yamlConfigKey != null && yamlConfigKey.equals("services")) {

                for(YAMLKeyValue yamlServiceKeyValue : PsiTreeUtil.getChildrenOfTypeAsList(yamlKeyValue.getValue(), YAMLKeyValue.class)) {
                    String serviceName = yamlServiceKeyValue.getName();

                    Set<String> serviceTagMap = getTags(yamlServiceKeyValue);
                    if(serviceTagMap.size() > 0) {
                        map.put(serviceName, serviceTagMap);
                    }

                }

            }
        }

        return map;
    }

    public static Set<String> getTags(YAMLKeyValue yamlServiceKeyValue) {

        Set<String> tags = new HashSet<String>();

        YAMLKeyValue tagTag = YamlHelper.getYamlKeyValue(yamlServiceKeyValue, "tags");
        if(tagTag == null) {
            return Collections.emptySet();
        }

        YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getChildOfType(tagTag, YAMLCompoundValue.class);
        if(yamlCompoundValue == null) {
            return Collections.emptySet();
        }

        Collection<YAMLSequence> yamlSequences = PsiTreeUtil.getChildrenOfTypeAsList(yamlCompoundValue, YAMLSequence.class);
        for(YAMLSequence yamlSequence: yamlSequences) {
            YAMLHash yamlHash = PsiTreeUtil.getChildOfType(yamlSequence, YAMLHash.class);

            if(yamlHash != null) {
                YAMLKeyValue yamlTagNameKeyValue = YamlHelper.getYamlKeyValue(yamlHash, "name");
                if(yamlTagNameKeyValue != null) {
                    String tagName = yamlTagNameKeyValue.getValueText();
                    if(tagName != null) {

                        tagName = PsiElementUtils.trimQuote(tagName);
                        if(StringUtils.isNotBlank(tagName)) {
                            tags.add(tagName);
                        }
                    }
                }
            }
        }

        return tags;
    }

    public static Map<String, Set<String>> getTags(XmlFile psiFile) {

        Map<String, Set<String>> map = new HashMap<String, Set<String>>();

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

        Set<String> tags = new HashSet<String>();

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

        Map<String, FormTypeClass> map = new HashMap<String, FormTypeClass>();

        for(PhpClass phpClass: PhpIndex.getInstance(project).getAllSubclasses("Symfony\\Component\\Form\\FormTypeInterface")) {
            String name = PhpElementsUtil.getMethodReturnAsString(phpClass, "getName");
            if(name == null) {
                continue;
            }

            map.put(name, new FormTypeClass(name, phpClass, EnumFormTypeSource.INDEX));
        }

        return map;
    }

    public static class FormTypeCollector {

        private final Map<String, FormTypeClass> formTypesMap;
        private final Project project;

        public FormTypeCollector(Project project) {
            this.project = project;
            this.formTypesMap = new HashMap<String, FormTypeClass>();
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
     * Concatenation "__NAMESPACE__.'\Foo' and string 'foo' supported
     */
    @Nullable
    public static String getFormParentOfPhpClass(@NotNull PhpClass phpClass) {
        Method getParent = phpClass.findOwnMethodByName("getParent");
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
}
