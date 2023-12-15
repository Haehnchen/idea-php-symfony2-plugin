package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResourceContextTypeEnum;
import fr.adrienbrault.idea.symfony2plugin.util.resource.FileResourceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.*;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResourceVisitorUtil {
    public static void visitFile(@NotNull PsiFile psiFile, @NotNull Consumer<FileResourceConsumer> consumer) {
        if(psiFile instanceof XmlFile) {
            visitXmlFile((XmlFile) psiFile, consumer);
        } else if(psiFile instanceof YAMLFile) {
            visitYamlFile((YAMLFile) psiFile, consumer);
        }
    }

    /**
     * foo:
     *   resources: 'FOO'
     */
    private static void visitYamlFile(@NotNull YAMLFile yamlFile, @NotNull Consumer<FileResourceConsumer> consumer) {
        for (YAMLKeyValue yamlKeyValue : YamlHelper.getTopLevelKeyValues(yamlFile)) {
            // imports:
            //   - { resource: ../src/import/services.yml, ignore_errors: true }
            String keyText = yamlKeyValue.getKeyText();
            if ("imports".equals(keyText)) {
                YAMLValue value = yamlKeyValue.getValue();
                if (value instanceof YAMLSequence) {
                    for (YAMLSequenceItem item : ((YAMLSequence) value).getItems()) {
                        YAMLValue value1 = item.getValue();
                        if (value1 instanceof YAMLMapping) {
                            String resource = YamlHelper.getYamlKeyValueAsString((YAMLMapping) value1, "resource");
                            if (resource != null  && resource.length() < 128) {
                                consumer.consume(new FileResourceConsumer(value1, normalize(resource), FileResourceContextTypeEnum.UNKNOWN, new HashMap<>()));
                            }
                        }
                    }
                }
            }

            if (keyText.startsWith("when@")) {
                YAMLValue value = yamlKeyValue.getValue();
                if (value instanceof YAMLMapping) {
                    for (YAMLKeyValue yamlKeyValue2 : ((YAMLMapping) value).getKeyValues()) {
                        visitKeyValue(yamlKeyValue2, consumer);
                    }
                }
            }

            visitKeyValue(yamlKeyValue, consumer);
        }
    }

    private static void visitKeyValue(@NotNull YAMLKeyValue yamlKeyValue, @NotNull Consumer<FileResourceConsumer> consumer) {
        // app1:
        //   resource: "@AcmeOtherBundle/Resources/config/routing1.yml"
        //   resource:
        //      path: foo

        YAMLKeyValue resourceYamlKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue, "resource");
        if (resourceYamlKeyValue == null) {
            return;
        }

        String resource = FileResourceUtil.getResourcePath(yamlKeyValue);
        if (resource == null) {
            return;
        }

        FileResourceContextTypeEnum fileResourceContextType = FileResourceContextTypeEnum.UNKNOWN;

        Map<String, String> map = new HashMap<>();
        for (String option: new String[] {"type", "prefix", "name_prefix"}) {
            String attributeValue = YamlHelper.getYamlKeyValueAsString(yamlKeyValue, option, true);
            if (StringUtils.isNotBlank(attributeValue) && attributeValue.length() < 128) {
                map.put(option, attributeValue);
            }
        }

        boolean isRouteContext = map.containsKey("type")
            || map.containsKey("prefix")
            || map.containsKey("name_prefix")
            || YamlHelper.getYamlKeyValue(yamlKeyValue, "requirements", true) != null
            || YamlHelper.getYamlKeyValue(resourceYamlKeyValue, "namespace", true) != null;

        if (isRouteContext) {
            fileResourceContextType = FileResourceContextTypeEnum.ROUTE;
        }

        consumer.consume(new FileResourceConsumer(resourceYamlKeyValue, normalize(resource), fileResourceContextType, map));
    }

    /**
     * <routes><import resource="FOO" /></routes>
     */
    private static void visitXmlFile(@NotNull XmlFile psiFile, @NotNull Consumer<FileResourceConsumer> consumer) {
        XmlTag rootTag = psiFile.getRootTag();
        if (rootTag == null) {
            return;
        }

        String rootTagName = rootTag.getName();
        if ("routes".equals(rootTagName)) {
            for (XmlTag xmlTag : rootTag.findSubTags("import")) {
                String resource = xmlTag.getAttributeValue("resource");
                if(StringUtils.isBlank(resource)) {
                    continue;
                }

                Map<String, String> map = new HashMap<>();
                for (String option: new String[] {"type", "prefix", "name-prefix"}) {
                    String attributeValue = xmlTag.getAttributeValue(option);
                    if (StringUtils.isNotBlank(attributeValue) && attributeValue.length() < 128) {
                        map.put(option.replace("-", "_"), attributeValue);
                    }
                }

                consumer.consume(new FileResourceConsumer(xmlTag, normalize(resource), FileResourceContextTypeEnum.ROUTE, map));
            }
        } else if("container".equals(rootTagName)) {
            for (XmlTag xmlTag : rootTag.findSubTags("imports")) {
                for (XmlTag anImport : xmlTag.findSubTags("import")) {
                    String attributeValue = anImport.getAttributeValue("resource");
                    if (StringUtils.isNotBlank(attributeValue) && attributeValue.length() < 128) {
                        consumer.consume(new FileResourceConsumer(xmlTag, normalize(attributeValue), FileResourceContextTypeEnum.CONTAINER, new HashMap<>()));
                    }
                }
            }
        }
    }

    @NotNull
    public static String normalize(@NotNull String resource) {
        return StringUtils.stripEnd(resource.replace("\\", "/").replaceAll("/+", "/"), "/");
    }

     public static class FileResourceConsumer {
         @NotNull
         private final PsiElement psiElement;

         @NotNull
         private final String resource;

         @NotNull
         private final FileResourceContextTypeEnum contextType;

         @NotNull
         private final Map<String, String> contextValues;

         public FileResourceConsumer(@NotNull PsiElement target, @NotNull String resource, @NotNull FileResourceContextTypeEnum fileResourceContextTypeEnum, @NotNull Map<String, String> contextValues) {
             this.psiElement = target;
             this.resource = resource;
             this.contextType = fileResourceContextTypeEnum;
             this.contextValues = contextValues;
         }

         @NotNull
         public String getResource() {
             return resource;
         }

         @NotNull
         public PsiElement getPsiElement() {
             return psiElement;
         }

         @NotNull
         public FileResourceContextTypeEnum getContextType() {
             return contextType;
         }

         @NotNull
         public FileResource createFileResource() {
             return new FileResource(this.getResource(), this.getContextType(), new TreeMap<>(this.contextValues));
         }
     }
}
