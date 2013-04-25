package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiFilePattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlElementPatternHelper {

    public static ElementPattern<PsiElement> getOrmSingleLineScalarKey(String keyName) {
        return PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withParent(
                PlatformPatterns.psiElement(YAMLKeyValue.class).withName(
                        PlatformPatterns.string().equalTo(keyName)
                )
        ).inFile(getOrmFilePattern()).withLanguage(YAMLLanguage.INSTANCE);
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
        )).inFile(getOrmFilePattern()).withLanguage(YAMLLanguage.INSTANCE);
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
        ).inFile(getOrmFilePattern()).withLanguage(YAMLLanguage.INSTANCE);
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

    public static ElementPattern<PsiElement> getOrmRoot() {
        return PlatformPatterns.or(

                // match refer|: Value
                PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY).withParent(
                        PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                                PlatformPatterns.psiElement(YAMLElementTypes.COMPOUND_VALUE).withParent(
                                        PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                                                PlatformPatterns.psiElement(YAMLDocument.class)
                                        )
                                )
                        )).inFile(getOrmFilePattern()).withLanguage(YAMLLanguage.INSTANCE),

                // match refer|
                PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withParent(
                        PlatformPatterns.psiElement(YAMLElementTypes.COMPOUND_VALUE).withParent(
                                PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                                        PlatformPatterns.psiElement(YAMLDocument.class)
                                )
                        )
                ).inFile(getOrmFilePattern()).withLanguage(YAMLLanguage.INSTANCE)
        );
    }

    public static ElementPattern<PsiElement> getFilterOnPrevParent(String tree) {
        return PlatformPatterns.or(

                // match refer|: Value
                PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY).withParent(
                        PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                                PlatformPatterns.psiElement(YAMLElementTypes.COMPOUND_VALUE).withParent(
                                        PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                                                PlatformPatterns.psiElement(YAMLElementTypes.COMPOUND_VALUE).withParent(
                                                        PlatformPatterns.psiElement(YAMLKeyValue.class).withName(
                                                                PlatformPatterns.string().oneOfIgnoreCase(tree)
                                                        )
                                                )
                                        )
                                )
                        )).inFile(getOrmFilePattern()).withLanguage(YAMLLanguage.INSTANCE),

                // match refer|
                PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withParent(
                        PlatformPatterns.psiElement(YAMLElementTypes.COMPOUND_VALUE).withParent(
                                PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                                        PlatformPatterns.psiElement(YAMLElementTypes.COMPOUND_VALUE).withParent(
                                                PlatformPatterns.psiElement(YAMLKeyValue.class).withName(
                                                        PlatformPatterns.string().oneOfIgnoreCase(tree)
                                                )
                                        )
                                )
                )).inFile(getOrmFilePattern()).withLanguage(YAMLLanguage.INSTANCE)
        );
    }

    /**
     * find common services
     *
     * @return
     */
    public static ElementPattern<PsiElement> getServiceDefinition () {
        return PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).withText(StandardPatterns.string().startsWith("@")).withLanguage(YAMLLanguage.INSTANCE);
    }

    private static ElementPattern<? extends PsiFile> getOrmFilePattern() {
        return PlatformPatterns.psiFile().withName(PlatformPatterns.string().endsWith("orm.yml"));
    }

}
