package fr.adrienbrault.idea.symfony2plugin.util;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class FileResourceVisitorUtil {

    public static void visitFile(@NotNull PsiFile psiFile, @NotNull Processor<Pair<String, PsiElement>> processor) {
        if(psiFile instanceof XmlFile) {
            visitXmlFile((XmlFile) psiFile, processor);
        } else if(psiFile instanceof YAMLFile) {
            visitYamlFile((YAMLFile) psiFile, processor);
        }
    }

    /**
     * foo:
     *   resources: 'FOO'
     */
    private static void visitYamlFile(@NotNull YAMLFile yamlFile, @NotNull Processor<Pair<String, PsiElement>> processor) {
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

            processor.process(Pair.<String, PsiElement>create(normalize(resource), resourceKey));
        }
    }

    /**
     * <routes><import resource="FOO" /></routes>
     */
    private static void visitXmlFile(@NotNull XmlFile psiFile, @NotNull Processor<Pair<String, PsiElement>> processor) {
        XmlTag rootTag = psiFile.getRootTag();
        if(rootTag == null || !"routes".equals(rootTag.getName())) {
            return;
        }

        for (XmlTag xmlTag : rootTag.findSubTags("import")) {
            String resource = xmlTag.getAttributeValue("resource");
            if(StringUtils.isBlank(resource)) {
                continue;
            }

            processor.process(Pair.<String, PsiElement>create(normalize(resource), xmlTag));
        }
    }

    @NotNull
    public static String normalize(@NotNull String resource) {
        return StringUtils.stripEnd(resource.replace("\\", "/").replaceAll("/+", "/"), "/");
    }
}
