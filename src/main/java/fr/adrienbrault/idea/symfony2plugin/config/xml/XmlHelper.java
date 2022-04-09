package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.util.Consumer;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.dic.ParameterResolverConsumer;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.ParameterVisitor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class XmlHelper {
    public static PsiElementPattern.Capture<PsiElement> getTagPattern(String... tags) {
        return XmlPatterns
            .psiElement()
            .inside(XmlPatterns
                .xmlAttributeValue()
                .inside(XmlPatterns
                    .xmlAttribute()
                    .withName(StandardPatterns.string().oneOfIgnoreCase(tags)
                    )
                )
            );
    }

    /**
     * <tag attributeNames="|"/>
     *
     * @param tag tagname
     * @param attributeNames attribute values listen for
     */
    public static PsiElementPattern.Capture<PsiElement> getTagAttributePattern(String tag, String... attributeNames) {
        return XmlPatterns
            .psiElement()
            .inside(XmlPatterns
                .xmlAttributeValue()
                .inside(XmlPatterns
                    .xmlAttribute()
                    .withName(StandardPatterns.string().oneOfIgnoreCase(attributeNames))
                    .withParent(XmlPatterns
                        .xmlTag()
                        .withName(tag)
                    )
                )
            ).inFile(getXmlFilePattern());
    }

    /**
     * <tag attributeNames="|"/>
     *
     * @param attributeNames attribute values listen for
     */
    public static PsiElementPattern.Capture<PsiElement> getAttributePattern(String... attributeNames) {
        return XmlPatterns
            .psiElement()
            .inside(XmlPatterns
                .xmlAttributeValue()
                .inside(XmlPatterns
                    .xmlAttribute()
                    .withName(StandardPatterns.string().oneOfIgnoreCase(attributeNames))
                )
            ).inFile(getXmlFilePattern());
    }

    /**
     * <tag attributeNames="|"/>
     */
    public static PsiElementPattern.Capture<PsiElement> getGlobalStringAttributePattern() {
        return XmlPatterns
            .psiElement()
            .inside(XmlPatterns
                .xmlAttributeValue().withValue(XmlPatterns.string().andOr(
                    XmlPatterns.string().endsWith(".html.twig"),
                    XmlPatterns.string().endsWith(".html.php")
                ))
            ).inFile(getXmlFilePattern());
    }

    /**
     * <parameter key="fos_user.user_manager.class">FOS\UserBundle\Doctrine\UserManager</parameter>
     */
    public static PsiElementPattern.Capture<PsiElement> getParameterWithClassEndingPattern() {
        return XmlPatterns
            .psiElement()
            .withParent(XmlPatterns
                .xmlText()
                .withParent(XmlPatterns
                    .xmlTag()
                    .withName("parameter").withChild(
                        XmlPatterns.xmlAttribute("key").withValue(
                            XmlPatterns.string().endsWith(".class")
                        )
                    )
                )
            ).inside(
                XmlPatterns.psiElement(XmlTag.class).withName("parameters")
            ).inFile(getXmlFilePattern());
    }

    /**
     * <argument type="service" id="service_container" />
     */
    public static XmlAttributeValuePattern getArgumentServiceIdPattern() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("id")
                .withParent(XmlPatterns
                    .xmlTag()
                    .withChild(XmlPatterns
                        .xmlAttribute("type")
                        .withValue(
                            StandardPatterns.string().equalTo("service")
                        )
                    )
                )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <service alias="Foobar" />
     */
    public static XmlAttributeValuePattern getServiceAliasPattern() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("alias")
                .withParent(
                    XmlPatterns.xmlTag().withName("service")
                )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * Possible service id completion on not ready type="service" argument
     *
     * <service><argument id="service_container" /></service>
     */
    public static XmlAttributeValuePattern getArgumentServiceIdForArgumentPattern() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("id")
                .withParent(XmlPatterns
                    .xmlTag().withParent(XmlPatterns
                        .xmlTag().withName("service"))
                )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <service id="service_container" />
     */
    public static XmlAttributeValuePattern getServiceIdAttributePattern() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                    .xmlAttribute("id")
                    .withParent(XmlPatterns
                            .xmlTag()
                            .withName("service")
                    )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <argument type="tagged" tag="twig.extension" />
     */
    public static XmlAttributeValuePattern getTypeTaggedTagAttribute() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("tag")
                .withParent(XmlPatterns
                    .xmlTag()
                    .withChild(XmlPatterns
                        .xmlAttribute("type")
                        .withValue(
                            StandardPatterns.string().equalTo("tagged")
                        )
                    )
                )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }
    
    /**
     * <factory service="factory_service" method="createFooMethod" />
     */
    public static XmlAttributeValuePattern getFactoryServiceCompletionPattern() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                 .xmlAttribute("service")
                 .withParent(XmlPatterns
                      .xmlTag().withName("factory")
                 )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <parameter key="fos_user.user_manager.class">FOS\UserBundle\Doctrine\UserManager</parameter>
     */
    public static PsiElementPattern.Capture<PsiElement> getParameterClassValuePattern() {
        // @TODO: check attribute value ends with ".class"
        return XmlPatterns
            .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
            .withText(StandardPatterns.string().contains("\\"))
            .withParent(XmlPatterns
                .xmlText()
                .withParent(XmlPatterns
                    .xmlTag()
                    .withName("parameter")
                    .withAnyAttribute("key")
                ).inside(
                    XmlHelper.getInsideTagPattern("services")
                )
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <argument>%form.resolved_type_factory.class%</argument>
     */
    public static PsiElementPattern.Capture<PsiElement> getArgumentValuePattern() {
        return XmlPatterns
            .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
            .withParent(XmlPatterns
                .xmlText()
                .withParent(XmlPatterns
                    .xmlTag()
                    .withName("argument")
                )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <argument type="constant">Foo\Bar::CONST</argument>
     */
    public static PsiElementPattern.Capture<PsiElement> getArgumentValueWithTypePattern(@NotNull String type) {
        return XmlPatterns
            .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
            .withParent(XmlPatterns
                .xmlText()
                .withParent(XmlPatterns
                    .xmlTag()
                    .withName("argument")
                    .withAttributeValue("type", type)
                )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <autowiring-type>Foo\Class</autowiring-type>
     */
    public static PsiElementPattern.Capture<PsiElement> getAutowiringTypePattern() {
        return XmlPatterns
            .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
            .withParent(XmlPatterns
                .xmlText()
                .withParent(XmlPatterns
                    .xmlTag()
                    .withName("autowiring-type")
                )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <service class="%foo.class%" id="required_attribute">
     */
    public static XmlAttributeValuePattern getServiceClassAttributeWithIdPattern() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("class")
                .withParent(XmlPatterns
                    .xmlTag()
                    .withChild(
                        XmlPatterns.xmlAttribute("id")
                    )
                )
            ).inside(
                XmlHelper.getInsideTagPattern("services")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    public static PsiFilePattern.Capture<PsiFile> getXmlFilePattern() {
        return XmlPatterns.psiFile()
            .withName(XmlPatterns
                .string().endsWith(".xml")
            );
    }

    /**
     * <import>
     *   <resource="@Foo"/>
     * </import>
     */
    public static XmlAttributeValuePattern getImportResourcePattern() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("resource")
                .withParent(XmlPatterns
                    .xmlTag().withName("import")
                )
            );
    }

    public static PsiElementPattern.Capture<XmlTag> getInsideTagPattern(String insideTagName) {
        return XmlPatterns.psiElement(XmlTag.class).withName(insideTagName);
    }

    public static PsiElementPattern.Capture<XmlTag> getInsideTagPattern(String... insideTagName) {
        return XmlPatterns.psiElement(XmlTag.class).withName(XmlPatterns.string().oneOf(insideTagName));
    }

    /**
     * <route id="foo" controller="Foo:Demo:hello"/>
     *
     * <route id="foo" path="/blog/{slug}">
     *  <default key="_controller">Foo:Demo:hello</default>
     * </route>
     */
    public static ElementPattern<PsiElement> getRouteControllerPattern() {
        return PlatformPatterns.or(getRouteControllerKeywordPattern(), getRouteDefaultWithKeyAttributePattern("_controller"));
    }

    /**
     * <route id="foo" controller="Foo:Demo:hello"/>
     */
    private static XmlAttributeValuePattern getRouteControllerKeywordPattern() {
        return XmlPatterns
            .xmlAttributeValue()
            .withParent(XmlPatterns
                .xmlAttribute("controller")
                .withParent(XmlPatterns
                    .xmlTag().withName("route")
                )
            ).inside(
                XmlHelper.getInsideTagPattern("route")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    /**
     * <route id="foo" path="/blog/{slug}">
     *  <default key="_controller">Foo:Demo:hello</default>
     * </route>
     */
    public static PsiElementPattern.Capture<PsiElement> getRouteDefaultWithKeyAttributePattern(@NotNull String key) {
        return XmlPatterns
            .psiElement(XmlTokenType.XML_DATA_CHARACTERS)
            .withParent(XmlPatterns
                .xmlText()
                .withParent(XmlPatterns
                    .xmlTag()
                    .withName("default")
                    .withChild(
                        XmlPatterns.xmlAttribute().withName("key").withValue(
                            XmlPatterns.string().oneOfIgnoreCase(key)
                        )
                    )
                )
            ).inside(
                XmlHelper.getInsideTagPattern("route")
            ).inFile(XmlHelper.getXmlFilePattern());
    }

    @Nullable
    public static PsiElement getLocalServiceName(PsiFile psiFile, String serviceName) {

        if(!(psiFile.getFirstChild() instanceof XmlDocument)) {
            return null;
        }

        XmlTag xmlTags[] = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
        if(xmlTags == null) {
            return null;
        }

        for(XmlTag xmlTag: xmlTags) {
            if(xmlTag.getName().equals("container")) {
                for(XmlTag servicesTag: xmlTag.getSubTags()) {
                    if(servicesTag.getName().equals("services")) {
                        for(XmlTag serviceTag: servicesTag.getSubTags()) {
                            String serviceNameId = serviceTag.getAttributeValue("id");
                            if(serviceNameId != null && serviceNameId.equalsIgnoreCase(serviceName)) {
                                return serviceTag;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    public static PsiElement getLocalParameterName(PsiFile psiFile, String serviceName) {

        if(!(psiFile.getFirstChild() instanceof XmlDocumentImpl)) {
            return null;
        }

        XmlTag xmlTags[] = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
        if(xmlTags == null) {
            return null;
        }

        for(XmlTag xmlTag: xmlTags) {
            if(xmlTag.getName().equals("container")) {
                for(XmlTag servicesTag: xmlTag.getSubTags()) {
                    if(servicesTag.getName().equals("parameters")) {
                        for(XmlTag serviceTag: servicesTag.getSubTags()) {
                            XmlAttribute attrValue = serviceTag.getAttribute("key");
                            if(attrValue != null) {
                                String serviceNameId = attrValue.getValue();
                                if(serviceNameId != null && serviceNameId.equals(serviceName)) {
                                    return serviceTag;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static Map<String, String> getFileParameterMap(XmlFile psiFile) {

        Map<String, String> services = new HashMap<>();

        if(!(psiFile.getFirstChild() instanceof XmlDocumentImpl)) {
            return services;
        }

        XmlTag xmlTags[] = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
        if(xmlTags == null) {
            return services;
        }

        for(XmlTag xmlTag: xmlTags) {
            if(xmlTag.getName().equals("container")) {
                for(XmlTag servicesTag: xmlTag.getSubTags()) {
                    if(servicesTag.getName().equals("parameters")) {
                        for(XmlTag parameterTag: servicesTag.getSubTags()) {

                            // <parameter key="fos_user.user_manager.class">FOS\UserBundle\Doctrine\UserManager</parameter>
                            // <parameter key="fos_rest.formats" type="collection">
                            //    <parameter key="json">false</parameter>
                            // </parameter>

                            if(parameterTag.getName().equals("parameter")) {
                                XmlAttribute keyAttr = parameterTag.getAttribute("key");
                                if(keyAttr != null) {
                                    String parameterName = keyAttr.getValue();
                                    if(parameterName != null && StringUtils.isNotBlank(parameterName)) {

                                        String parameterValue = null;

                                        String typeAttr = parameterTag.getAttributeValue("type");

                                        // get value of parameter if we have a text value
                                        if(!"collection".equals(typeAttr) && parameterTag.getSubTags().length == 0) {
                                            XmlTagValue attrClass = parameterTag.getValue();
                                            String myParameterValue = attrClass.getText();

                                            // dont index long values
                                            if(myParameterValue.length() < 150) {
                                                parameterValue = myParameterValue;
                                            }
                                        }

                                        services.put(parameterName.toLowerCase(), parameterValue);
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }

        return services;
    }


    /**
     * Get class attribute from service on every inside element
     *
     * @param psiInsideService every PsiElement inside service
     * @return raw class attribute value
     */
    @Nullable
    public static String getServiceDefinitionClass(@NotNull PsiElement psiInsideService) {

        // search for parent service definition
        XmlTag callXmlTag = PsiTreeUtil.getParentOfType(psiInsideService, XmlTag.class);
        XmlTag xmlTag = PsiTreeUtil.getParentOfType(callXmlTag, XmlTag.class);
        if(xmlTag == null || !xmlTag.getName().equals("service")) {
            return null;
        }

        return getClassFromServiceDefinition(xmlTag);
    }

    /**
     * Extract service class for class or id attribute on shortcut
     *
     * <service id="foo" class="Foobar"/> => Foobar
     * <service class="Foobar"/> => Foobar
     */
    @Nullable
    public static String getClassFromServiceDefinition(@NotNull XmlTag xmlTag) {
        String classAttribute = xmlTag.getAttributeValue("class");
        if(StringUtils.isNotBlank(classAttribute)) {
            return classAttribute;
        }

        String id = xmlTag.getAttributeValue("id");
        if(id == null || StringUtils.isBlank(id) || !YamlHelper.isClassServiceId(id)) {
            return null;
        }

        return id;
    }

    /**
     * Get class factory method attribute
     *
     * <factory class="FooBar" method="cre<caret>ate"/>
     */
    @Nullable
    public static PhpClass getPhpClassForClassFactory(@NotNull XmlAttributeValue xmlAttributeValue) {
        String method = xmlAttributeValue.getValue();
        if(StringUtils.isBlank(method)) {
            return null;
        }

        XmlTag parentOfType = PsiTreeUtil.getParentOfType(xmlAttributeValue, XmlTag.class);
        if(parentOfType == null) {
            return null;
        }

        String aClass = parentOfType.getAttributeValue("class");
        if(aClass == null || StringUtils.isBlank(aClass)) {
            return null;
        }

        return PhpElementsUtil.getClass(xmlAttributeValue.getProject(), aClass);
    }

    @Nullable
    public static PhpClass getPhpClassForServiceFactory(@NotNull XmlAttributeValue xmlAttributeValue) {
        String method = xmlAttributeValue.getValue();
        if(StringUtils.isBlank(method)) {
            return null;
        }

        XmlTag callXmlTag = PsiTreeUtil.getParentOfType(xmlAttributeValue, XmlTag.class);
        if(callXmlTag == null) {
            return null;
        }

        String service = callXmlTag.getAttributeValue("service");
        if(StringUtils.isBlank(service)) {
            return null;
        }

        return ServiceUtil.getResolvedClassDefinition(xmlAttributeValue.getProject(), service);
    }

    /**
     * <service id="app.newsletter_manager" class="AppBundle\Mail\NewsletterManager">
     *   <call method="setMailer">
     *     <argument type="service" id="mai<caret>ler" />
     *   </call>
     * </service>
     */
    public static void visitServiceCallArgument(@NotNull XmlAttributeValue xmlAttributeValue, @NotNull Consumer<ParameterVisitor> consumer) {
        // search for parent service definition
        PsiElement xmlAttribute = xmlAttributeValue.getParent();
        if(xmlAttribute instanceof XmlAttribute) {
            PsiElement xmlArgumentTag = xmlAttribute.getParent();
            if(xmlArgumentTag instanceof XmlTag) {
                PsiElement xmlCallTag = xmlArgumentTag.getParent();
                if(xmlCallTag instanceof XmlTag) {
                    String name = ((XmlTag) xmlCallTag).getName();
                    if (name.equals("call")) {
                        // service/call/argument[id]
                        XmlAttribute methodAttribute = ((XmlTag) xmlCallTag).getAttribute("method");
                        if(methodAttribute != null) {
                            String methodName = methodAttribute.getValue();
                            if(methodName != null) {
                                XmlTag serviceTag = ((XmlTag) xmlCallTag).getParentTag();
                                // get service class
                                if(serviceTag != null && "service".equals(serviceTag.getName())) {
                                    String className = XmlHelper.getClassFromServiceDefinition(serviceTag);
                                    if(className != null) {
                                        consumer.consume(new ParameterVisitor(
                                            className,
                                            methodName,
                                            getArgumentIndex((XmlTag) xmlArgumentTag)
                                        ));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Consumer for method parameter match
     *
     * service_name:
     *   class: FOOBAR
     *   calls:
     *      - [onFoobar, [@fo<caret>o]]
     */
    public static void visitServiceCallArgumentMethodIndex(@NotNull XmlAttributeValue xmlAttribute, @NotNull Consumer<Parameter> consumer) {
        visitServiceCallArgument(xmlAttribute, new ParameterResolverConsumer(xmlAttribute.getProject(), consumer));
    }


    /**
     * Find argument of given service method scope
     *
     * <service>
     *     <argument key="$foobar"/>
     *     <argument index="0"/>
     *     <argument/>
     *     <call method="foobar">
     *          <argument key="$foobar"/>
     *          <argument index="0"/>
     *          <argument/>
     *     </call>
     * </service>
     */
    public static int getArgumentIndex(@NotNull XmlTag argumentTag) {
        String indexAttr = argumentTag.getAttributeValue("index");
        if(indexAttr != null) {
            try {
                return Integer.valueOf(indexAttr);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        String keyAttr = argumentTag.getAttributeValue("key");
        if(keyAttr != null && keyAttr.length() > 1 && keyAttr.startsWith("$")) {
            PsiElement parentTag = argumentTag.getParent();
            if(parentTag instanceof XmlTag) {
                String name = ((XmlTag) parentTag).getName();

                if("service".equalsIgnoreCase(name)) {
                    // <service><argument/></service>
                    String aClass = XmlHelper.getClassFromServiceDefinition((XmlTag) parentTag);
                    if(aClass != null) {
                        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(argumentTag.getProject(), aClass);
                        if(phpClass != null) {
                            int parameter = PhpElementsUtil.getConstructorArgumentByName(phpClass, StringUtils.stripStart(keyAttr, "$"));
                            if(parameter >= 0) {
                                return parameter;
                            }
                        }
                    }
                } else if("call".equalsIgnoreCase(name)) {
                    // <service><call method="foobar"><argument/></call></service>
                    PsiElement serviceTag = parentTag.getParent();
                    if(serviceTag instanceof XmlTag && "service".equalsIgnoreCase(((XmlTag) serviceTag).getName())) {
                        String methodName = ((XmlTag) parentTag).getAttributeValue("method");
                        if(methodName != null && StringUtils.isNotBlank(methodName)) {
                            String aClass = XmlHelper.getClassFromServiceDefinition((XmlTag) serviceTag);
                            if(aClass != null) {
                                PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(argumentTag.getProject(), aClass);
                                if(phpClass != null) {
                                    Method methodByName = phpClass.findMethodByName(methodName);
                                    if(methodByName != null) {
                                        int parameter = PhpElementsUtil.getFunctionArgumentByName(methodByName, StringUtils.stripStart(keyAttr, "$"));
                                        if(parameter >= 0) {
                                            return parameter;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return getArgumentIndexByCount(argumentTag);
    }

    /**
     * Find argument of given service function / method scope
     *
     * <service>
     *     <argument key="$foobar"/>
     *     <argument index="0"/>
     *     <argument/>
     * </service>
     */
    public static int getArgumentIndex(@NotNull XmlTag argumentTag, @NotNull Function function) {
        String indexAttr = argumentTag.getAttributeValue("index");
        if(indexAttr != null) {
            try {
                return Integer.valueOf(indexAttr);
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        String keyAttr = argumentTag.getAttributeValue("key");
        if(keyAttr != null) {
            int parameter = PhpElementsUtil.getFunctionArgumentByName(function, StringUtils.stripStart(keyAttr, "$"));
            if(parameter >= 0) {
                return parameter;
            }
        }

        return getArgumentIndexByCount(argumentTag);
    }

    /**
     * Returns current index of parent tag
     *
     * <foo>
     *     <argument/>
     *     <arg<caret>ument/>
     * </foo>
     */
    private static int getArgumentIndexByCount(@NotNull XmlTag xmlTag) {
        PsiElement psiElement = xmlTag;
        int index = 0;

        while (psiElement != null) {
            psiElement = psiElement.getPrevSibling();
            // ignore: <argument index="0"/>, <argument key="$foobar"/>
            if(psiElement instanceof XmlTag && "argument".equalsIgnoreCase(((XmlTag) psiElement).getName()) && ((XmlTag) psiElement).getAttribute("key") == null && ((XmlTag) psiElement).getAttribute("index") == null) {
                index++;
            }
        }

        return index;
    }

    /**
     * Sine linemarker must be attached to leaf items, we add every navigation target onto the tag name. As XML_NAME
     * also indicate ending tag we need to make sure that only that starting is allowed for os
     *
     * Attach to "<service" but not to "</service":
     * <service class="Foobar"></service>
     */
    public static PsiElementPattern.Capture<PsiElement> getXmlTagNameLeafStartPattern() {
        return PlatformPatterns.psiElement(XmlElementType.XML_NAME)
            .afterLeaf(PlatformPatterns.psiElement(XmlTokenType.XML_START_TAG_START))
            .withParent(XmlTag.class);
    }

    public static boolean isXmlFileExtension(@NotNull PsiFile psiFile) {
        if (psiFile.getFileType() != XmlFileType.INSTANCE) {
            return false;
        }

        String filename = psiFile.getName();

        int i = filename.lastIndexOf('.');
        String extension = null;
        if (i > 0) {
            extension = filename.substring(i + 1).toLowerCase();
        }

        if ("xml".equalsIgnoreCase(extension)) {
            return true;
        }

        VirtualFile virtualFile = psiFile.getVirtualFile();
        return virtualFile == null || !"xml".equalsIgnoreCase(virtualFile.getExtension());
    }
}
