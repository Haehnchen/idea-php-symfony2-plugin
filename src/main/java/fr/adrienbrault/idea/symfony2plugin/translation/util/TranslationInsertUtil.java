package fr.adrienbrault.idea.symfony2plugin.translation.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementFactory;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.YAMLFile;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.function.Function;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TranslationInsertUtil {
    @Nullable
    public static PsiElement invokeTranslation(@NotNull final PsiFile psiFile, @NotNull final String keyName, @NotNull final String translation) {
        if(psiFile instanceof YAMLFile) {
            return invokeTranslation((YAMLFile) psiFile, keyName, translation);
        } else if(psiFile instanceof XmlFile && TranslationUtil.isSupportedXlfFile(psiFile)) {
            return invokeTranslation((XmlFile) psiFile, keyName, translation);
        }

        return null;
    }

    private static PsiElement invokeTranslation(@NotNull final YAMLFile yamlFile, @NotNull final String keyName, @NotNull final String translation) {
        String[] split = keyName.split("\\.");
        PsiElement psiElement = YamlHelper.insertKeyIntoFile(yamlFile, "'" + translation + "'", split);
        if(psiElement == null) {
            return null;
        }

        // resolve target to get value
        YAMLKeyValue target = YAMLUtil.getQualifiedKeyInFile(yamlFile, split);
        if(target != null && target.getValue() != null) {
            return target.getValue();
        } else if(target != null) {
            return target;
        }

        return yamlFile;
    }

    @Nullable
    public static XmlTag invokeTranslation(@NotNull final XmlFile xmlFile, @NotNull final String keyName, @NotNull final String translation) {
        XmlTag rootTag = xmlFile.getRootTag();
        if(rootTag == null) {
            return null;
        }

        String version = rootTag.getAttributeValue("version");
        if(version == null) {
            return null;
        }

        XmlTag file = rootTag.findFirstSubTag("file");
        if(file == null) {
            return null;
        }

        // version="1.2"
        if(version.equalsIgnoreCase("1.2")) {
            Function<XmlTag, XmlTag> func12 = body -> {
                XmlElementFactory instance = XmlElementFactory.getInstance(xmlFile.getProject());

                XmlTag source = instance.createTagFromText("<source/>");
                source.getValue().setText(keyName);

                XmlTag target = instance.createTagFromText("<target/>");
                target.getValue().setText(translation);

                XmlTag transUnit = instance.createTagFromText("<trans-unit/>");
                transUnit.setAttribute("id", String.valueOf(getIdForNewXlfUnit(body, "trans-unit")));

                transUnit.addSubTag(source, false);
                transUnit.addSubTag(target, false);

                return body.addSubTag(transUnit, false);
            };

            XmlTag body = file.findFirstSubTag("body");
            if(body != null) {
                return func12.apply(body);
            }
        } else if(version.equalsIgnoreCase("2.0")) {
            Function<XmlTag, XmlTag> func20 = body -> {
                XmlElementFactory instance = XmlElementFactory.getInstance(xmlFile.getProject());

                XmlTag source = instance.createTagFromText("<source/>");
                source.getValue().setText(keyName);

                XmlTag target = instance.createTagFromText("<target/>");
                target.getValue().setText(translation);

                XmlTag transUnit = instance.createTagFromText("<unit/>");
                transUnit.setAttribute("id", String.valueOf(String.valueOf(getIdForNewXlfUnit(body, "unit"))));

                XmlTag segment = transUnit.addSubTag(instance.createTagFromText("<segment/>"), false);

                segment.addSubTag(source, false);
                segment.addSubTag(target, false);

                return body.addSubTag(transUnit, false);
            };

            // version="2.0"
            XmlTag group = file.findFirstSubTag("group");
            if(group != null) {
                return func20.apply(group);
            } else {
                // version="2.0" shortcut
                return func20.apply(file);
            }
        }

        return null;
    }

    private static int getIdForNewXlfUnit(@NotNull XmlTag body, @NotNull String subTag) {
        int lastId = 0;

        for (XmlTag transUnit : body.findSubTags(subTag)) {
            String id = transUnit.getAttributeValue("id");
            if(id == null) {
                continue;
            }

            Integer integer;
            try {
                integer = Integer.valueOf(id);
            } catch (NumberFormatException e) {
                continue;
            }

            // next safe id
            if(integer >= lastId) {
                lastId = integer + 1;
            }
        }

        return lastId;
    }

    /**
     * Remove TODO; moved to core
     */
    @Deprecated
    @NotNull
    public static String findEol(@NotNull PsiElement psiElement) {

        for(PsiElement child: YamlHelper.getChildrenFix(psiElement)) {
            if(PlatformPatterns.psiElement(YAMLTokenTypes.EOL).accepts(child)) {
                return child.getText();
            }
        }

        PsiElement[] indentPsiElements = PsiTreeUtil.collectElements(psiElement.getContainingFile(), element ->
            PlatformPatterns.psiElement(YAMLTokenTypes.EOL).accepts(element)
        );

        if(indentPsiElements.length > 0) {
            return indentPsiElements[0].getText();
        }

        return "\n";
    }
}
