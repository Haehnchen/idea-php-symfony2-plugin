package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;

/**
 * Remove TODO: Moved to core
 */
@Deprecated
public class YamlKeyFinder {

    private PsiElement startRoot;

    public YamlKeyFinder(PsiElement startRoot) {
        this.startRoot = startRoot;
    }

    public YAMLKeyValue find(String keyName) {
        return find(this.startRoot, keyName);
    }

    public static YAMLKeyValue find(PsiElement psiElement, String keyName) {

        YAMLKeyValue currentYAMLKeyValues[] = PsiTreeUtil.getChildrenOfType(psiElement, YAMLKeyValue.class);

        if(currentYAMLKeyValues == null) {
            return null;
        }

        for(YAMLKeyValue currentYAMLKeyValue: currentYAMLKeyValues ) {

            // we found your key
            // also online line is supported: test.boo.bar
            if(keyName.equals(currentYAMLKeyValue.getKeyText())) {
                return currentYAMLKeyValue;
            }

            // call me again on key-value child
            if(keyName.startsWith(currentYAMLKeyValue.getKeyText() + ".")) {
                return findKey(currentYAMLKeyValue, keyName);
            }

        }

        return null;
    }


    public static YAMLKeyValue findKey(YAMLKeyValue currentYAMLKeyValue, String keyName) {

        // not a key-value with more child
        YAMLCompoundValue yamlCompoundValues[] = PsiTreeUtil.getChildrenOfType(currentYAMLKeyValue, YAMLCompoundValue.class);
        if(yamlCompoundValues == null) {
            return null;
        }

        // work
        for(YAMLCompoundValue yamlCompoundValue : yamlCompoundValues) {
            PsiElement foundPsiElement = find(yamlCompoundValue, keyName.substring(keyName.indexOf(".") + 1));
            if(foundPsiElement != null) {
                return (YAMLKeyValue) foundPsiElement;
            }
        }

        return null;

    }

    @Nullable
    public static YAMLKeyValue findKeyValueElement(PsiElement psiElementRoot, String keyName) {
        return YamlKeyFinder.find(psiElementRoot, keyName);
    }

    @Nullable
    public static MatchedKey findLastValueElement(PsiElement yamlKeyValue, String keyName) {
        PsiElement lastMatchedKey = yamlKeyValue;
        ArrayList<String> foundKeyName = new ArrayList<String>();
        int startDepth = 0;

        for(String currentKey : keyName.split("\\.")) {

            YAMLKeyValue foundKey;

            // root file yaml key value are not inside compound value
            if(lastMatchedKey instanceof YAMLKeyValue) {
                foundKey = YamlKeyFinder.findKey((YAMLKeyValue) lastMatchedKey, currentKey);
            } else {
                foundKey = YamlKeyFinder.find(lastMatchedKey, currentKey);
            }

            if(foundKey == null) {
                return new MatchedKey(lastMatchedKey, foundKeyName, keyName, startDepth);
            }

            foundKeyName.add(currentKey);
            lastMatchedKey = foundKey;
            startDepth++;
        }

        return new MatchedKey(lastMatchedKey, foundKeyName, keyName, startDepth);
    }

    public static class MatchedKey {

        protected PsiElement yamlKeyValue;
        protected ArrayList<String> foundKeyName;
        protected String keyName;
        protected int startDepth;

        public MatchedKey(PsiElement yamlKeyValue, ArrayList<String> foundKeyName, String keyName, int startDepth) {
            this.yamlKeyValue = yamlKeyValue;
            this.foundKeyName = foundKeyName;
            this.keyName = keyName;
            this.startDepth = startDepth;
        }

        public PsiElement getYamlKeyValue() {
            return yamlKeyValue;
        }

        public String getFoundKeyName() {
            return StringUtils.join(foundKeyName, ".");
        }

        public String[] getMissingKeys() {
            String missing = this.keyName.substring(this.getFoundKeyName().length());

            if(missing.startsWith(".")) {
                missing = missing.substring(1);
            }

            return missing.split("\\.");
        }

        public String getMissingKeysString() {
            String missing = this.keyName.substring(this.getFoundKeyName().length());

            if(!missing.startsWith(".")) {
                return missing;
            }

            return missing.substring(1);
        }

        public int getStartDepth() {
            return startDepth;
        }

    }

}