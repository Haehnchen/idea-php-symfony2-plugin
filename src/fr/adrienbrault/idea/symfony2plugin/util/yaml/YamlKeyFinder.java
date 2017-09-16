package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * Remove TODO: Moved to core
 */
@Deprecated
public class YamlKeyFinder {
    @Nullable
    public static YAMLKeyValue findKeyValueElement(PsiElement psiElementRoot, String keyName) {
        return YamlKeyFinder.find(psiElementRoot, keyName);
    }

    private static YAMLKeyValue find(PsiElement psiElement, String keyName) {

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

    private static YAMLKeyValue findKey(YAMLKeyValue currentYAMLKeyValue, String keyName) {
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
}