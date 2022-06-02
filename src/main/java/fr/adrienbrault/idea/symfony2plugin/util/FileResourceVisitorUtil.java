package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResourceContextTypeEnum;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

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
            YAMLKeyValue resourceKey = YamlHelper.getYamlKeyValue(yamlKeyValue, "resource", true);
            if(resourceKey == null) {
                continue;
            }

            String resource = PsiElementUtils.trimQuote(resourceKey.getValueText());
            if(StringUtils.isBlank(resource)) {
                continue;
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
                || YamlHelper.getYamlKeyValue(yamlKeyValue, "requirements", true) != null;

            if (isRouteContext) {
                fileResourceContextType = FileResourceContextTypeEnum.ROUTE;
            }

            consumer.consume(new FileResourceConsumer(resourceKey, normalize(resource), fileResourceContextType, map));
        }
    }

    /**
     * <routes><import resource="FOO" /></routes>
     */
    private static void visitXmlFile(@NotNull XmlFile psiFile, @NotNull Consumer<FileResourceConsumer> consumer) {
        XmlTag rootTag = psiFile.getRootTag();
        if(rootTag == null || !"routes".equals(rootTag.getName())) {
            return;
        }

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
