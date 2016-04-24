package fr.adrienbrault.idea.symfony2plugin.dic.container.util;

import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper;
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlServiceContainerAnnotator;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlAnnotator;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.AttributeValueInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.XmlTagAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.YamlKeyValueAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.visitor.ServiceConsumer;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerUtil {

    @NotNull
    public static Collection<ServiceInterface> getServicesInFile(@NotNull PsiFile psiFile) {
        final Collection<ServiceInterface> services = new ArrayList<ServiceInterface>();

        if(psiFile instanceof XmlFile) {
            visitFile((XmlFile) psiFile, new Consumer<ServiceConsumer>() {
                @Override
                public void consume(ServiceConsumer serviceConsumer) {
                    SerializableService serializableService = createService(serviceConsumer);
                    serializableService.setDecorationInnerName(serviceConsumer.attributes().getString("decoration-inner-name"));
                    serializableService.setIsDeprecated(serviceConsumer.attributes().getBoolean("deprecated"));

                    services.add(serializableService);
                }
            });
        }

        if(psiFile instanceof YAMLFile) {
            visitFile((YAMLFile) psiFile, new Consumer<ServiceConsumer>() {
                @Override
                public void consume(ServiceConsumer serviceConsumer) {

                    // alias inline "foo: @bar"
                    PsiElement yamlKeyValue = serviceConsumer.attributes().getPsiElement();
                    if(yamlKeyValue instanceof YAMLKeyValue) {
                        PsiElement value = ((YAMLKeyValue) yamlKeyValue).getValue();
                        if(value instanceof YAMLScalar) {
                            String valueText = ((YAMLScalar) value).getTextValue();
                            if(StringUtils.isNotBlank(valueText) && valueText.startsWith("@")) {
                                services.add(new SerializableService(serviceConsumer.getServiceId()).setAlias(valueText.substring(1)));
                                return;
                            }
                        }
                    }

                    SerializableService serializableService = createService(serviceConsumer);
                    serializableService.setDecorationInnerName(serviceConsumer.attributes().getString("decoration_inner_name"));

                    // catch: deprecated: ~
                    String string = serviceConsumer.attributes().getString("deprecated");
                    if("~".equals(string)) {
                        serializableService.setIsDeprecated(true);
                    } else {
                        serializableService.setIsDeprecated(serviceConsumer.attributes().getBoolean("deprecated"));
                    }

                    services.add(serializableService);
                }
            });
        }

        return services;
    }

    @NotNull
    private static SerializableService createService(@NotNull ServiceConsumer serviceConsumer) {
        AttributeValueInterface attributes = serviceConsumer.attributes();

        return new SerializableService(serviceConsumer.getServiceId())
            .setAlias(attributes.getString("alias"))
            .setClassName(attributes.getString("class"))
            .setDecorates(attributes.getString("decorates"))
            .setParent(attributes.getString("parent"))
            .setIsAbstract(attributes.getBoolean("abstract"))
            .setIsAutowire(attributes.getBoolean("autowrite"))
            .setIsLazy(attributes.getBoolean("lazy"))
            .setIsPublic(attributes.getBoolean("public"));
    }

    public static void visitFile(@NotNull YAMLFile psiFile, @NotNull Consumer<ServiceConsumer> consumer) {
        for (YAMLKeyValue keyValue : YamlHelper.getQualifiedKeyValuesInFile(psiFile, "services")) {
            String serviceId = keyValue.getKeyText();
            if(StringUtils.isBlank(serviceId)) {
                continue;
            }

            consumer.consume(new ServiceConsumer(keyValue, serviceId, new YamlKeyValueAttributeValue(keyValue)));
        }
    }

    public static void visitFile(@NotNull XmlFile psiFile, @NotNull Consumer<ServiceConsumer> consumer) {
        if(!(psiFile.getFirstChild() instanceof XmlDocument)) {
            return;
        }

        XmlTag xmlTags[] = PsiTreeUtil.getChildrenOfType(psiFile.getFirstChild(), XmlTag.class);
        if(xmlTags == null) {
            return;
        }

        for(XmlTag xmlTag: xmlTags) {
            if(xmlTag.getName().equals("container")) {
                for(XmlTag servicesTag: xmlTag.getSubTags()) {
                    if(servicesTag.getName().equals("services")) {
                        for(XmlTag serviceTag: servicesTag.getSubTags()) {
                            String serviceId = serviceTag.getAttributeValue("id");
                            if(StringUtils.isBlank(serviceId)) {
                                continue;
                            }

                            consumer.consume(new ServiceConsumer(serviceTag, serviceId, new XmlTagAttributeValue(serviceTag)));
                       }
                    }
                }
            }
        }
    }


    /**
     * foo:
     *  class: Foo
     *  arguments: [@<caret>]
     *  arguments:
     *      - @<caret>
     */
    @Nullable
    public static ServiceTypeHint getYamlConstructorTypeHint(@NotNull PsiElement psiElement, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        if (!YamlAnnotator.isStringValue(psiElement)) {
            return null;
        }

        // @TODO: simplify code checks

        PsiElement yamlScalar = psiElement.getContext();
        if(!(yamlScalar instanceof YAMLScalar)) {
            return null;
        }

        PsiElement context = yamlScalar.getContext();
        if(!(context instanceof YAMLSequenceItem)) {
            return null;
        }

        final YAMLSequenceItem sequenceItem = (YAMLSequenceItem) context;
        if (!(sequenceItem.getContext() instanceof YAMLSequence)) {
            return null;
        }

        final YAMLSequence yamlArray = (YAMLSequence) sequenceItem.getContext();
        if(!(yamlArray.getContext() instanceof YAMLKeyValue)) {
            return null;
        }

        final YAMLKeyValue yamlKeyValue = (YAMLKeyValue) yamlArray.getContext();
        if(!yamlKeyValue.getKeyText().equals("arguments")) {
            return null;
        }

        YAMLMapping parentMapping = yamlKeyValue.getParentMapping();
        if(parentMapping == null) {
            return null;
        }

        final YAMLKeyValue classKeyValue = parentMapping.getKeyValueByKey("class");
        if(classKeyValue == null) {
            return null;
        }

        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), classKeyValue.getValueText(), lazyServiceCollector);
        if(serviceClass == null) {
            return null;
        }

        Method constructor = serviceClass.getConstructor();
        if(constructor == null) {
            return null;
        }

        return new ServiceTypeHint(
            constructor,
            PsiElementUtils.getPrevSiblingsOfType(sequenceItem, PlatformPatterns.psiElement(YAMLSequenceItem.class)).size(),
            psiElement
        );
    }

    /**
     *  <services>
     *   <service class="Foo\\Bar\\Car">
     *    <argument type="service" id="<caret>" />
     *  </service>
     * </services>
     */
    @Nullable
    public static ServiceTypeHint getXmlConstructorTypeHint(@NotNull PsiElement psiElement, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        if(!(psiElement.getContainingFile() instanceof XmlFile) || psiElement.getNode().getElementType() != XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            return null;
        }

        XmlAttributeValue xmlAttributeValue = PsiTreeUtil.getParentOfType(psiElement, XmlAttributeValue.class);
        if(xmlAttributeValue == null) {
            return null;
        }

        XmlTag argumentTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        if(argumentTag == null) {
            return null;
        }

        XmlTag serviceTag = PsiElementAssertUtil.getParentOfTypeOrNull(argumentTag, XmlTag.class);
        if(serviceTag == null) {
            return null;
        }

        if(!serviceTag.getName().equals("service")) {
            return null;
        }

        // service/argument[id]
        String serviceDefName = serviceTag.getAttributeValue("class");
        if(serviceDefName != null) {
            PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefName);

            // check type hint on constructor
            if(phpClass != null) {
                Method constructor = phpClass.getConstructor();
                if(constructor != null) {
                    return new ServiceTypeHint(constructor, getArgumentIndex(argumentTag), psiElement);
                }
            }

        }

        return null;
    }

    /**
     *  <services>
     *   <service class="Foo\\Bar\\Car">
     *    <call method="foo"></call>
     *      <argument type="service" id="<caret>" />
     *    </call>
     *  </service>
     * </services>
     */
    @Nullable
    public static ServiceTypeHint getXmlCallTypeHint(@NotNull PsiElement psiElement, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        // search for parent service definition
        XmlTag currentXmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag.class);
        XmlTag parentXmlTag = PsiTreeUtil.getParentOfType(currentXmlTag, XmlTag.class);
        if(parentXmlTag == null) {
            return null;
        }

        String name = parentXmlTag.getName();
        if(!"call".equals(name)) {
            return null;
        }

        // service/call/argument[id]
        XmlAttribute methodAttribute = parentXmlTag.getAttribute("method");
        if(methodAttribute != null) {
            String methodName = methodAttribute.getValue();
            XmlTag serviceTag = parentXmlTag.getParentTag();

            // get service class
            if(serviceTag != null && "service".equals(serviceTag.getName())) {
                XmlAttribute classAttribute = serviceTag.getAttribute("class");
                if(classAttribute != null) {

                    String serviceDefName = classAttribute.getValue();
                    if(serviceDefName != null) {
                        PhpClass phpClass = ServiceUtil.getResolvedClassDefinition(psiElement.getProject(), serviceDefName);

                        // finally check method type hint
                        if(phpClass != null) {
                            Method method = phpClass.findMethodByName(methodName);
                            if(method != null) {
                                return new ServiceTypeHint(method, getArgumentIndex(currentXmlTag), psiElement);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static int getArgumentIndex(@NotNull XmlTag xmlTag) {

        PsiElement psiElement = xmlTag;
        int index = 0;

        while (psiElement != null) {
            psiElement = psiElement.getPrevSibling();
            if(psiElement instanceof XmlTag && "argument".equalsIgnoreCase(((XmlTag) psiElement).getName())) {
                index++;
            }
        }

        return index;
    }
}
