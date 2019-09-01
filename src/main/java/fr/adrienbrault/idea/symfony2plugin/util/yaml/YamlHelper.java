package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ProcessingContext;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.lang.psi.elements.Parameter;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.refactoring.PhpNameUtil;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.ParameterResolverConsumer;
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.tags.yaml.StaticAttributeResolver;
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.ParameterVisitor;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlServiceTag;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlTagVisitor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;
import org.jetbrains.yaml.psi.impl.YAMLHashImpl;
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl;

import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class YamlHelper {

    @Nullable
    static public PsiElement getLocalServiceName(PsiFile psiFile, String findServiceName) {
        return new YamlLocalServiceMap().getLocalServiceName(psiFile, findServiceName);
    }

    static public Map<String, String> getLocalParameterMap(PsiFile psiElement) {
        return new YamlLocalServiceMap().getLocalParameterMap(psiElement);
    }

    static public PsiElement getLocalParameterMap(PsiFile psiFile, String parameterName) {
        return new YamlLocalServiceMap().getLocalParameterName(psiFile, parameterName);
    }

    /**
     * getChildren eg on YamlArray is empty, provide workaround
     */
    static public PsiElement[] getChildrenFix(PsiElement psiElement) {
        List<PsiElement> psiElements = new ArrayList<>();

        PsiElement startElement = psiElement.getFirstChild();
        if(startElement == null) {
            return psiElements.toArray(new PsiElement[psiElements.size()]);
        }

        psiElements.add(startElement);

        for (PsiElement child = psiElement.getFirstChild().getNextSibling(); child != null; child = child.getNextSibling()) {
            psiElements.add(child);
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    /**
     *  FOO:
     *    - foobar
     *
     *  FOO: [foobar]
     */
    @NotNull
    static public Collection<YAMLSequenceItem> getSequenceItems(@NotNull YAMLKeyValue yamlKeyValue) {
        PsiElement yamlSequence = yamlKeyValue.getLastChild();

        if(yamlSequence instanceof YAMLSequence) {
            return ((YAMLSequence) yamlSequence).getItems();
        }

        return Collections.emptyList();
    }

    /**
     * [ROLE_USER, FEATURE_ALPHA, ROLE_ALLOWED_TO_SWITCH]
     */
    @NotNull
    static public Collection<String> getYamlArrayValuesAsString(@NotNull YAMLSequence yamlArray) {
        return new HashSet<>(getYamlArrayValuesAsList(yamlArray));
    }

    /**
     * [ROLE_USER, FEATURE_ALPHA, ROLE_ALLOWED_TO_SWITCH]
     */
    @NotNull
    static public Collection<String> getYamlArrayValuesAsList(@NotNull YAMLSequence yamlArray) {
        Collection<String> keys = new ArrayList<>();

        for (YAMLSequenceItem yamlSequenceItem : yamlArray.getItems()) {
            YAMLValue value = yamlSequenceItem.getValue();
            if(!(value instanceof YAMLScalar)) {
                continue;
            }

            String textValue = ((YAMLScalar) value).getTextValue();
            if(StringUtils.isNotBlank(textValue)) {
                keys.add(textValue);
            }
        }

        return keys;
    }

    @Nullable
    public static YAMLKeyValue getYamlKeyValue(@Nullable PsiElement yamlCompoundValue, String keyName) {
        return getYamlKeyValue(yamlCompoundValue, keyName, false);
    }

    @Nullable
    public static YAMLKeyValue getYamlKeyValue(@Nullable PsiElement yamlCompoundValue, String keyName, boolean ignoreCase) {
        if (!(yamlCompoundValue instanceof YAMLMapping)) {
            return null;
        }

        if (!ignoreCase) {
            return ((YAMLMapping) yamlCompoundValue).getKeyValueByKey(keyName);
        }
        
        YAMLKeyValue classKeyValue;
        classKeyValue = PsiElementUtils.getChildrenOfType(yamlCompoundValue, PlatformPatterns.psiElement(YAMLKeyValue.class).withName(PlatformPatterns.string().oneOfIgnoreCase(keyName)));

        if(classKeyValue == null) {
            return null;
        }

        return classKeyValue;
    }

    private static class YamlLocalServiceMap {

        public Map<String, String> getLocalParameterMap(PsiFile psiFile) {
            Map<String, String> map = new HashMap<>();

            for(YAMLKeyValue yamlParameterArray: getQualifiedKeyValuesInFile((YAMLFile) psiFile, "parameters")) {
                String keyName = yamlParameterArray.getKeyText();
                if(StringUtils.isBlank(keyName)) {
                    continue;
                }

                // extract parameter value
                String textValue = null;
                PsiElement value = yamlParameterArray.getValue();
                if(value instanceof YAMLScalar) {
                    String myTextValue = ((YAMLScalar) value).getTextValue();
                    if(myTextValue.length() > 0 && myTextValue.length() < 150) {
                        textValue = myTextValue;
                    }
                }

                map.put(keyName.toLowerCase(), textValue);
            }

            return map;
        }

        @Nullable
        public PsiElement getLocalServiceName(PsiFile psiFile, String findServiceName) {
            return getYamlKeyPath(psiFile, findServiceName, "services");
        }

        @Nullable
        public PsiElement getLocalParameterName(PsiFile psiFile, String findServiceName) {
            return getYamlKeyPath(psiFile, findServiceName, "parameters");
        }

        @Nullable
        private PsiElement getYamlKeyPath(@NotNull PsiFile psiFile, final @NotNull String findServiceName, @NotNull String rootKey) {

            final Collection<PsiElement> psiElements = new ArrayList<>();

            // @TODO: support case insensitive
            visitQualifiedKeyValuesInFile((YAMLFile) psiFile, rootKey, yamlKeyValue -> {
                if(findServiceName.equalsIgnoreCase(yamlKeyValue.getKeyText())) {
                    psiElements.add(yamlKeyValue);
                }
            });

            if(psiElements.size() == 0) {
                return null;
            }

            // @TODO: provide support for multiple targets
            return psiElements.iterator().next();
        }
    }

    public static boolean isValidParameterName(@NotNull String parameterName) {

        if(parameterName.length() < 3) {
            return false;
        }

        if(!parameterName.startsWith("%") || !parameterName.endsWith("%") || parameterName.toLowerCase().startsWith("%env(")) {
            return false;
        }

        // use regular expr here?
        // %kernel.root_dir%/../web/%webpath_modelmasks%
        if(parameterName.contains("/") || parameterName.contains("..")) {
            return false;
        }
        
        // more than 2x "%" is invalid
        return !parameterName.substring(1, parameterName.length() - 1).contains("%");

    }

    @NotNull
    public static String trimSpecialSyntaxServiceName(@NotNull String serviceName) {

        if(serviceName.startsWith("@")) {
            serviceName = serviceName.substring(1);
        }

        // yaml strict syntax
        if(serviceName.endsWith("=")) {
            serviceName = serviceName.substring(0, serviceName.length() -1);
        }

        // optional syntax
        if(serviceName.startsWith("?")) {
            serviceName = serviceName.substring(1, serviceName.length());
        }

        return serviceName;

    }

    @Nullable
    public static String getYamlKeyName(@NotNull YAMLKeyValue yamlKeyValue) {

        PsiElement modelName = yamlKeyValue.getKey();
        if(modelName == null) {
            return null;
        }

        String keyName = StringUtils.trim(modelName.getText());
        if(keyName.endsWith(":")) {
            keyName = StringUtils.trim((keyName.substring(0, keyName.length() - 1)));
        }

        return keyName;
    }

    @Nullable
    public static Set<String> getKeySet(@Nullable YAMLKeyValue yamlKeyValue) {

        if(yamlKeyValue == null) {
            return null;
        }

        PsiElement yamlCompoundValue = yamlKeyValue.getValue();
        if(yamlCompoundValue == null) {
            return null;
        }

        Set<String> keySet = new HashSet<>();
        for(YAMLKeyValue yamlKey: PsiTreeUtil.getChildrenOfTypeAsList(yamlCompoundValue, YAMLKeyValue.class)) {
            String fieldName = getYamlKeyName(yamlKey);
            if(fieldName != null) {
                keySet.add(fieldName);
            }
        }

        return keySet;
    }

    @Nullable
    public static YAMLKeyValue getYamlKeyValue(@NotNull YAMLMapping yamlHash, String keyName) {
        return getYamlKeyValue(yamlHash, keyName, false);
    }

    /**
     * test:
     *   DEBUG_WEB_1: 1
     *   DEBUG_WEB_2: 1
     */
    @NotNull
    public static Map<String, YAMLValue> getYamlArrayKeyMap(@NotNull YAMLMapping yamlHash) {
        Map<String, YAMLValue> keys = new HashMap<>();

        for(YAMLKeyValue yamlKeyValue: PsiTreeUtil.getChildrenOfAnyType(yamlHash, YAMLKeyValue.class)) {
            String keyText = yamlKeyValue.getKeyText();
            if (StringUtils.isNotBlank(keyText)) {
                YAMLValue value = yamlKeyValue.getValue();
                if (value != null) {
                    keys.put(keyText, value);
                }
            }
        }

        return keys;
    }

    @Nullable
    public static String getYamlKeyValueAsString(@NotNull YAMLMapping yamlHash, @NotNull String keyName) {
        YAMLKeyValue yamlKeyValue = getYamlKeyValue(yamlHash, keyName, false);
        if(yamlKeyValue == null) {
            return null;
        }

        final String valueText = yamlKeyValue.getValueText();
        if(StringUtils.isBlank(valueText)) {
            return null;
        }

        return valueText;
    }


    @Nullable
    public static YAMLKeyValue getYamlKeyValue(@Nullable YAMLKeyValue yamlKeyValue, @NotNull String keyName) {
        return getYamlKeyValue(yamlKeyValue, keyName, false);
    }

    /**
     *  foo:
     *     class: "name"
     */
    @Nullable
    public static String getYamlKeyValueAsString(@NotNull YAMLKeyValue yamlKeyValue, @NotNull String keyName) {
        return getYamlKeyValueAsString(yamlKeyValue, keyName, false);
    }

    @Nullable
    public static String getYamlKeyValueAsString(@NotNull YAMLKeyValue yamlKeyValue, @NotNull String keyName, boolean ignoreCase) {

        PsiElement yamlCompoundValue = yamlKeyValue.getValue();
        if(!(yamlCompoundValue instanceof YAMLCompoundValue)) {
            return null;
        }

        return getYamlKeyValueAsString((YAMLCompoundValue) yamlCompoundValue, keyName, ignoreCase);
    }

    @Nullable
    public static String getYamlKeyValueAsString(@Nullable YAMLCompoundValue yamlCompoundValue, String keyName, boolean ignoreCase) {
        YAMLKeyValue yamlKeyValue1 = getYamlKeyValue(yamlCompoundValue, keyName, ignoreCase);

        if(yamlKeyValue1 == null) {
            return null;
        }

        String valueText = yamlKeyValue1.getValueText();
        if (StringUtils.isBlank(valueText)) {
            return null;
        }

        return valueText;
    }

    @Nullable
    public static YAMLKeyValue getYamlKeyValue(@Nullable YAMLKeyValue yamlKeyValue, String keyName, boolean ignoreCase) {
        if(yamlKeyValue == null) {
            return null;
        }

        PsiElement yamlCompoundValue = yamlKeyValue.getValue();
        if(!(yamlCompoundValue instanceof YAMLCompoundValue)) {
            return null;
        }

        return getYamlKeyValue(yamlCompoundValue, keyName, ignoreCase);
    }


    /**
     * foo:
     *   bar:
     *     |
     *
     *  Will return [foo, bar]
     *
     * todo: YAMLUtil.getFullKey is useless because its not possible to prefix self item value and needs array value
     * @param psiElement any PsiElement inside a key value
     */
    public static List<String> getParentArrayKeys(PsiElement psiElement) {
        List<String> keys = new ArrayList<>();

        YAMLKeyValue yamlKeyValue = PsiTreeUtil.getParentOfType(psiElement, YAMLKeyValue.class);
        if(yamlKeyValue != null) {
            getParentArrayKeys(yamlKeyValue, keys);
        }

        return keys;
    }


    /**
     * Attach all parent array keys to list (foo:\n bar:): [foo, bar]
     *
     * @param yamlKeyValue current key value context
     * @param key the key list
     */
    public static void getParentArrayKeys(YAMLKeyValue yamlKeyValue, List<String> key) {
        key.add(yamlKeyValue.getKeyText());

        PsiElement yamlCompount = yamlKeyValue.getParent();
        if(yamlCompount instanceof YAMLCompoundValue) {
            PsiElement yamlKeyValueParent = yamlCompount.getParent();
            if(yamlKeyValueParent instanceof YAMLKeyValue) {
                getParentArrayKeys((YAMLKeyValue) yamlKeyValueParent, key);
            }
        }

    }

    /**
     * Migrate to processKeysAfterRoot @TODO
     *
     * @param keyContext Should be Document or YAMLCompoundValueImpl which holds the key value children
     */
    public static void attachDuplicateKeyInspection(PsiElement keyContext, @NotNull ProblemsHolder holder) {

        Map<String, PsiElement> psiElementMap = new HashMap<>();
        Set<PsiElement> yamlKeyValues = new HashSet<>();

        Collection<YAMLKeyValue> collection = PsiTreeUtil.getChildrenOfTypeAsList(keyContext, YAMLKeyValue.class);
        for(YAMLKeyValue yamlKeyValue: collection) {
            String keyText = PsiElementUtils.trimQuote(yamlKeyValue.getKeyText());
            if(StringUtils.isNotBlank(keyText)) {
                if(psiElementMap.containsKey(keyText)) {
                    yamlKeyValues.add(psiElementMap.get(keyText));
                    yamlKeyValues.add(yamlKeyValue);
                } else {
                    psiElementMap.put(keyText, yamlKeyValue);
                }

            }

        }

        if(yamlKeyValues.size() > 0) {
            for(PsiElement psiElement: yamlKeyValues) {
                if(psiElement instanceof YAMLKeyValue) {
                    final PsiElement keyElement = ((YAMLKeyValue) psiElement).getKey();
                    assert keyElement != null;
                    holder.registerProblem(keyElement, "Duplicate key", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
                }
            }
        }

    }

    /**
     * Process yaml key in second level filtered by a root:
     * File > roots -> "Item"
     * TODO: visitQualifiedKeyValuesInFile
     */
    public static void processKeysAfterRoot(@NotNull PsiFile psiFile, @NotNull Processor<YAMLKeyValue> yamlKeyValueProcessor, @NotNull String... roots) {
        for (String root : roots) {
            YAMLKeyValue yamlKeyValue = YAMLUtil.getQualifiedKeyInFile((YAMLFile) psiFile, root);
            if(yamlKeyValue != null) {
                YAMLCompoundValue yaml = PsiTreeUtil.findChildOfType(yamlKeyValue, YAMLCompoundValue.class);
                if(yaml != null) {
                    for(YAMLKeyValue yamlKeyValueVisit: PsiTreeUtil.getChildrenOfTypeAsList(yaml, YAMLKeyValue.class)) {
                        yamlKeyValueProcessor.process(yamlKeyValueVisit);
                    }
                }
            }
        }
    }

    public static boolean isRoutingFile(PsiFile psiFile) {
        return psiFile.getName().contains("routing") || psiFile.getVirtualFile().getPath().contains("/routing");
    }

    public static boolean isConfigFile(@NotNull PsiFile psiFile) {
        return psiFile.getName().contains("config") || psiFile.getVirtualFile().getPath().contains("/config");
    }

    public static boolean isServicesFile(@NotNull PsiFile psiFile) {
        return psiFile.getName().contains("services") || psiFile.getVirtualFile().getPath().contains("/services");
    }

    public static boolean isInsideServiceDefinition(@NotNull PsiElement psiElement) {
        return YamlElementPatternHelper.getInsideServiceKeyPattern().accepts(psiElement);
    }

    public static boolean isInsideServiceArgumentDefinition(@NotNull PsiElement psiElement) {
        return isInsideServiceDefinition(psiElement)
            && YamlElementPatternHelper.getInsideKeyValue("arguments", "properties", "calls").accepts(psiElement);
    }

    /**
     * foo.service.method:
     *   class: "ClassName\Foo"
     *   arguments:
     *     - "@twig"
     *     - '@twig'
     *   tags:
     *     -  { name: routing.loader, method: "crossHint<caret>" }
     *
     * ClassName\Foo:
     *   tags:
     *     -  { method: "crossHint<caret>" }
     */
    @Nullable
    public static String getServiceDefinitionClassFromTagMethod(@NotNull PsiElement psiElement) {
        PsiElement yamlScalar = psiElement.getParent();
        if(yamlScalar instanceof YAMLScalar) {
            PsiElement yamlKeyValue = yamlScalar.getParent();
            if(yamlKeyValue instanceof YAMLKeyValue) {
                // "{ method: '' }"
                PsiElement yamlMapping = ((YAMLKeyValue) yamlKeyValue).getParentMapping();
                if(yamlMapping != null) {
                    PsiElement yamlSequenceItem = yamlMapping.getParent();
                    if(yamlSequenceItem instanceof YAMLSequenceItem) {
                        PsiElement yamlSequence = yamlSequenceItem.getParent();
                        if(yamlSequence instanceof YAMLSequence) {
                            PsiElement yamlKeyValueTags = yamlSequence.getParent();
                            if(yamlKeyValueTags instanceof YAMLKeyValue) {
                                return getClassFromServiceDefinition((YAMLKeyValue) yamlKeyValueTags);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     *  Simplify getting of array psi elements in array or sequence context
     *
     * arguments: [@foo]
     * arguments:
     *   - @foo
     *
     * TODO: can be handled nice know because on new yaml plugin
     */
    @Nullable
    public static List<PsiElement> getYamlArrayOnSequenceOrArrayElements(@NotNull YAMLCompoundValue yamlCompoundValue) {
        if (yamlCompoundValue instanceof YAMLSequence) {
            return new ArrayList<>(((YAMLSequence) yamlCompoundValue).getItems());
        }

        if (yamlCompoundValue instanceof YAMLMapping) {
            return new ArrayList<>(((YAMLMapping) yamlCompoundValue).getKeyValues());
        }

        return null;
    }

    /**
     * Finds top most service of any given PsiElement context
     *
     * @param psiElement any PsiElement that is inside an service definition
     */
    @Nullable
    public static YAMLKeyValue findServiceInContext(@NotNull PsiElement psiElement) {

        YAMLKeyValue serviceSubKey = PsiTreeUtil.getParentOfType(psiElement, YAMLKeyValue.class);
        if(serviceSubKey == null) {
            return null;
        }

        PsiElement serviceSubKeyCompound = serviceSubKey.getParent();

        // we are inside a YAMLHash element, find most parent array key
        // { name: foo }
        if(serviceSubKeyCompound instanceof YAMLHashImpl) {
            YAMLKeyValue yamlKeyValue = PsiTreeUtil.getParentOfType(serviceSubKeyCompound, YAMLKeyValue.class);
            if(yamlKeyValue == null) {
                return null;
            }

            serviceSubKeyCompound = yamlKeyValue.getParent();
        }

        // find array key inside service and check if we are inside "services"
        if(serviceSubKeyCompound instanceof YAMLCompoundValue) {
            PsiElement serviceKey = serviceSubKeyCompound.getParent();
            if(serviceKey instanceof YAMLKeyValue) {
                PsiElement servicesKeyCompound = serviceKey.getParent();
                if(servicesKeyCompound instanceof YAMLCompoundValue) {
                    PsiElement servicesKey = servicesKeyCompound.getParent();
                    if(servicesKey instanceof YAMLKeyValue) {
                        if("services".equals(((YAMLKeyValue) servicesKey).getKeyText())) {
                            return (YAMLKeyValue) serviceKey;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Collect defined service tags on a sequence list
     * - { name: assetic.factory_worker }
     * - [ assetic.factory_worker ]
     *
     * @param yamlKeyValue the service key value to find the "tags" key on
     * @return tag names
     */
    @Nullable
    public static Set<String> collectServiceTags(@NotNull YAMLKeyValue yamlKeyValue) {

        YAMLKeyValue tagsKeyValue = YamlHelper.getYamlKeyValue(yamlKeyValue, "tags");
        if(tagsKeyValue == null) {
            return null;
        }

        PsiElement tagsCompound = tagsKeyValue.getValue();
        if(!(tagsCompound instanceof YAMLSequence)) {
            return null;
        }

        Set<String> tags = new HashSet<>();

        for (YAMLSequenceItem yamlSequenceItem : ((YAMLSequence) tagsCompound).getItems()) {

            YAMLValue value = yamlSequenceItem.getValue();
            if(value instanceof YAMLMapping) {
                // tags:
                //  - {name: foobar}

                String name = YamlHelper.getYamlKeyValueAsString(((YAMLMapping) value), "name");
                if(name != null) {
                    tags.add(name);
                }
            } else if(value instanceof YAMLScalar) {
                // tags: [foobar]

                String textValue = ((YAMLScalar) value).getTextValue();
                if(StringUtils.isNotBlank(textValue)) {
                    tags.add(textValue);
                }
            }
        }

        return tags;
    }

    /**
     * TODO: use visitor pattern for all tags, we are using them to often
     */
    public static void visitTagsOnServiceDefinition(@NotNull YAMLKeyValue yamlServiceKeyValue, @NotNull YamlTagVisitor visitor) {

        YAMLKeyValue tagTag = YamlHelper.getYamlKeyValue(yamlServiceKeyValue, "tags");
        if(tagTag == null) {
            return;
        }

        final YAMLValue tagsValue = tagTag.getValue();
        if(!(tagsValue instanceof YAMLSequence)) {
            return;
        }

        String serviceId = yamlServiceKeyValue.getKeyText();

        for(YAMLSequenceItem yamlSequenceItem: ((YAMLSequence) tagsValue).getItems()) {
            final YAMLValue itemValue = yamlSequenceItem.getValue();

            if(itemValue instanceof YAMLMapping) {
                // tags:
                //  - {name: foobar}

                final YAMLMapping yamlHash = (YAMLMapping) itemValue;
                String tagName = YamlHelper.getYamlKeyValueAsString(yamlHash, "name");
                if(tagName != null) {
                    visitor.visit(new YamlServiceTag(serviceId, tagName, yamlHash));
                }
            } else if(itemValue instanceof YAMLScalar) {
                // tags: [foobar]

                String textValue = ((YAMLScalar) itemValue).getTextValue();
                if(StringUtils.isNotBlank(textValue)) {
                    visitor.visit(new YamlServiceTag(serviceId, textValue, new StaticAttributeResolver("name", textValue)));
                }
            }
        }
    }

    /**
     * Get all children key values of a parent key value
     */
    @NotNull
    private static Collection<YAMLKeyValue> getNextKeyValues(@NotNull YAMLKeyValue yamlKeyValue) {

        final Collection<YAMLKeyValue> yamlKeyValues = new ArrayList<>();
        visitNextKeyValues(yamlKeyValue, yamlKeyValues::add);

        return yamlKeyValues;
    }

    /**
     * Visit all children key values of a parent key value
     */
    private static void visitNextKeyValues(@NotNull YAMLKeyValue yamlKeyValue, @NotNull Consumer<YAMLKeyValue> consumer) {
        List<YAMLPsiElement> yamlElements = yamlKeyValue.getYAMLElements();

        // @TODO: multiple?
        if(yamlElements.size() != 1) {
            return;
        }

        YAMLPsiElement next = yamlElements.iterator().next();
        if(!(next instanceof YAMLMapping)) {
            return;
        }

        for (YAMLKeyValue keyValue : ((YAMLMapping) next).getKeyValues()) {
            consumer.consume(keyValue);
        }
    }


    /**
     * Get all key values in first level key visit
     *
     * parameters:
     *  foo: "foo"
     */
    @NotNull
    public static Collection<YAMLKeyValue> getQualifiedKeyValuesInFile(@NotNull YAMLFile yamlFile, @NotNull String firstLevelKeyToVisit) {
        YAMLKeyValue parameters = YAMLUtil.getQualifiedKeyInFile(yamlFile, firstLevelKeyToVisit);
        if(parameters == null) {
            return Collections.emptyList();
        }

        return getNextKeyValues(parameters);
    }

    /**
     * Visit all key values in first level key
     *
     * parameters:
     *  foo: "foo"
     */
    public static void visitQualifiedKeyValuesInFile(@NotNull YAMLFile yamlFile, @NotNull String firstLevelKeyToVisit, @NotNull Consumer<YAMLKeyValue> consumer) {
        YAMLKeyValue parameters = YAMLUtil.getQualifiedKeyInFile(yamlFile, firstLevelKeyToVisit);
        if(parameters == null) {
            return;
        }

        visitNextKeyValues(parameters, consumer);
    }

    @Nullable
    public static String getStringValueOfKeyInProbablyMapping(@Nullable YAMLValue node, @NotNull String keyText) {
        YAMLKeyValue mapping = YAMLUtil.findKeyInProbablyMapping(node, keyText);
        if(mapping == null) {
            return null;
        }

        YAMLValue value = mapping.getValue();
        if(value == null) {
            return null;
        }

        return value.getText();
    }

    @NotNull
    public static Collection<YAMLKeyValue> getTopLevelKeyValues(@NotNull YAMLFile yamlFile) {
        YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(yamlFile, YAMLDocument.class);
        if(yamlDocument == null) {
            return Collections.emptyList();
        }

        YAMLValue topLevelValue = yamlDocument.getTopLevelValue();
        if(!(topLevelValue instanceof YAMLMapping)) {
            return Collections.emptyList();
        }

        return ((YAMLMapping) topLevelValue).getKeyValues();
    }

    /**
     * Returns "@foo" value of ["@foo", "fo<caret>o"]
     */
    @Nullable
    public static String getPreviousSequenceItemAsText(@NotNull PsiElement psiElement) {
        PsiElement yamlScalar = psiElement.getParent();
        if(!(yamlScalar instanceof YAMLScalar)) {
            return null;
        }

        PsiElement yamlSequence = yamlScalar.getParent();
        if(!(yamlSequence instanceof YAMLSequenceItem)) {
            return null;
        }

        // @TODO: catch new lexer error on empty item [<caret>,@foo] "PsiErrorElement:Sequence item expected"
        YAMLSequenceItem prevSequenceItem = PsiTreeUtil.getPrevSiblingOfType(yamlSequence, YAMLSequenceItem.class);
        if(prevSequenceItem == null) {
            return null;
        }

        YAMLValue value = prevSequenceItem.getValue();
        if(!(value instanceof YAMLScalar)) {
            return null;
        }

        return ((YAMLScalar) value).getTextValue();
    }

    private interface KeyInsertValueFormatter {
        @Nullable
        String format(@NotNull YAMLMapping yamlMapping, @NotNull String chainedKey);
    }

    /**
     * Adds a yaml key on path. This implemention merge values and support nested key values
     * foo:\n  bar: car -> foo.car.foo.bar
     *
     * @param formatter any string think of provide qoute
     */
    @Nullable
    public static PsiElement insertKeyIntoFile(final @NotNull YAMLFile yamlFile, @NotNull KeyInsertValueFormatter formatter, @NotNull String... keys) {
        final Pair<YAMLKeyValue, String[]> lastKeyStorage = findLastKnownKeyInFile(yamlFile, keys);

        if(lastKeyStorage.getSecond().length == 0) {
            return null;
        }

        YAMLMapping childOfType = null;

        // root condition
        if(lastKeyStorage.getFirst() == null && lastKeyStorage.getSecond().length == keys.length) {
            YAMLValue topLevelValue = yamlFile.getDocuments().get(0).getTopLevelValue();
            if(topLevelValue instanceof YAMLMapping) {
                childOfType = (YAMLMapping) topLevelValue;
            }
        } else if(lastKeyStorage.getFirst() != null) {
            // found a key value in key path append it there
            childOfType = PsiTreeUtil.getChildOfType(lastKeyStorage.getFirst(), YAMLMapping.class);
        }

        if(childOfType == null) {
            return null;
        }

        // pre-generate an empty key value
        String chainedKey = YAMLElementGenerator.createChainedKey(Arrays.asList(lastKeyStorage.getSecond()), YAMLUtil.getIndentInThisLine(childOfType));

        // append value: should be string with right indent for key value
        String value = formatter.format(childOfType, chainedKey);
        if(value != null) {
            chainedKey += value;
        }

        YAMLFile dummyFile = YAMLElementGenerator.getInstance(yamlFile.getProject()).createDummyYamlWithText(chainedKey);

        final YAMLKeyValue next = PsiTreeUtil.collectElementsOfType(dummyFile, YAMLKeyValue.class).iterator().next();
        if(next == null) {
            return null;
        }

        // finally wirte changes
        final YAMLMapping finalChildOfType = childOfType;
        new WriteCommandAction(yamlFile.getProject()) {
            @Override
            protected void run(@NotNull Result result) throws Throwable {
                finalChildOfType.putKeyValue(next);
            }

            @Override
            public String getGroupID() {
                return "Key insertion";
            }
        }.execute();

        return childOfType;
    }

    @Nullable
    public static PsiElement insertKeyIntoFile(final @NotNull YAMLFile yamlFile, final @NotNull YAMLKeyValue yamlKeyValue, @NotNull String... keys) {
        String keyText = yamlKeyValue.getKeyText();

        return insertKeyIntoFile(yamlFile, (yamlMapping, chainedKey) -> {
            String text = yamlKeyValue.getText();

            final String previousIndent = StringUtil.repeatSymbol(' ', YAMLUtil.getIndentInThisLine(yamlMapping));

            // split content of array value object;
            // drop first item as getValueText() removes our key indent
            String[] remove = (String[]) ArrayUtils.remove(text.split("\\r?\\n"), 0);

            List<String> map = ContainerUtil.map(remove, s -> previousIndent + s);

            return "\n" + StringUtils.strip(StringUtils.join(map, "\n"), "\n");
        }, (String[]) ArrayUtils.add(keys, keyText));
    }

    public static PsiElement insertKeyIntoFile(final @NotNull YAMLFile yamlFile, final @Nullable String value, @NotNull String... keys) {
        return insertKeyIntoFile(yamlFile, (yamlMapping, chainedKey) -> " " + value, keys);
    }

    /**
     * Find last known KeyValue of key path, so that we can merge new incoming keys
     */
    @NotNull
    private static Pair<YAMLKeyValue, String[]> findLastKnownKeyInFile(@NotNull YAMLFile file, @NotNull String... keys) {

        YAMLKeyValue last = null;
        YAMLMapping mapping = ObjectUtils.tryCast(file.getDocuments().get(0).getTopLevelValue(), YAMLMapping.class);

        for (int i = 0; i < keys.length; i++) {
            String s = keys[i];
            if (mapping == null) {
                return Pair.create(last, Arrays.copyOfRange(keys, i, keys.length));
            }

            YAMLKeyValue keyValue = mapping.getKeyValueByKey(s);
            if (keyValue == null) {
                return Pair.create(last, Arrays.copyOfRange(keys, i, keys.length));
            }

            last = keyValue;

            mapping = ObjectUtils.tryCast(keyValue.getValue(), YAMLMapping.class);
        }

        return Pair.create(last, new String[]{});
    }

    /**
     * Bridge to allow YAMLKeyValue adding child key-values elements.
     * Yaml plugin provides key adding only on YAMLMapping
     *
     * ser<caret>vice:
     *   foo: "aaa"
     *
     */
    @Nullable
    public static YAMLKeyValue putKeyValue(@NotNull YAMLKeyValue yamlKeyValue, @NotNull String keyName, @NotNull String valueText) {

        // create "foo: foo"
        YAMLKeyValue newYamlKeyValue = YAMLElementGenerator.getInstance(yamlKeyValue.getProject())
            .createYamlKeyValue(keyName, valueText);

        YAMLMapping childOfAnyType = PsiTreeUtil.findChildOfAnyType(yamlKeyValue, YAMLMapping.class);
        if(childOfAnyType == null) {
            return null;
        }

        childOfAnyType.putKeyValue(newYamlKeyValue);

        return newYamlKeyValue;
    }

    /**
     * Services id in Symfony 3.3 are allowed to be class names
     * defensive extract by naming strategy
     */
    public static boolean isClassServiceId(@NotNull String serviceId) {
        // foo.bar
        if(serviceId.contains(".")) {
            return false;
        }

        // fallback let decide PhpStorm if this is a valid class
        return PhpNameUtil.isValidNamespaceFullName(serviceId, true);
    }

    /**
     * service_name:
     *   class: FOOBAR
     *   calls:
     *      - [onF<caret>oobar, []]
     *
     * FOOBAR:
     *   calls:
     *      - [onF<caret>oobar, []]
     */
    public static void visitServiceCall(@NotNull YAMLScalar yamlScalar, @NotNull Consumer<String> consumer) {
        PsiElement yamlSeq = yamlScalar.getContext();
        if(yamlSeq instanceof YAMLSequenceItem) {
            PsiElement context = yamlSeq.getContext();
            if(context instanceof YAMLSequence) {
                PsiElement yamlSequenceItem = context.getParent();
                if(yamlSequenceItem instanceof YAMLSequenceItem) {
                    PsiElement yamlSeq1 = yamlSequenceItem.getParent();
                    if(yamlSeq1 instanceof YAMLSequence) {
                        PsiElement callYamlKeyValue = yamlSeq1.getParent();
                        if(callYamlKeyValue instanceof YAMLKeyValue) {
                            YAMLKeyValue classKeyValue = YamlHelper.getYamlKeyValue(callYamlKeyValue.getContext(), "class");
                            if(classKeyValue != null) {
                                // "class" key found use this as valid class name
                                String valueText = classKeyValue.getValueText();
                                if(StringUtils.isNotBlank(valueText)) {
                                    consumer.consume(valueText);
                                }
                            } else {
                                // named services; key is our class name
                                PsiElement yamlMapping = callYamlKeyValue.getParent();
                                if(yamlMapping instanceof YAMLMapping) {
                                    PsiElement parent = yamlMapping.getParent();
                                    if(parent instanceof YAMLKeyValue) {
                                        String keyText = ((YAMLKeyValue) parent).getKeyText();
                                        if(!keyText.contains(".") && PhpNameUtil.isValidNamespaceFullName(keyText)) {
                                            consumer.consume(keyText);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get class from server definition; supports shortcut
     *
     * service.id:
     *  class: MyClass
     *
     * MyClass: ~
     */
    private static String getClassFromServiceDefinition(@NotNull YAMLKeyValue yamlKeyValue) {
        YAMLMapping parentMapping = yamlKeyValue.getParentMapping();

        if(parentMapping != null) {
            YAMLKeyValue classKeyValue = parentMapping.getKeyValueByKey("class");
            if (classKeyValue != null) {
                String valueText = classKeyValue.getValueText();
                if (StringUtils.isNotBlank(valueText)) {
                    return valueText;
                }
            } else {
                // named services; key is our class name
                PsiElement yamlMapping = yamlKeyValue.getParent();
                if(yamlMapping instanceof YAMLMapping) {
                    PsiElement parent = yamlMapping.getParent();
                    if(parent instanceof YAMLKeyValue) {
                        String keyText = ((YAMLKeyValue) parent).getKeyText();
                        if(StringUtils.isNotBlank(keyText) && !keyText.contains(".") && PhpNameUtil.isValidNamespaceFullName(keyText)) {
                            return keyText;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * service_name:
     *   class: FOOBAR
     *   calls:
     *      - [onFoobar, [@fo<caret>o]]
     *
     * FOOBAR:
     *   calls:
     *      - [onFoobar, [@fo<caret>o]]
     */
    public static void visitServiceCallArgument(@NotNull YAMLScalar yamlScalar, @NotNull Consumer<ParameterVisitor> consumer) {
        PsiElement context = yamlScalar.getContext();
        if(context instanceof YAMLSequenceItem) {
            // [@foobar, @fo<caret>obar]
            YAMLSequenceItem argumentSequenceItem = (YAMLSequenceItem) context;
            if (argumentSequenceItem.getContext() instanceof YAMLSequence) {
                YAMLSequence yamlCallParameterArray = (YAMLSequence) argumentSequenceItem.getContext();
                PsiElement callSequenceItem = yamlCallParameterArray.getContext();
                if(callSequenceItem instanceof YAMLSequenceItem) {
                    YAMLSequenceItem enclosingItem = (YAMLSequenceItem) callSequenceItem;
                    if (enclosingItem.getContext() instanceof YAMLSequence) {
                        YAMLSequence yamlCallArray = (YAMLSequence) enclosingItem.getContext();
                        PsiElement seqItem = yamlCallArray.getContext();
                        if(seqItem instanceof YAMLSequenceItem) {
                            // - [ setFoo, [@args_bar] ]
                            PsiElement callYamlSeq = seqItem.getContext();
                            if(callYamlSeq instanceof YAMLSequence) {
                                // only given method and args are valid "setFoo, [@args_bar]"
                                List<YAMLSequenceItem> methodParameter = yamlCallArray.getItems();
                                if(methodParameter.size() > 1) {
                                    YAMLValue methodNameElement = methodParameter.get(0).getValue();
                                    if(methodNameElement instanceof YAMLScalar) {
                                        String methodName = ((YAMLScalar) methodNameElement).getTextValue();
                                        if(StringUtils.isNotBlank(methodName)) {
                                            PsiElement callYamlKeyValue = callYamlSeq.getContext();
                                            if(callYamlKeyValue instanceof YAMLKeyValue) {
                                                String classFromServiceDefinition = getClassFromServiceDefinition((YAMLKeyValue) callYamlKeyValue);
                                                if(classFromServiceDefinition != null) {
                                                    consumer.consume(new ParameterVisitor(
                                                        classFromServiceDefinition,
                                                        methodName,
                                                        PsiElementUtils.getPrevSiblingsOfType(argumentSequenceItem, PlatformPatterns.psiElement(YAMLSequenceItem.class)).size())
                                                    );
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Consumer for method parameter match
     *
     * service_name:
     *   class: FOOBAR
     *   calls:
     *      - [onFoobar, [@fo<caret>o]]
     */
    public static void visitServiceCallArgumentMethodIndex(@NotNull YAMLScalar yamlScalar, @NotNull Consumer<Parameter> consumer) {
        YamlHelper.visitServiceCallArgument(yamlScalar, new ParameterResolverConsumer(yamlScalar.getProject(), consumer));
    }

    /**
     * Get false or true value for given key or null if not found or invalid value was found
     */
    @Nullable
    public static Boolean getYamlKeyValueAsBoolean(@NotNull YAMLKeyValue yamlKeyValue, @NotNull String key) {
        YAMLKeyValue autowire = YamlHelper.getYamlKeyValue(yamlKeyValue, key);
        if(autowire == null) {
            return null;
        }

        YAMLValue value = autowire.getValue();
        if(!(value instanceof YAMLScalar)) {
            return null;
        }

        String textValue = ((YAMLScalar) value).getTextValue().toLowerCase();
        switch (textValue) {
            case "false":
                return false;
            case "true":
                return true;
            default:
                return null;
        }
    }

    /**
     * Try to find a valid indent value, which are spaces which we need to fill
     */
    public static int getIndentSpaceForFile(@NotNull YAMLFile yamlFile) {
        List<YAMLDocument> documents = yamlFile.getDocuments();

        YAMLMapping mapping = ObjectUtils.tryCast(documents.get(0).getTopLevelValue(), YAMLMapping.class);
        if(mapping != null) {
            // first first INDENT element in mapping
            PsiElementPattern.Capture<PsiElement> pattern = PlatformPatterns
                .psiElement(YAMLTokenTypes.INDENT)
                .with(new PsiElementPatternCondition());

            for (YAMLPsiElement yamlPsiElement : mapping.getKeyValues()) {
                // get first value
                PsiElement firstChild = yamlPsiElement.getFirstChild();
                if(firstChild == null) {
                    continue;
                }

                // first valid INDENT
                PsiElement nextSiblingOfType = PsiElementUtils.getNextSiblingOfType(firstChild, pattern);
                if(nextSiblingOfType != null && nextSiblingOfType.getTextLength() > 0) {
                    return nextSiblingOfType.getTextLength();
                }
            }
        }

        // default value
        return 4;
    }

    public static boolean isStringValue(@NotNull PsiElement psiElement) {
        // @TODO use new YAMLScalar element
        return PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement)
            || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING).accepts(psiElement)
            ;
    }

    private static class PsiElementPatternCondition extends PatternCondition<PsiElement> {
        PsiElementPatternCondition() {
            super("Indent Check");
        }

        @Override
        public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
            return psiElement.getNextSibling() instanceof YAMLMapping;
        }
    }

    @NotNull
    public static Collection<PhpClass> getPhpClassesInYamlFile(@NotNull YAMLFile yamlFile, @NotNull ContainerCollectionResolver.LazyServiceCollector lazyServiceCollector) {
        Collection<PhpClass> phpClasses = new HashSet<>();

        for (YAMLKeyValue keyValue : YamlHelper.getQualifiedKeyValuesInFile(yamlFile, "services")) {
            YAMLValue value = keyValue.getValue();
            if (value instanceof YAMLMapping) {
                // foo.bar:
                //    classes: ...
                String serviceId = ServiceContainerUtil.getServiceClassFromServiceMapping((YAMLMapping) value);
                if (StringUtils.isNotBlank(serviceId)) {
                    PhpClass serviceClass = ServiceUtil.getResolvedClassDefinition(yamlFile.getProject(), serviceId, lazyServiceCollector);
                    if (serviceClass != null) {
                        phpClasses.add(serviceClass);
                    }
                }
            } else if(value instanceof YAMLPlainTextImpl) {
                // Foo\Bar: ~
                String text = keyValue.getKeyText();

                if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text)) {
                    phpClasses.addAll(PhpElementsUtil.getClassesInterface(yamlFile.getProject(), text));
                }
            }
        }

        return phpClasses;
    }

    /**
     * key: !my_tag <caret>
     */
    public static boolean isElementAfterYamlTag(PsiElement psiElement) {
        if (!(psiElement instanceof LeafPsiElement)) {
            return false;
        }

        // key: !my_tag <caret>\n
        if (((LeafPsiElement) psiElement).getElementType() == YAMLTokenTypes.EOL) {
            PsiElement prevElement = PsiTreeUtil.getDeepestVisibleLast(psiElement);
            if (prevElement instanceof LeafPsiElement) {
                if (((LeafPsiElement) prevElement).getElementType() == YAMLTokenTypes.TAG) {
                    return ((LeafPsiElement) prevElement).getText().startsWith("!");
                }
            }
        }

        return PsiTreeUtil.findSiblingBackward(psiElement, YAMLTokenTypes.TAG, null) != null;
    }

    /**
     * key: foo\n
     * <caret>
     */
    public static boolean isElementAfterEol(PsiElement psiElement) {
        if (psiElement.getParent() instanceof YAMLPlainTextImpl) {
            psiElement = psiElement.getParent();
        }
        return PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(YAMLTokenTypes.EOL)) != null;
    }
}
