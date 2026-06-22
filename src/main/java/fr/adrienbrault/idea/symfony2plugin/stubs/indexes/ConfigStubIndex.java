package fr.adrienbrault.idea.symfony2plugin.stubs.indexes;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.jetbrains.php.codeInsight.PhpScopeHolder;
import com.jetbrains.php.codeInsight.controlFlow.PhpControlFlowUtil;
import com.jetbrains.php.codeInsight.controlFlow.PhpInstructionProcessor;
import com.jetbrains.php.codeInsight.controlFlow.instructions.PhpReturnInstruction;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.config.php.PhpConfigUtil;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.ConfigIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.ConfigIndexExternalizer;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLFileType;
import org.jetbrains.yaml.psi.*;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class ConfigStubIndex extends FileBasedIndexExtension<String, ConfigIndex> {
    public static final ID<String, ConfigIndex> KEY = ID.create("fr.adrienbrault.idea.symfony2plugin.config_stub_index");
    private final KeyDescriptor<String> myKeyDescriptor = new EnumeratorStringDescriptor();
    private static final int MAX_FILE_BYTE_SIZE = 2097152;
    private static final ConfigIndexExternalizer EXTERNALIZER = ConfigIndexExternalizer.INSTANCE;
    private static final String TWIG_COMPONENT = "twig_component";
    private static final String TWIG_COMPONENT_DEFAULTS = "twig_component_defaults";
    private static final String ANONYMOUS_TEMPLATE_DIRECTORY = "anonymous_template_directory";

    @NotNull
    @Override
    public ID<String, ConfigIndex> getName() {
        return KEY;
    }

    @Override
    // Index-safe only: no PhpIndex/type resolution here.
    public @NotNull DataIndexer<String, ConfigIndex, FileContent> getIndexer() {
        return inputData -> {
            if (!Symfony2ProjectComponent.isEnabledForIndex(inputData.getProject())) {
                return Collections.emptyMap();
            }

            if (inputData.getFile().getFileType() == PhpFileType.INSTANCE) {
                if (inputData.getFile().getLength() > MAX_FILE_BYTE_SIZE) {
                    return Collections.emptyMap();
                }

                CharSequence content = inputData.getContentAsText();
                if (!StringUtil.contains(content, TWIG_COMPONENT)) {
                    return Collections.emptyMap();
                }

                PsiFile psiFile = inputData.getPsiFile();
                if (!(psiFile instanceof PhpFile phpFile) ||
                    !StubIndexValidationUtil.isValidForIndex(inputData, psiFile, MAX_FILE_BYTE_SIZE, true, null)
                ) {
                    return Collections.emptyMap();
                }

                return mapPhpConfig(phpFile);
            }

            PsiFile psiFile = inputData.getPsiFile();
            if (psiFile instanceof YAMLFile yamlFile &&
                StubIndexValidationUtil.isValidForIndex(inputData, psiFile, MAX_FILE_BYTE_SIZE, true, null)
            ) {
                return mapYamlConfig(yamlFile);
            }

            return Collections.emptyMap();
        };
    }

    @NotNull
    private static Map<String, ConfigIndex> mapYamlConfig(@NotNull YAMLFile yamlFile) {
        LinkedHashMap<String, LinkedHashMap<String, String>> configs = new LinkedHashMap<>();
        Set<String> anonymousTemplateDirectory = new LinkedHashSet<>();

        for (YAMLKeyValue yamlKeyValue : YamlHelper.getTopLevelKeyValues(yamlFile)) {
            String keyText = yamlKeyValue.getKeyText();
            if (TWIG_COMPONENT.equals(keyText)) {
                visitYamlTwigComponent(yamlKeyValue, configs, anonymousTemplateDirectory);
            }

            if (keyText.startsWith("when@")) {
                YAMLValue value = yamlKeyValue.getValue();
                if (value instanceof YAMLMapping yamlMapping) {
                    for (YAMLKeyValue yamlKeyValue2 : yamlMapping.getKeyValues()) {
                        String keyText2 = yamlKeyValue2.getKeyText();
                        if (TWIG_COMPONENT.equals(keyText2)) {
                            visitYamlTwigComponent(yamlKeyValue2, configs, anonymousTemplateDirectory);
                        }
                    }
                }
            }
        }

        return buildConfigMap(configs, anonymousTemplateDirectory);
    }

    @NotNull
    private static Map<String, ConfigIndex> mapPhpConfig(@NotNull PhpFile phpFile) {
        LinkedHashMap<String, LinkedHashMap<String, String>> configs = new LinkedHashMap<>();
        Set<String> anonymousTemplateDirectory = new LinkedHashSet<>();

        visitPhpConfigFile(phpFile, configs, anonymousTemplateDirectory);

        return buildConfigMap(configs, anonymousTemplateDirectory);
    }

    @NotNull
    private static Map<String, ConfigIndex> buildConfigMap(
        @NotNull LinkedHashMap<String, LinkedHashMap<String, String>> configs,
        @NotNull Set<String> anonymousTemplateDirectory
    ) {
        Map<String, ConfigIndex> map = new HashMap<>();

        if (!configs.isEmpty()) {
            map.put(TWIG_COMPONENT_DEFAULTS, new ConfigIndex(TWIG_COMPONENT_DEFAULTS, configs, Collections.emptySet()));
        }

        if (!anonymousTemplateDirectory.isEmpty()) {
            map.put(ANONYMOUS_TEMPLATE_DIRECTORY, new ConfigIndex(ANONYMOUS_TEMPLATE_DIRECTORY, new LinkedHashMap<>(), anonymousTemplateDirectory));
        }

        return map;
    }

    private static void visitYamlTwigComponent(
        @NotNull YAMLKeyValue yamlKeyValue,
        @NotNull LinkedHashMap<String, LinkedHashMap<String, String>> configs,
        @NotNull Set<String> anonymousTemplateDirectory
    ) {
        YAMLValue value = yamlKeyValue.getValue();
        if (value instanceof YAMLMapping yamlMapping) {
            YAMLKeyValue defaults = YamlHelper.getYamlKeyValue(yamlMapping, "defaults");
            if (defaults != null) {
                YAMLValue value1 = defaults.getValue();
                if (value1 instanceof YAMLMapping yamlMapping1) {
                    for (YAMLKeyValue keyValue : yamlMapping1.getKeyValues()) {
                        String keyText1 = keyValue.getKeyText();

                        YAMLValue value2 = keyValue.getValue();
                        if (value2 instanceof YAMLQuotedText || value2 instanceof YAMLPlainTextImpl) {
                            String s = PsiElementUtils.trimQuote(value2.getText());
                            if (!StringUtils.isBlank(s)) {
                                LinkedHashMap<String, String> items = new LinkedHashMap<>();
                                items.put("template_directory", s);
                                configs.put(keyText1, items);
                            }
                        } else if (value2 instanceof YAMLMapping yamlMapping2) {
                            LinkedHashMap<String, String> items = new LinkedHashMap<>();

                            String templateDirectory = YamlHelper.getYamlKeyValueAsString(yamlMapping2, "template_directory");
                            if (templateDirectory == null) {
                                templateDirectory = "components";
                            }

                            items.put("template_directory", templateDirectory);

                            String namePrefix = YamlHelper.getYamlKeyValueAsString(yamlMapping2, "name_prefix");
                            if (namePrefix != null) {
                                items.put("name_prefix", namePrefix);
                            }

                            configs.put(keyText1, items);
                        }
                    }
                }
            }

            String templateDirectory = YamlHelper.getYamlKeyValueAsString(yamlMapping, ANONYMOUS_TEMPLATE_DIRECTORY);
            if (templateDirectory != null) {
                anonymousTemplateDirectory.add(templateDirectory);
            }
        }
    }

    private static void visitPhpConfigFile(
        @NotNull PhpFile phpFile,
        @NotNull LinkedHashMap<String, LinkedHashMap<String, String>> configs,
        @NotNull Set<String> anonymousTemplateDirectory
    ) {
        visitTopLevelReturnArguments(phpFile, argument -> {
            // Returned root forms: ['twig_component' => [...]] and App::config(['twig_component' => [...]]).
            ArrayCreationExpression array = getRootConfigArray(argument);
            if (array != null) {
                visitPhpConfigArray(array, configs, anonymousTemplateDirectory);
            }

            // Returned closure form: second argument of $container->extension('twig_component', [...]).
            Function function = findReturnedClosure(argument);
            if (function != null) {
                visitPhpExtensionCalls(function, configs, anonymousTemplateDirectory);
            }
        });

        visitTopLevelConfigFactoryCalls(phpFile, array -> visitPhpConfigArray(array, configs, anonymousTemplateDirectory));
    }

    private static void visitPhpConfigArray(
        @NotNull ArrayCreationExpression configArray,
        @NotNull LinkedHashMap<String, LinkedHashMap<String, String>> configs,
        @NotNull Set<String> anonymousTemplateDirectory
    ) {
        // Config entries: ['twig_component' => [...]] and ['when@test' => ['twig_component' => [...]]].
        for (ArrayHashElement hashElement : configArray.getHashElements()) {
            String key = getPhpArrayKey(hashElement);
            if (TWIG_COMPONENT.equals(key)) {
                PsiElement value = hashElement.getValue();
                if (value instanceof ArrayCreationExpression array) {
                    visitPhpTwigComponentArray(array, configs, anonymousTemplateDirectory);
                }
            } else if (key != null && key.startsWith("when@")) {
                PsiElement value = hashElement.getValue();
                if (value instanceof ArrayCreationExpression array) {
                    visitPhpConfigArray(array, configs, anonymousTemplateDirectory);
                }
            }
        }
    }

    @Nullable
    private static Function findReturnedClosure(@NotNull PsiElement element) {
        Function function = unwrapPhpClosure(element);
        if (function != null) {
            return function;
        }

        return element instanceof PhpPsiElement phpPsiElement ? unwrapPhpClosure(phpPsiElement.getFirstPsiChild()) : null;
    }

    /**
     * Returned closure configs:
     * {@code return static function (ContainerConfigurator $container): void { ... };}
     * The PHP PSI can expose this as the closure or as a PHP expression wrapper
     * whose first PSI child is the closure.
     */
    @Nullable
    private static Function unwrapPhpClosure(@Nullable PsiElement element) {
        if (element instanceof Function function && function.isClosure()) {
            return function;
        }

        return null;
    }

    private static void visitPhpExtensionCalls(
        @NotNull Function function,
        @NotNull LinkedHashMap<String, LinkedHashMap<String, String>> configs,
        @NotNull Set<String> anonymousTemplateDirectory
    ) {
        Set<String> parameterNames = new HashSet<>();
        for (Parameter parameter : function.getParameters()) {
            if (StringUtils.isNotBlank(parameter.getName())) {
                parameterNames.add(parameter.getName());
            }
        }

        if (parameterNames.isEmpty()) {
            return;
        }

        for (MethodReference methodReference : PhpElementsUtil.collectMethodReferencesInsideControlFlow(function, "extension")) {
            if (!isMethodCallOnParameter(methodReference, parameterNames)) {
                continue;
            }

            PsiElement[] parameters = methodReference.getParameters();
            if (parameters.length < 2 || !(parameters[1] instanceof ArrayCreationExpression array)) {
                continue;
            }

            String extensionName = getPhpStringLiteralContents(parameters[0]);
            if (TWIG_COMPONENT.equals(extensionName)) {
                visitPhpTwigComponentArray(array, configs, anonymousTemplateDirectory);
            }
        }
    }

    @Nullable
    private static ArrayCreationExpression getRootConfigArray(@NotNull PsiElement argument) {
        if (argument instanceof ArrayCreationExpression array && PhpConfigUtil.isAcceptedConfigRootArray(array)) {
            return array;
        }

        if (argument instanceof MethodReference methodReference) {
            return getConfigFactoryArray(methodReference);
        }

        return null;
    }

    private static void visitTopLevelConfigFactoryCalls(
        @NotNull PhpFile phpFile,
        @NotNull Consumer<ArrayCreationExpression> consumer
    ) {
        visitTopLevelPhpScopes(phpFile, scope -> {
            for (MethodReference methodReference : PhpElementsUtil.collectMethodReferencesInsideControlFlow(scope, "config")) {
                if (methodReference.getParent() instanceof PhpReturn) {
                    continue;
                }

                ArrayCreationExpression array = getConfigFactoryArray(methodReference);
                if (array != null) {
                    consumer.accept(array);
                }
            }
        });
    }

    @Nullable
    private static ArrayCreationExpression getConfigFactoryArray(@NotNull MethodReference methodReference) {
        PsiElement[] parameters = methodReference.getParameters();
        if (parameters.length == 0 || !(parameters[0] instanceof ArrayCreationExpression array)) {
            return null;
        }

        return PhpConfigUtil.isAcceptedConfigRootArray(array) ? array : null;
    }

    private static boolean isMethodCallOnParameter(@NotNull MethodReference methodReference, @NotNull Set<String> parameterNames) {
        PhpExpression classReference = methodReference.getClassReference();
        return classReference instanceof Variable variable && parameterNames.contains(variable.getName());
    }

    private static void visitTopLevelReturnArguments(@NotNull PhpFile phpFile, @NotNull Consumer<PsiElement> consumer) {
        visitTopLevelPhpScopes(phpFile, scope -> PhpControlFlowUtil.processFlow(scope.getControlFlow(), new PhpInstructionProcessor() {
            @Override
            public boolean processReturnInstruction(PhpReturnInstruction instruction) {
                PsiElement argument = instruction.getArgument();
                if (argument != null) {
                    consumer.accept(argument);
                }

                return super.processReturnInstruction(instruction);
            }
        }));
    }

    private static void visitTopLevelPhpScopes(@NotNull PhpFile phpFile, @NotNull Consumer<PhpScopeHolder> consumer) {
        consumer.accept(phpFile);

        for (PhpNamedElement value : phpFile.getTopLevelDefs().values()) {
            if (value instanceof PhpNamespace phpNamespace) {
                consumer.accept(phpNamespace);
            }
        }
    }

    private static void visitPhpTwigComponentArray(
        @NotNull ArrayCreationExpression twigComponentArray,
        @NotNull LinkedHashMap<String, LinkedHashMap<String, String>> configs,
        @NotNull Set<String> anonymousTemplateDirectory
    ) {
        // twig_component array keys: 'defaults' and 'anonymous_template_directory'.
        PsiElement defaults = getPhpArrayValue(twigComponentArray, "defaults");
        if (defaults instanceof ArrayCreationExpression defaultsArray) {
            visitPhpTwigComponentDefaults(defaultsArray, configs);
        }

        String templateDirectory = getPhpStringLiteralContents(getPhpArrayValue(twigComponentArray, ANONYMOUS_TEMPLATE_DIRECTORY));
        if (StringUtils.isNotBlank(templateDirectory)) {
            anonymousTemplateDirectory.add(templateDirectory);
        }
    }

    private static void visitPhpTwigComponentDefaults(
        @NotNull ArrayCreationExpression defaultsArray,
        @NotNull LinkedHashMap<String, LinkedHashMap<String, String>> configs
    ) {
        // Defaults array entries: namespace => directory or namespace => ['template_directory' => directory, 'name_prefix' => prefix].
        for (ArrayHashElement hashElement : defaultsArray.getHashElements()) {
            String namespace = getPhpArrayKey(hashElement);
            if (StringUtils.isBlank(namespace)) {
                continue;
            }

            PsiElement value = hashElement.getValue();
            String shortTemplateDirectory = getPhpStringLiteralContents(value);
            if (StringUtils.isNotBlank(shortTemplateDirectory)) {
                LinkedHashMap<String, String> items = new LinkedHashMap<>();
                items.put("template_directory", shortTemplateDirectory);
                configs.put(namespace, items);
            } else if (value instanceof ArrayCreationExpression array) {
                LinkedHashMap<String, String> items = new LinkedHashMap<>();

                String templateDirectory = getPhpStringLiteralContents(getPhpArrayValue(array, "template_directory"));
                if (templateDirectory == null) {
                    templateDirectory = "components";
                }

                items.put("template_directory", templateDirectory);

                String namePrefix = getPhpStringLiteralContents(getPhpArrayValue(array, "name_prefix"));
                if (namePrefix != null) {
                    items.put("name_prefix", namePrefix);
                }

                configs.put(namespace, items);
            }
        }
    }

    @Nullable
    private static PsiElement getPhpArrayValue(@NotNull ArrayCreationExpression array, @NotNull String key) {
        for (ArrayHashElement hashElement : array.getHashElements()) {
            if (key.equals(getPhpArrayKey(hashElement))) {
                return hashElement.getValue();
            }
        }

        return null;
    }

    @Nullable
    private static String getPhpArrayKey(@NotNull ArrayHashElement hashElement) {
        return getPhpStringLiteralContents(hashElement.getKey());
    }

    @Nullable
    private static String getPhpStringLiteralContents(@Nullable PsiElement element) {
        if (!(element instanceof StringLiteralExpression stringLiteralExpression)) {
            return null;
        }

        return stringLiteralExpression.getContents().replace("\\\\", "\\");
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return this.myKeyDescriptor;
    }

    @Override
    public @NotNull DataExternalizer<ConfigIndex> getValueExternalizer() {
        return EXTERNALIZER;
    }

    @Override
    public int getVersion() {
        return 5;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return virtualFile -> virtualFile.getFileType() == YAMLFileType.YML || virtualFile.getFileType() == PhpFileType.INSTANCE;
    }


    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}
