package fr.adrienbrault.idea.symfony2plugin.config.xml;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.PsiFilePattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.config.component.parser.ParameterServiceParser;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMap;
import fr.adrienbrault.idea.symfony2plugin.dic.ServiceMapParser;
import gnu.trove.THashSet;
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

    @Nullable
    public static ServiceMap getLocalMissingServiceMap(PsiElement psiElement,@Nullable Map<String, String> currentServiceMap) {
        try {
            VirtualFile virtualFile = psiElement.getContainingFile().getOriginalFile().getVirtualFile();
            if(virtualFile != null) {
                ServiceMap localServiceMap = new ServiceMapParser().parse(virtualFile.getInputStream());
                ServiceMap unknownServiceMap = new ServiceMap();
                for(Map.Entry<String, String> entry: localServiceMap.getPublicMap().entrySet()) {
                    if(currentServiceMap == null) {
                        unknownServiceMap.getMap().put(entry.getKey(), entry.getValue());
                    } else if ( !currentServiceMap.containsKey(entry.getKey())) {
                        unknownServiceMap.getMap().put(entry.getKey(), entry.getValue());
                    }
                }

                return unknownServiceMap;
            }

        } catch (SAXException ignored) {
        } catch (IOException ignored) {
        } catch (ParserConfigurationException ignored) {
        }

        return null;
    }

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

    public static Set<String> getLocalServices(PsiFile psiFile) {

        if(!(psiFile.getFirstChild() instanceof XmlDocumentImpl)) {
            return null;
        }

        XmlTag xmlTags[] = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
        if(xmlTags == null) {
            return null;
        }

        Set<String> services = new THashSet<String>();

        for(XmlTag xmlTag: xmlTags) {
            if(xmlTag.getName().equals("container")) {
                for(XmlTag servicesTag: xmlTag.getSubTags()) {
                    if(servicesTag.getName().equals("services")) {
                        for(XmlTag serviceTag: servicesTag.getSubTags()) {
                            XmlAttribute attrValue = serviceTag.getAttribute("id");
                            if(attrValue != null) {
                                String serviceNameId = attrValue.getValue();
                                if(serviceNameId != null) {
                                    services.add(serviceNameId);
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
