package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
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
    public static XmlAttributeValuePattern getServiceIdNamePattern() {
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
     * <service class="%foo.class%">
     */
    public static XmlAttributeValuePattern getServiceIdPattern() {
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
                    if(servicesTag.getName().equals("services")) {
                        for(XmlTag serviceTag: servicesTag.getSubTags()) {
                            XmlAttribute attrValue = serviceTag.getAttribute("id");
                            if(attrValue != null) {
                                String serviceNameId = attrValue.getValue();
                                if(serviceNameId != null && serviceNameId.equalsIgnoreCase(serviceName)) {
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
    public static String getServiceDefinitionClass(PsiElement psiInsideService) {

        // search for parent service definition
        XmlTag callXmlTag = PsiTreeUtil.getParentOfType(psiInsideService, XmlTag.class);
        XmlTag xmlTag = PsiTreeUtil.getParentOfType(callXmlTag, XmlTag.class);
        if(xmlTag == null || !xmlTag.getName().equals("service")) {
            return null;
        }

        XmlAttribute classAttribute = xmlTag.getAttribute("class");
        if(classAttribute == null) {
            return null;
        }

        String value = classAttribute.getValue();
        if(StringUtils.isNotBlank(value)) {
            return value;
        }

        return null;
    }


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
}
