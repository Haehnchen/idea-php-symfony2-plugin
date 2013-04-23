package fr.adrienbrault.idea.symfony2plugin.config.yaml;


import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLKeyValue;

public class YamlElementPatternHelper {

    public static ElementPattern<PsiElement> getOrmSingleLineScalarKey(String keyName) {
        return PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withParent(
                PlatformPatterns.psiElement(YAMLKeyValue.class).withName(
                        PlatformPatterns.string().equalTo(keyName)
                )
        ).inFile(PlatformPatterns.psiFile().withName(PlatformPatterns.string().contains("orm.yml"))).withLanguage(YAMLLanguage.INSTANCE);
    }

    /**
     * provides auto complete on
     *
     * keyName:
     *   refer|: Value
     *
     * @param keyName key name
     * @return
     */
    public static ElementPattern<PsiElement> getOrmParentKeyLookup(String keyName) {
        return PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY).withParent(
                PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                        PlatformPatterns.psiElement(YAMLElementTypes.COMPOUND_VALUE).withParent(
                                PlatformPatterns.psiElement(YAMLKeyValue.class).withName(
                                        PlatformPatterns.string().equalTo(keyName)
                        )
                )
        )).inFile(PlatformPatterns.psiFile().withName(PlatformPatterns.string().contains("orm.yml"))).withLanguage(YAMLLanguage.INSTANCE);
    }

    /**
     * provides auto complete on
     *
     * keyName:
     *   refer|
     *
     * @param keyName key name
     * @return
     */
    public static ElementPattern<PsiElement> getOrmParentEmptyLookup(String keyName) {
        return PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withParent(
                PlatformPatterns.psiElement(YAMLElementTypes.COMPOUND_VALUE).withParent(
                        PlatformPatterns.psiElement(YAMLKeyValue.class).withName(
                                PlatformPatterns.string().equalTo(keyName)
                        )
                )
        ).inFile(PlatformPatterns.psiFile().withName(PlatformPatterns.string().contains("orm.yml"))).withLanguage(YAMLLanguage.INSTANCE);
    }

    /**
     * provides auto complete on
     *
     * keyName:
     *   refer|
     *   refer|: Value
     *
     * @param keyName key name
     * @return
     */
    public static ElementPattern<PsiElement> getOrmParentLookup(String keyName) {
        return PlatformPatterns.or(getOrmParentKeyLookup(keyName), getOrmParentEmptyLookup(keyName));
    }

    /**
     * find common services
     *
     * @return
     */
    public static ElementPattern<PsiElement> getServiceDefinition () {
        return PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withText(StandardPatterns.string().startsWith("@")).withLanguage(YAMLLanguage.INSTANCE);
    }

}
