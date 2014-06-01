package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMapParser;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    public static PsiElementPattern.Capture<XmlTag> getInsideTagPattern(String insideTagName) {
        return XmlPatterns.psiElement(XmlTag.class).withName(insideTagName);
    }

    public static PsiElementPattern.Capture<XmlTag> getInsideTagPattern(String... insideTagName) {
        return XmlPatterns.psiElement(XmlTag.class).withName(XmlPatterns.string().oneOf(insideTagName));
    }

    @Deprecated
    @Nullable
    public static HashMap<String, String> getLocalMissingParameterMap(PsiElement psiElement,@Nullable Map<String, String> currentServiceMap) {

        VirtualFile virtualFile = psiElement.getContainingFile().getOriginalFile().getVirtualFile();
        if(virtualFile != null) {
            ParameterServiceParser parameterServiceParser = new ParameterServiceParser();
            parameterServiceParser.parser(VfsUtil.virtualToIoFile(virtualFile));
            HashMap<String, String> unknownParameterMap = new HashMap<String, String>();

            for(Map.Entry<String, String> entry: parameterServiceParser.getParameterMap().entrySet()) {
                if(currentServiceMap == null) {
                    unknownParameterMap.put(entry.getKey(), entry.getValue());
                } else if ( !currentServiceMap.containsKey(entry.getKey())) {
                    unknownParameterMap.put(entry.getKey(), entry.getValue());
                }
            }

            return unknownParameterMap;
        }


        return null;
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

    @Nullable
    public static Map<String, ContainerService> getLocalServiceMap(PsiFile psiFile) {

        Map<String, ContainerService> services = new HashMap<String, ContainerService>();

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
                    if(servicesTag.getName().equals("services")) {
                        for(XmlTag serviceTag: servicesTag.getSubTags()) {
                            XmlAttribute attrValue = serviceTag.getAttribute("id");
                            if(attrValue != null) {

                                // <service id="foo.bar" class="Class\Name">
                                String serviceNameId = attrValue.getValue();
                                if(serviceNameId != null) {

                                    // <service ... class="%doctrine.orm.proxy_cache_warmer.class%">
                                    String serviceClassName = null;
                                    XmlAttribute attrClass = serviceTag.getAttribute("class");
                                    if(attrClass != null) {
                                        serviceClassName = attrClass.getValue();
                                    }

                                    // <service ... public="false" />
                                    boolean isPrivate = false;
                                    XmlAttribute publicAttr = serviceTag.getAttribute("public");
                                    if(publicAttr != null && "false".equals(publicAttr.getValue())) {
                                        isPrivate = true;
                                    }

                                    // <service id="doctrine.orm.metadata.annotation_reader" alias="annotation_reader"/>
                                    // @TODO: resolve alias
                                    XmlAttribute attrAlias = serviceTag.getAttribute("alias");
                                    if(attrAlias != null && attrAlias.getValue() != null) {
                                        serviceNameId = attrAlias.getValue();

                                        // if aliased service is in current file use value; not nice here but a simple workaround
                                        if(serviceClassName == null && services.containsKey(serviceNameId)) {
                                            serviceClassName = services.get(serviceNameId).getClassName();
                                        }
                                    }

                                    if(StringUtils.isNotBlank(serviceNameId)) {
                                        services.put(serviceNameId, new ContainerService(serviceNameId, serviceClassName, true, isPrivate));
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

    public static Map<String, String> getFileParameterMap(XmlFile psiFile) {

        Map<String, String> services = new HashMap<String, String>();

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

                                        XmlAttribute typeAttr = parameterTag.getAttribute("type");
                                        if(typeAttr == null || !"collection".equals(typeAttr.getValue())) {
                                            XmlTagValue attrClass = parameterTag.getValue();
                                            parameterValue = attrClass.getText();
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

}
