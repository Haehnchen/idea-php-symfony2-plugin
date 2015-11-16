package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.AttributeValueInterface;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.DummyAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.XmlTagAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.dic.attribute.value.YamlKeyValueAttributeValue;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.FileResource;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

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
        YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(yamlFile, YAMLDocument.class);
        if(yamlDocument == null) {
            return;
        }

        for (YAMLKeyValue yamlKeyValue : PsiTreeUtil.getChildrenOfTypeAsList(yamlDocument, YAMLKeyValue.class)) {
            YAMLKeyValue resourceKey = YamlHelper.getYamlKeyValue(yamlKeyValue, "resource", true);
            if(resourceKey == null) {
                continue;
            }

            String resource = PsiElementUtils.trimQuote(resourceKey.getValueText());
            if(StringUtils.isBlank(resource)) {
                continue;
            }

            consumer.consume(new FileResourceConsumer(resourceKey, yamlKeyValue, normalize(resource)));
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

            consumer.consume(new FileResourceConsumer(xmlTag, xmlTag, normalize(resource)));
        }
    }

    @NotNull
    public static String normalize(@NotNull String resource) {
        return StringUtils.stripEnd(resource.replace("\\", "/").replaceAll("/+", "/"), "/");
    }

     public static class FileResourceConsumer {

         @NotNull
         private final PsiElement psiElement;

         @Nullable
         private AttributeValueInterface attributeValue = null;

         @NotNull
         private final PsiElement scope;
         @NotNull
         private final String resource;

         public FileResourceConsumer(@NotNull PsiElement target, @NotNull PsiElement scope, @NotNull String resource) {
             this.psiElement = target;
             this.scope = scope;
             this.resource = resource;
         }

         @NotNull
         public AttributeValueInterface getAttributeValue() {
             if(this.attributeValue != null) {
                 return this.attributeValue;
             }

             // We use lazy instances
             // @TODO: replace with factory pattern
             if(this.psiElement instanceof YAMLKeyValue) {
                 return this.attributeValue = new YamlKeyValueAttributeValue((YAMLKeyValue) this.scope);
             } else if(this.psiElement instanceof XmlTag) {
                 return this.attributeValue = new XmlTagAttributeValue((XmlTag) this.scope);
             }

             return this.attributeValue = new DummyAttributeValue();
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
         public FileResource createFileResource() {
             FileResource fileResource = new FileResource(this.getResource());
             String prefix = this.getAttributeValue().getString("prefix");
             if(prefix != null) {
                 fileResource.setPrefix(prefix);
             }

             return fileResource;
         }
     }
}
