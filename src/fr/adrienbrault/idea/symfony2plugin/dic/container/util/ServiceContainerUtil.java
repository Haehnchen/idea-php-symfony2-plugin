package fr.adrienbrault.idea.symfony2plugin.dic.container.util;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.*;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlAnnotator;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.AttributeValueInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.XmlTagAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.YamlKeyValueAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.container.SerializableService;
import fr.adrienbrault.idea.symfony2plugin.dic.container.ServiceSerializable;
import fr.adrienbrault.idea.symfony2plugin.dic.container.dict.ServiceTypeHint;
import fr.adrienbrault.idea.symfony2plugin.dic.container.visitor.ServiceConsumer;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.ContainerIdUsagesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
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
import java.util.Comparator;
import java.util.List;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ServiceContainerUtil {

    private static String[] LOWER_PRIORITY = new String[] {
        "debug", "default", "abstract", "inner", "chain", "decorate", "delegat"
    };

    @NotNull
    public static Collection<ServiceSerializable> getServicesInFile(@NotNull PsiFile psiFile) {
        final Collection<ServiceSerializable> services = new ArrayList<>();

        if(psiFile instanceof XmlFile) {
            visitFile((XmlFile) psiFile, serviceConsumer -> {
                SerializableService serializableService = createService(serviceConsumer);
                serializableService.setDecorationInnerName(serviceConsumer.attributes().getString("decoration-inner-name"));
                serializableService.setIsDeprecated(serviceConsumer.attributes().getBoolean("deprecated"));

                services.add(serializableService);
            });
        } else if (psiFile instanceof YAMLFile) {
            visitFile((YAMLFile) psiFile, serviceConsumer -> {

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
            });
        }

        // decorated services
        services.addAll(getPseudoDecoratedServices(services));

        return services;
    }

    /**
     * "espend.my_next_foo" > "espend.my_next_foo.inner" or custom inner name
     */
    @NotNull
    private static Collection<ServiceSerializable> getPseudoDecoratedServices(@NotNull Collection<ServiceSerializable> services) {
        Collection<ServiceSerializable> decoratedServices = new ArrayList<>();

        for (ServiceSerializable service : services) {
            String decorates = service.getDecorates();
            if(decorates == null || StringUtils.isBlank(decorates)) {
                continue;
            }

            String decorationInnerName = service.getDecorationInnerName();
            if(StringUtils.isBlank(decorationInnerName)) {
                decorationInnerName = service.getId() + ".inner";
            }

            decoratedServices.add(new SerializableService(decorationInnerName));
        }

        return decoratedServices;
    }

    @NotNull
    private static SerializableService createService(@NotNull ServiceConsumer serviceConsumer) {
        AttributeValueInterface attributes = serviceConsumer.attributes();

        Boolean anAbstract = attributes.getBoolean("abstract");
        String aClass = StringUtils.stripStart(attributes.getString("class"), "\\");
        if(aClass == null && isServiceIdAsClassSupported(attributes, anAbstract)) {
            // if no "class" given since Syfmony 3.3 we have lowercase "id" names
            // as we internally use case insensitive maps; add user provided values
            aClass = serviceConsumer.getServiceId();
        }

        return new SerializableService(serviceConsumer.getServiceId())
            .setAlias(attributes.getString("alias"))
            .setClassName(aClass)
            .setDecorates(attributes.getString("decorates"))
            .setParent(attributes.getString("parent"))
            .setIsAbstract(anAbstract)
            .setIsAutowire(attributes.getBoolean("autowrite"))
            .setIsLazy(attributes.getBoolean("lazy"))
            .setIsPublic(attributes.getBoolean("public"));
    }

    /**
     * Service definition allows "id" to "class" transformation: eg not an alias or abstract service
     */
    private static boolean isServiceIdAsClassSupported(@NotNull AttributeValueInterface attributes, @Nullable Boolean anAbstract) {
        return attributes.getString("alias") == null && !(anAbstract != null && anAbstract);
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

        return getYamlConstructorTypeHint((YAMLScalar) yamlScalar, lazyServiceCollector);
    }

    /**
     * foo:
     *  class: Foo
     *  arguments: [@<caret>]
     *  arguments:
     *      - @<caret>
     */
    @Nullable
    public static ServiceTypeHint getYamlConstructorTypeHint(@NotNull YAMLScalar yamlScalar, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
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

        PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(yamlScalar.getProject(), classKeyValue.getValueText(), lazyServiceCollector);
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
            yamlScalar
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

    /**
     * Foobar::CONST
     */
    @NotNull
    public static Collection<PsiElement> getTargetsForConstant(@NotNull Project project, @NotNull String contents) {
        // FOO
        if (!contents.contains(":")) {
            if(!contents.startsWith("\\")) {
                contents = "\\" + contents;
            }

            return new ArrayList<>(
                PhpIndex.getInstance(project).getConstantsByFQN(contents)
            );
        }

        contents = contents.replaceAll(":+", ":");
        String[] split = contents.split(":");

        Collection<PsiElement> psiElements = new ArrayList<>();
        for (PhpClass phpClass : PhpElementsUtil.getClassesInterface(project, split[0])) {
            Field fieldByName = phpClass.findFieldByName(split[1], true);
            if(fieldByName != null && fieldByName.isConstant()) {
                psiElements.add(fieldByName);
            }
        }

        return psiElements;
    }

    /**
     * Calculate usage as of given service id in project scope
     */
    public static int getServiceUsage(@NotNull Project project, @NotNull String id) {
        int usage = 0;

        List<Integer> values = FileBasedIndex.getInstance().getValues(ContainerIdUsagesStubIndex.KEY, id, GlobalSearchScope.allScope(project));
        for (Integer integer : values) {
            usage += integer;
        }

        return usage;
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

    public static boolean isLowerPriority(String name) {
        for(String lowerName: LOWER_PRIORITY) {
            if(name.contains(lowerName)) {
                return true;
            }
        }

        return false;
    }

    public static class ContainerServiceIdPriorityNameComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {

            if(isLowerPriority(o1) && isLowerPriority(o2)) {
                return 0;
            }

            return isLowerPriority(o1) ? 1 : -1;
        }
    }

    @NotNull
    public static List<String> getSortedServiceId(@NotNull Project project, @NotNull Collection<String> ids) {
        if(ids.size() == 0) {
            return new ArrayList<>(ids);
        }

        List<String> myIds = new ArrayList<>(ids);

        myIds.sort(new ServiceContainerUtil.ContainerServiceIdPriorityNameComparator());

        myIds.sort((o1, o2) ->
            ((Integer) ServiceContainerUtil.getServiceUsage(project, o2))
                .compareTo(ServiceContainerUtil.getServiceUsage(project, o1))
        );

        return myIds;
    }
}
