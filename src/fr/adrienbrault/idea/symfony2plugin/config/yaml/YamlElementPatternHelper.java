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

    /**
     * auto complete on
     *
     * keyName: refer|
     *
     * @param keyName
     */
    public static ElementPattern<PsiElement> getOrmSingleLineScalarKey(String keyName) {
        return PlatformPatterns
            .psiElement(YAMLTokenTypes.TEXT)
            .withParent(PlatformPatterns
                .psiElement(YAMLKeyValue.class)
                .withName(
                    PlatformPatterns.string().equalTo(keyName)
                )
            )
            .inFile(getOrmFilePattern())
            .withLanguage(YAMLLanguage.INSTANCE)
        ;
    }

    /**
     * provides auto complete on
     *
     * keyName:
     *   refer|
     *   refer|: xxx
     *   refer|
     *
     * @param keyName key name
     */
    public static ElementPattern<PsiElement> getOrmParentLookup(String keyName) {
        return PlatformPatterns.or(
            // match
            //
            // keyName:
            //   refer|: xxx
            PlatformPatterns
                .psiElement(YAMLTokenTypes.SCALAR_KEY)
                .withParent(PlatformPatterns
                    .psiElement(YAMLKeyValue.class)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLElementTypes.COMPOUND_VALUE)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLKeyValue.class)
                            .withName(
                                PlatformPatterns.string().equalTo(keyName)
                            )
                        )
                    )
                )
                .inFile(getOrmFilePattern())
                .withLanguage(YAMLLanguage.INSTANCE),

            // match
            //
            // keyName:
            //   xxx: xxx
            //   refer|
            PlatformPatterns
                .psiElement(YAMLTokenTypes.TEXT)
                .withParent(PlatformPatterns
                    .psiElement(YAMLElementTypes.COMPOUND_VALUE)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withName(
                            PlatformPatterns.string().equalTo(keyName)
                        )
                    )
                )
                .inFile(getOrmFilePattern())
                .withLanguage(YAMLLanguage.INSTANCE),

            // match
            //
            // keyName:
            //   refer|
            //   xxx: xxx
            PlatformPatterns
                .psiElement(YAMLTokenTypes.TEXT)
                .withParent(PlatformPatterns
                    .psiElement(YAMLKeyValue.class)
                    .withName(
                        PlatformPatterns.string().equalTo(keyName)
                    )
                )
                .inFile(getOrmFilePattern())
                .withLanguage(YAMLLanguage.INSTANCE)
        );
    }

    /**
     * provides auto complete on
     *
     * xxx:
     *   refer|
     *   refer|: xxx
     *   refer|
     */
    public static ElementPattern<PsiElement> getOrmRoot() {
        return PlatformPatterns.or(

            // match
            //
            // xxx:
            //   refer|: xxx
            PlatformPatterns
                .psiElement(YAMLTokenTypes.SCALAR_KEY)
                .withParent(PlatformPatterns
                    .psiElement(YAMLKeyValue.class)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLElementTypes.COMPOUND_VALUE)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLKeyValue.class)
                            .withParent(PlatformPatterns
                                .psiElement(YAMLDocument.class)
                            )
                        )
                    )
                )
                .inFile(getOrmFilePattern())
                .withLanguage(YAMLLanguage.INSTANCE),

            // match
            //
            // xxx:
            //   xxx: xxx
            //   refer|
            PlatformPatterns
                .psiElement(YAMLTokenTypes.TEXT)
                .withParent(PlatformPatterns
                    .psiElement(YAMLElementTypes.COMPOUND_VALUE)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLDocument.class)
                        )
                    )
                )
                .inFile(getOrmFilePattern())
                .withLanguage(YAMLLanguage.INSTANCE),

            // match
            //
            // xxx:
            //   refer|
            //   xxx: xxx
            PlatformPatterns
                .psiElement(YAMLTokenTypes.TEXT)
                .withParent(PlatformPatterns
                    .psiElement(YAMLKeyValue.class)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLDocument.class)
                    )
                )
                .inFile(getOrmFilePattern())
                .withLanguage(YAMLLanguage.INSTANCE)
        );
    }

    /**
     * provides auto complete on
     *
     * tree:
     *   xxx:
     *     refer|
     *     refer|: xxx
     *     refer|
     */
    public static ElementPattern<PsiElement> getFilterOnPrevParent(String... tree) {
        return PlatformPatterns.or(

            // match
            //
            // tree:
            //   xxx:
            //     refer|: xxx
            PlatformPatterns
            .psiElement(YAMLTokenTypes.SCALAR_KEY)
            .withParent(PlatformPatterns
                .psiElement(YAMLKeyValue.class)
                .withParent(PlatformPatterns
                    .psiElement(YAMLElementTypes.COMPOUND_VALUE)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLElementTypes.COMPOUND_VALUE)
                            .withParent(PlatformPatterns
                                .psiElement(YAMLKeyValue.class)
                                .withName(PlatformPatterns
                                    .string().oneOfIgnoreCase(tree)
                                )
                            )
                        )
                    )
                )
            )
            .inFile(getOrmFilePattern())
            .withLanguage(YAMLLanguage.INSTANCE),

            // match
            //
            // tree:
            //   xxx:
            //     xxx: xxx
            //     refer|
            PlatformPatterns
                .psiElement(YAMLTokenTypes.TEXT)
                .withParent(PlatformPatterns
                    .psiElement(YAMLElementTypes.COMPOUND_VALUE)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLElementTypes.COMPOUND_VALUE)
                            .withParent(PlatformPatterns
                                .psiElement(YAMLKeyValue.class)
                                .withName(PlatformPatterns
                                    .string().oneOfIgnoreCase(tree)
                                )
                            )
                        )
                    )
                )
                .inFile(getOrmFilePattern())
                .withLanguage(YAMLLanguage.INSTANCE),

            // match
            //
            // tree:
            //   xxx:
            //     refer|
            //     xxx: xxx
            PlatformPatterns
                .psiElement(YAMLTokenTypes.TEXT)
                .withParent(PlatformPatterns
                    .psiElement(YAMLKeyValue.class)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLElementTypes.COMPOUND_VALUE)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLKeyValue.class)
                            .withName(PlatformPatterns
                                .string().oneOfIgnoreCase(tree)
                            )
                        )
                    )
                )
                .inFile(getOrmFilePattern())
                .withLanguage(YAMLLanguage.INSTANCE)
        );
    }

    /**
     * find common services
     */
    public static ElementPattern<PsiElement> getServiceDefinition() {
        return PlatformPatterns
            .psiElement(YAMLTokenTypes.TEXT)
            .withText(
                StandardPatterns.string().startsWith("@")
            )
            .withLanguage(YAMLLanguage.INSTANCE)
        ;
    }

    /**
     * find common service parameter
     */
    public static ElementPattern<PsiElement> getServiceParameterDefinition() {
        return PlatformPatterns
            .psiElement(YAMLTokenTypes.TEXT)
            .withText(
                StandardPatterns.string().startsWith("%")
            )
            .withLanguage(YAMLLanguage.INSTANCE)
            ;
    }

    private static ElementPattern<? extends PsiFile> getOrmFilePattern() {
        return PlatformPatterns.psiFile().withName(PlatformPatterns.string().endsWith("orm.yml"));
    }

}
