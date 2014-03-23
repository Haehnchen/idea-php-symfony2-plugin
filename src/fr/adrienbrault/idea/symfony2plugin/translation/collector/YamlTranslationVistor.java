package fr.adrienbrault.idea.symfony2plugin.translation.collector;


import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.psi.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class YamlTranslationVistor {

    public static void collectFileTranslations(YAMLFile yamlFile, YamlTranslationCollector translationCollector) {
        List<YAMLDocument> documents = yamlFile.getDocuments();

        if(documents.size() < 1) {
            return;
        }

        Collection<YAMLKeyValue> yamlKeyValues = PsiTreeUtil.getChildrenOfTypeAsList(documents.get(0), YAMLKeyValue.class);
        for(YAMLKeyValue yamlKeyValue: yamlKeyValues) {
            collectItems(yamlKeyValue, translationCollector);
        }

    }

    private static void collectNextLevelElements(YAMLCompoundValue yamlCompoundValue, List<String> levels, YamlTranslationCollector translationCollector) {
        Collection<YAMLKeyValue> yamlKeyValues = PsiTreeUtil.getChildrenOfTypeAsList(yamlCompoundValue, YAMLKeyValue.class);
        for(YAMLKeyValue yamlKeyValue: yamlKeyValues) {
            if(!collectItems(levels, yamlKeyValue, translationCollector)) {
                return;
            }
        }
    }

    private static void collectItems(YAMLKeyValue yamlKeyValue, YamlTranslationCollector translationCollector ) {
        collectItems(new ArrayList<String>(), yamlKeyValue, translationCollector);
    }

    private static boolean collectItems(List<String> levels, YAMLKeyValue yamlKeyValue, YamlTranslationCollector translationCollector) {

        List<YAMLPsiElement> childElements = yamlKeyValue.getYAMLElements();
        String keyText = yamlKeyValue.getKeyText();
        if(StringUtils.isBlank(keyText)) {
            return true;
        }

        // we check again after cleanup
        keyText = keyNormalizer(keyText);
        if(StringUtils.isBlank(keyText)) {
            return true;
        }

        // yaml key-value provide main psielement in last child element
        // depending of what we get here we have another key-value inside, multiline or string value
        if(childElements.size() == 1) {


            PsiElement lastChildElement = childElements.get(0);

            // catch next level keys
            if(lastChildElement instanceof YAMLCompoundValue) {

                // use copy of current level and pipe to children call
                ArrayList<String> copyLevels = new ArrayList<String>(levels);
                copyLevels.add(keyText);

                collectNextLevelElements((YAMLCompoundValue) childElements.get(0), copyLevels, translationCollector);
                return true;
            }

            // check multiline is also an tree end indicator
            // stats: |
            // accomplishment: >
            if((lastChildElement instanceof YAMLScalarText || lastChildElement instanceof YAMLScalarList)) {
                return callCollectCallback(levels, yamlKeyValue, translationCollector, keyText);
            }

        } else {
            return callCollectCallback(levels, yamlKeyValue, translationCollector, keyText);
        }

        return true;
    }

    private static boolean callCollectCallback(List<String> levels, YAMLKeyValue yamlKeyValue, YamlTranslationCollector translationCollector, String keyText) {
        ArrayList<String> copyLevels = new ArrayList<String>(levels);
        copyLevels.add(keyText);

        return translationCollector.collect(StringUtils.join(copyLevels, "."), yamlKeyValue);
    }

    /**
     * Translation key allow quoted values and also space before and after
     */
    private static String keyNormalizer(String keyName) {
        return StringUtils.trim(StringUtils.strip(StringUtils.strip(keyName, "'"), "\""));
    }

}
