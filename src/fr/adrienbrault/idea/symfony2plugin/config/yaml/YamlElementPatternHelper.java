package fr.adrienbrault.idea.symfony2plugin.config.yaml;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ProcessingContext;
import fr.adrienbrault.idea.symfony2plugin.util.VfsExUtil;
import fr.adrienbrault.idea.symfony2plugin.util.psi.ParentPathPatternCondition;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLElementTypes;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.*;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlElementPatternHelper {

    /**
     * "@foo", '@foo', @foo
     */
    private static final ElementTypePatternCondition SCALAR_ELEMENT_TYPES = new ElementTypePatternCondition(
        YAMLTokenTypes.TEXT, YAMLTokenTypes.SCALAR_STRING, YAMLTokenTypes.SCALAR_DSTRING
    );

    /**
     * services: ~
     */
    private static PatternCondition<YAMLKeyValue> YAML_KEY_SERVICES = new YAMLKeyValuePatternCondition("services");

    /**
     * config.yml, config_dev.yml,
     * security.yml, security_dev.yml
     *
     * /../config/packages/doctrine.yml
     * /../config/packages/test/doctrine.yml
     */
    private static final PatternCondition<PsiFile> CONFIG_YAML_PATTERN = new PatternCondition<PsiFile>("Yaml Configuration") {
        @Override
        public boolean accepts(@NotNull PsiFile psiFile, ProcessingContext processingContext) {
            if (psiFile.getFileType() != YAMLFileType.YML) {
                return false;
            }

            if (psiFile.getName().matches("(security|config).*\\.(yml|yaml)")) {
                return true;
            }

            VirtualFile virtualFile = psiFile.getVirtualFile();
            if(virtualFile == null) {
                return false;
            }

            String relativePath = VfsExUtil.getRelativeProjectPath(psiFile.getProject(), virtualFile);
            return relativePath != null && relativePath.contains("config/packages/");
        }
    };

    /**
     * auto complete on
     *
     * keyName: refer|
     *
     * @param keyName
     */
    public static ElementPattern<PsiElement> getOrmSingleLineScalarKey(String... keyName) {
        return getKeyPattern(keyName).inFile(getOrmFilePattern()).withLanguage(YAMLLanguage.INSTANCE);
    }

    public static ElementPattern<PsiElement> getSingleLineScalarKey(String... keyName) {
        // key: | and key: "quote" is valid here
        // getKeyPattern
        return PlatformPatterns.or(
                PlatformPatterns
                        .psiElement(YAMLTokenTypes.TEXT)
                        .withParent(PlatformPatterns.psiElement(YAMLScalar.class)
                                .withParent(PlatformPatterns
                                        .psiElement(YAMLKeyValue.class)
                                        .withName(
                                                PlatformPatterns.string().oneOf(keyName)
                                        )
                                ))
                        .withLanguage(YAMLLanguage.INSTANCE)
            ,
            PlatformPatterns
                .psiElement(YAMLTokenTypes.SCALAR_DSTRING)
                    .withParent(PlatformPatterns.psiElement(YAMLScalar.class)
                            .withParent(PlatformPatterns
                                    .psiElement(YAMLKeyValue.class)
                                    .withName(
                                            PlatformPatterns.string().oneOf(keyName)
                                    )
                            ))
                .withLanguage(YAMLLanguage.INSTANCE),
            PlatformPatterns
                .psiElement(YAMLTokenTypes.SCALAR_STRING)
                    .withParent(PlatformPatterns.psiElement(YAMLScalar.class)
                            .withParent(PlatformPatterns
                                    .psiElement(YAMLKeyValue.class)
                                    .withName(
                                            PlatformPatterns.string().oneOf(keyName)
                                    )
                            ))
                .withLanguage(YAMLLanguage.INSTANCE)
        );
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
                        .psiElement(YAMLCompoundValue.class)
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
                .psiElement(YAMLPlainTextImpl.class)
                .withParent(PlatformPatterns
                    .psiElement(YAMLCompoundValue.class)
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
            getKeyPattern(keyName)
                .inFile(getOrmFilePattern())
                .withLanguage(YAMLLanguage.INSTANCE)
        );
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
    public static ElementPattern<PsiElement> getParentKeyName(String keyName) {
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
                        .psiElement(YAMLCompoundValue.class)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLKeyValue.class)
                            .withName(
                                PlatformPatterns.string().equalTo(keyName)
                            )
                        )
                    )
                )
                .withLanguage(YAMLLanguage.INSTANCE),

            // match
            //
            // keyName:
            //   xxx: xxx
            //   refer|
            PlatformPatterns
                .psiElement(YAMLPlainTextImpl.class)
                .withParent(PlatformPatterns
                    .psiElement(YAMLCompoundValue.class)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withName(
                            PlatformPatterns.string().equalTo(keyName)
                        )
                    )
                )
                .withLanguage(YAMLLanguage.INSTANCE),

            // match
            //
            // keyName:
            //   refer|
            //   xxx: xxx
            getKeyPattern(keyName)
                .withLanguage(YAMLLanguage.INSTANCE)
        );
    }

    /**
     * services:
     *   My<caret>Class: ~
     */
    public static ElementPattern<PsiElement> getServicesKeyPattern() {
        return PlatformPatterns
            .psiElement(YAMLTokenTypes.SCALAR_KEY).withParent(
                PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                    PlatformPatterns.psiElement(YAMLMapping.class).withParent(
                        PlatformPatterns.psiElement(YAMLKeyValue.class).with(YAML_KEY_SERVICES)
                    )
                )
            );
    }

    /**
     * Proxy for getWithFirstRootKey to filter with file name condition
     */
    public static ElementPattern<? extends PsiElement> getOrmRoot() {
        return PlatformPatterns.and(PlatformPatterns.psiElement().with(new PatternCondition<PsiElement>("Doctrine file") {
            @Override
            public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
                return getOrmFilePattern().accepts(psiElement.getContainingFile());
            }
        }), getWithFirstRootKey());
    }

    public static ElementPattern<PsiElement> getWithFirstRootKey() {
        return PlatformPatterns.or(
            // foo:
            //   <caret>
            PlatformPatterns
                .psiElement().with(new ParentPathPatternCondition(
                    YAMLScalar.class, YAMLMapping.class,
                    YAMLKeyValue.class, YAMLMapping.class,
                    YAMLDocument.class
                ))
                .withLanguage(YAMLLanguage.INSTANCE),

            // foo:
            //   <caret> (on incomplete)
            PlatformPatterns
                .psiElement().afterLeaf(
                    PlatformPatterns.psiElement(YAMLTokenTypes.INDENT).with(
                        new ParentPathPatternCondition(YAMLKeyValue.class, YAMLMapping.class, YAMLDocument.class)
                    )
                )
                .withLanguage(YAMLLanguage.INSTANCE),

            // match
            //
            // foo:
            //   <caret>: bar
            //   <caret>:
            //   <caret>a:
            PlatformPatterns
                .psiElement().with(new ParentPathPatternCondition(
                    YAMLScalar.class, YAMLKeyValue.class,
                    YAMLMapping.class, YAMLKeyValue.class,
                    YAMLMapping.class, YAMLDocument.class)
                )
                .withLanguage(YAMLLanguage.INSTANCE),

            // foo:
            //   fo<caret>:
            PlatformPatterns.psiElement().with(new ParentPathPatternCondition(
                YAMLKeyValue.class, YAMLMapping.class,
                YAMLKeyValue.class, YAMLMapping.class,
                YAMLDocument.class)
            )
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
                    .psiElement(YAMLCompoundValue.class)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLCompoundValue.class)
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
                .psiElement(YAMLPlainTextImpl.class)
                .withParent(PlatformPatterns
                    .psiElement(YAMLCompoundValue.class)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLCompoundValue.class)
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
                .psiElement(YAMLPlainTextImpl.class)
                .withParent(PlatformPatterns
                    .psiElement(YAMLKeyValue.class)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLCompoundValue.class)
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
     * services:
     *   foo:
     *     class: '</caret>'
     */
    public static ElementPattern<PsiElement> getThreeLevelKeyPattern(String... tree) {
        return PlatformPatterns.psiElement().withParent(
            PlatformPatterns.psiElement(YAMLScalar.class).withParent(
                PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                    PlatformPatterns.psiElement(YAMLMapping.class).withParent(
                        PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                            PlatformPatterns.psiElement(YAMLMapping.class).withParent(
                                PlatformPatterns.psiElement(YAMLKeyValue.class).withName(PlatformPatterns
                                    .string().oneOfIgnoreCase(tree)
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    /**
     * simplified getFilterOnPrevParent :)
    * 
     * services:
     *   foo.name:
     *     "complete": foo
     */
    public static ElementPattern<PsiElement> getSuperParentArrayKey(String... tree) {
        return PlatformPatterns.or(
            // foo:
            //   <caret> (on incomplete)
            PlatformPatterns.psiElement().afterLeaf(
                PlatformPatterns.psiElement(YAMLTokenTypes.INDENT).withParent(
                    PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                        PlatformPatterns.psiElement(YAMLMapping.class).withParent(
                            PlatformPatterns.psiElement(YAMLKeyValue.class).withName(PlatformPatterns
                                .string().oneOfIgnoreCase(tree)
                            )
                        )
                    )
                )
            ),

            /**
             * services:
             *   foo:
             *     cla<caret>:
             */
            PlatformPatterns.psiElement().withParent(
                PlatformPatterns.psiElement(YAMLScalar.class).withParent(
                    PlatformPatterns.psiElement(YAMLMapping.class).withParent(
                        PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                            PlatformPatterns.psiElement(YAMLMapping.class).withParent(
                                PlatformPatterns.psiElement(YAMLKeyValue.class).withName(PlatformPatterns
                                    .string().oneOfIgnoreCase(tree)
                                )
                            )
                        )
                    )
                )
            ),

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
                        .psiElement(YAMLMapping.class)
                        .withParent(PlatformPatterns
                            .psiElement(YAMLKeyValue.class)
                            .withParent(PlatformPatterns
                                .psiElement(YAMLMapping.class)
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
                .withLanguage(YAMLLanguage.INSTANCE)
        );
    }

    /**
     * find common services: @foo, "@foo", '@foo'
     */
    public static ElementPattern<PsiElement> getServiceDefinition() {
        return PlatformPatterns
            .psiElement().with(SCALAR_ELEMENT_TYPES)
            .withParent(
                PlatformPatterns.psiElement(YAMLScalar.class).with(new YAMLScalarValueStartsWithPatternCondition("@"))
            )
            .withLanguage(YAMLLanguage.INSTANCE);
    }

    /**
     * find common service parameter: %foo%, %foo%, %foo%
     */
    public static ElementPattern<PsiElement> getServiceParameterDefinition() {
        return PlatformPatterns
            .psiElement().with(SCALAR_ELEMENT_TYPES)
            .withParent(
                PlatformPatterns.psiElement(YAMLScalar.class).with(new YAMLScalarValueStartsWithPatternCondition("%"))
            )
            .withLanguage(YAMLLanguage.INSTANCE);
    }

    private static ElementPattern<? extends PsiFile> getOrmFilePattern() {
        return PlatformPatterns.psiFile().withName(PlatformPatterns.string().andOr(
            PlatformPatterns.string().endsWith("orm.yml"),
            PlatformPatterns.string().endsWith("couchdb.yml"),
            PlatformPatterns.string().endsWith("odm.yml"),
            PlatformPatterns.string().endsWith("mongodb.yml"),
            PlatformPatterns.string().endsWith("document.yml"),

            PlatformPatterns.string().endsWith("orm.yaml"),
            PlatformPatterns.string().endsWith("couchdb.yaml"),
            PlatformPatterns.string().endsWith("odm.yaml"),
            PlatformPatterns.string().endsWith("mongodb.yaml"),
            PlatformPatterns.string().endsWith("document.yaml")
        ));
    }

    /**
     * foo: <caret>
     * foo: a<caret>
     */
    private static PsiElementPattern.Capture<PsiElement> getKeyPattern(String... keyName) {
        return PlatformPatterns
            .psiElement()
            .withParent(PlatformPatterns.psiElement(YAMLScalar.class).withParent(PlatformPatterns
                .psiElement(YAMLKeyValue.class)
                .withName(
                    PlatformPatterns.string().oneOfIgnoreCase(keyName)
                )
            ));
    }

    /**
     * Possible config key completion
     * In document root or key value context
     */
    public static ElementPattern<PsiElement> getConfigKeyPattern() {
        return PlatformPatterns.psiElement().withParent(PlatformPatterns.or(
            PlatformPatterns.psiElement(YAMLDocument.class),
            PlatformPatterns.psiElement(YAMLScalar.class),
            PlatformPatterns.psiElement(YAMLKeyValue.class)
        )).inFile(
            // not should fire this in all yaml files
            getConfigFileNamePattern()
        );
    }

    /**
     * config.yml, config_dev.yml,
     * security.yml, security_dev.yml
     *
     * /../config/packages/doctrine.yml
     * /../config/packages/test/doctrine.yml
     */
    @NotNull
    public static PsiFilePattern.Capture<PsiFile> getConfigFileNamePattern() {
        return PlatformPatterns.psiFile().with(CONFIG_YAML_PATTERN);
    }

    /**
     * Get service before comma
     *
     * ["@service', createNewsletterManager|]
     * [@service, createNewsletterManager|]
     * ['@service', createNewsletterManager|]
     */
    public static ElementPattern<? extends PsiElement> getAfterCommaPattern() {
        return PlatformPatterns.psiElement().withParent(
            PlatformPatterns.psiElement(YAMLScalar.class).withParent(
                PlatformPatterns.psiElement(YAMLSequenceItem.class).afterLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement().withText(",")
                )
            )
        );

    }

    /**
     * Get service before comma
     *
     * ["@service', createNewsletterManager|]
     * [@service, createNewsletterManager|]
     * ['@service', createNewsletterManager|]
     */
    public static ElementPattern<? extends PsiElement> getPreviousCommaSibling() {

        return PlatformPatterns.or(
            PlatformPatterns
                .psiElement(YAMLScalar.class)
                .beforeLeafSkipping(
                    PlatformPatterns.psiElement(PsiWhiteSpace.class),
                    PlatformPatterns.psiElement().withText(",")
                )
        );

    }

    /**
     * parameters:
     *    foo.example.class: |
     *
     */
    public static PsiElementPattern.Capture<? extends PsiElement> getParameterClassPattern() {
        return PlatformPatterns
            .psiElement()
            .withParent(PlatformPatterns
                .psiElement(YAMLScalar.class)
                    .withParent(PlatformPatterns
                        .psiElement(YAMLKeyValue.class)
                        .withName(
                            PlatformPatterns.string().endsWith(".class")
                        )
                        .withParent(PlatformPatterns
                            .psiElement(YAMLElementTypes.MAPPING)
                            .withParent(PlatformPatterns
                                .psiElement(YAMLKeyValue.class)
                                .withName("parameters")
                            )
                        )
                    )
            );

    }

    public static PsiElementPattern.Capture<PsiElement> getInsideServiceKeyPattern() {
        return getInsideKeyValue("services", "parameters");
    }

    public static PsiElementPattern.Capture<PsiElement> getInsideKeyValue(String... keys) {
        return PlatformPatterns
            .psiElement()
            .inside(
                PlatformPatterns
                    .psiElement(YAMLKeyValue.class)
                    .withName(
                        PlatformPatterns.string().oneOf(keys)
                    )
            );
    }

    /**
     * services:
     *   i<caret>d: []
     */
    public static PsiElementPattern.Capture<PsiElement> getServiceIdKeyPattern() {
        return PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY)
             .withParent(PlatformPatterns.psiElement(YAMLKeyValue.class)
                 .withParent(PlatformPatterns.psiElement(YAMLCompoundValue.class)
                     .withParent(
                          PlatformPatterns.psiElement(YAMLKeyValue.class)
                              .withName(PlatformPatterns.string().oneOfIgnoreCase("services"))
                     )
                 )
             );
    }

    /**
     * services:
     *   i<caret>d: []
     */
    public static PsiElementPattern.Capture<YAMLKeyValue> getServiceIdKeyValuePattern() {
        return PlatformPatterns.psiElement(YAMLKeyValue.class)
            .withParent(PlatformPatterns.psiElement(YAMLMapping.class)
                .withParent(PlatformPatterns.psiElement(YAMLKeyValue.class).withName("services"))
            );
    }

    /**
     * PsiFile / Document:
     *   serv<caret>ices: ~
     */
    public static PsiElementPattern.Capture<PsiElement> getRootConfigKeyPattern() {
        return PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_KEY).withParent(
            PlatformPatterns.psiElement(YAMLKeyValue.class).withParent(
                PlatformPatterns.psiElement(YAMLMapping.class).withParent(
                    PlatformPatterns.psiElement(YAMLDocument.class)
                )
            )
        ).inFile(getConfigFileNamePattern());
    }

    /**
     * tags: [ foobar ]
     */
    public static PsiElementPattern.Capture<PsiElement> getTagsAsSequencePattern() {
        return PlatformPatterns.psiElement().withParent(
            PlatformPatterns.psiElement(YAMLScalar.class).withParent(
                PlatformPatterns.psiElement(YAMLSequenceItem.class).withParent(
                    PlatformPatterns.psiElement(YAMLSequence.class).withParent(
                        PlatformPatterns.psiElement(YAMLKeyValue.class).withName("tags")
                    )
                )
            )
        );
    }

    /**
     * Match elements types
     */
    private static class ElementTypePatternCondition extends PatternCondition<PsiElement> {
        private final IElementType[] elementType;

        ElementTypePatternCondition(IElementType... elementType) {
            super("IElementType matcher");
            this.elementType = elementType;
        }

        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
            return ArrayUtils.contains(elementType, psiElement.getNode().getElementType());
        }
    }

    private static class YAMLScalarValueStartsWithPatternCondition extends PatternCondition<YAMLScalar> {
            @NotNull
        private final String value;

        YAMLScalarValueStartsWithPatternCondition(@NotNull String value) {
            super("YAMLScalar startsWith");
            this.value = value;
        }

        @Override
        public boolean accepts(@NotNull YAMLScalar yamlScalar, ProcessingContext processingContext) {
            return StringUtils.startsWith(yamlScalar.getTextValue(), value);
        }
    }

    private static class YAMLKeyValuePatternCondition extends PatternCondition<YAMLKeyValue> {
        @NotNull
        private final String keyText;

        YAMLKeyValuePatternCondition(@NotNull String keyText) {
            super("yaml " + keyText +" key");
            this.keyText = keyText;
        }

        @Override
        public boolean accepts(@NotNull YAMLKeyValue yamlKeyValue, ProcessingContext processingContext) {
            return this.keyText.equals(yamlKeyValue.getKeyText());
        }
    }
}
