package fr.adrienbrault.idea.symfony2plugin.dic.container.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.AttributeValueInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.XmlTagAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.YamlKeyValueAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.visitor.ServiceConsumer;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.Collection;

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
                        if(value instanceof LeafPsiElement) {
                            String valueText = ((YAMLKeyValue) yamlKeyValue).getValueText();
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
        YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
        if(yamlDocument == null) {
            return;
        }

        // get services or parameter key
        YAMLKeyValue[] yamlKeys = PsiTreeUtil.getChildrenOfType(yamlDocument, YAMLKeyValue.class);
        if(yamlKeys == null) {
            return;
        }

        for(YAMLKeyValue yamlKeyValue : yamlKeys) {
            String yamlConfigKey = yamlKeyValue.getName();
            if(yamlConfigKey == null || !yamlConfigKey.equals("services")) {
                continue;
            }

            for (YAMLKeyValue keyValue : PsiTreeUtil.getChildrenOfTypeAsList(yamlKeyValue.getValue(), YAMLKeyValue.class)) {
                String serviceId = keyValue.getKeyText();
                if(serviceId == null) {
                    continue;
                }

                consumer.consume(new ServiceConsumer(keyValue, serviceId, new YamlKeyValueAttributeValue(keyValue)));
            }
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
}
