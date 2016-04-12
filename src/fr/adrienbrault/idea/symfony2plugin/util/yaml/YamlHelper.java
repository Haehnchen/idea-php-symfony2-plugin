package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.Pair;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlServiceTag;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.visitor.YamlTagVisitor;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;
import org.jetbrains.yaml.psi.impl.YAMLHashImpl;

import java.util.*;

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
        List<PsiElement> psiElements = new ArrayList<PsiElement>();

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
     *  Try to find psi value which match should be a array value and filter out comma, whitespace...
     *  [@service, "@service2", [""], ['']];
     *
     *  TODO: drop this hack; included in core now
     */
    @NotNull
    static public List<YAMLSequenceItem> getYamlArrayValues(@NotNull YAMLSequence yamlArray) {
        return yamlArray.getItems();
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
            Map<String, String> map = new HashMap<String, String>();

            for(YAMLKeyValue yamlParameterArray: getQualifiedKeyValuesInFile((YAMLFile) psiFile, "parameters")) {
                String keyName = yamlParameterArray.getKeyText();
                if(StringUtils.isBlank(keyName)) {
                    continue;
                }

                PsiElement value = yamlParameterArray.getValue();
                if(value != null) {
                    String valueText = value.getText();
                    if(StringUtils.isNotBlank(valueText)) {
                        map.put(keyName.toLowerCase(), PsiElementUtils.trimQuote(valueText));
                    }

                }
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

            final Collection<PsiElement> psiElements = new ArrayList<PsiElement>();

            visitQualifiedKeyValuesInFile((YAMLFile) psiFile, rootKey, new Consumer<YAMLKeyValue>() {
                @Override
                public void consume(YAMLKeyValue yamlKeyValue) {
                    if(findServiceName.equals(yamlKeyValue.getKeyText())) {
                        psiElements.add(yamlKeyValue);
                    }
                }
            });

            if(psiElements.size() == 0) {
                return null;
            }

            // @TODO: provide support for multiple targets
            return psiElements.iterator().next();
        }
    }

    public static Set<String> getYamlCompoundValueKeyNames(YAMLCompoundValue yamlCompoundValue) {

        Set<String> stringSet = new HashSet<String>();

        List<YAMLKeyValue> yamlKeyValues = PsiTreeUtil.getChildrenOfTypeAsList(yamlCompoundValue, YAMLKeyValue.class);

        for(YAMLKeyValue yamlKeyValue: yamlKeyValues) {
            stringSet.add(yamlKeyValue.getKeyText());
        }

        return stringSet;
    }

    public static boolean isValidParameterName(String parameterName) {

        if(parameterName.length() < 3) {
            return false;
        }

        if(!parameterName.startsWith("%") || !parameterName.endsWith("%")) {
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

        Set<String> keySet = new HashSet<String>();
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
        List<String> keys = new ArrayList<String>();

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

        Map<String, PsiElement> psiElementMap = new HashMap<String, PsiElement>();
        Set<PsiElement> yamlKeyValues = new HashSet<PsiElement>();

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

    /**
     * foo.service.method:
     *   class: "ClassName\Foo"
     *   arguments:
     *     - "@twig"
     *     - '@twig'
     *   tags:
     *     -  { name: routing.loader, method: "crossHint<cursor>" }
     *
     */
    @Nullable
    public static String getServiceDefinitionClass(PsiElement psiElement) {

        YAMLHashImpl yamlCompoundValue = PsiTreeUtil.getParentOfType(psiElement, YAMLHashImpl.class);
        if(yamlCompoundValue == null) {
            return null;
        }

        YAMLMapping yamlMapping = PsiTreeUtil.getParentOfType(yamlCompoundValue, YAMLMapping.class);
        if(yamlMapping == null) {
            return null;
        }

        YAMLKeyValue aClass = yamlMapping.getKeyValueByKey("class");
        if(aClass == null) {
            return null;
        }

        return aClass.getValueText();
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
            return new ArrayList<PsiElement>(((YAMLSequence) yamlCompoundValue).getItems());
        }

        if (yamlCompoundValue instanceof YAMLMapping) {
            return new ArrayList<PsiElement>(((YAMLMapping) yamlCompoundValue).getKeyValues());
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

        Set<String> tags = new HashSet<String>();

        for (YAMLSequenceItem yamlSequenceItem : ((YAMLSequence) tagsCompound).getItems()) {

            final YAMLValue value = yamlSequenceItem.getValue();
            if(!(value instanceof YAMLMapping)) {
                continue;
            }

            String name = YamlHelper.getYamlKeyValueAsString(((YAMLMapping) value), "name");
            if(name != null) {
                tags.add(name);
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

        for(YAMLSequenceItem yamlSequenceItem: ((YAMLSequence) tagsValue).getItems()) {
            final YAMLValue itemValue = yamlSequenceItem.getValue();

            if(itemValue instanceof YAMLMapping) {
                final YAMLMapping yamlHash = (YAMLMapping) itemValue;
                String tagName = YamlHelper.getYamlKeyValueAsString(yamlHash, "name");
                if(tagName != null) {
                    visitor.visit(new YamlServiceTag(tagName, yamlHash));
                }
            }
        }
    }

    /**
     * Get all children key values of a parent key value
     */
    @NotNull
    private static Collection<YAMLKeyValue> getNextKeyValues(@NotNull YAMLKeyValue yamlKeyValue) {

        final Collection<YAMLKeyValue> yamlKeyValues = new ArrayList<YAMLKeyValue>();
        visitNextKeyValues(yamlKeyValue, new Consumer<YAMLKeyValue>() {
            @Override
            public void consume(YAMLKeyValue yamlKeyValue) {
                yamlKeyValues.add(yamlKeyValue);
            }
        });

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

    /**
     * Adds a yaml key on path. This implemention merge values and support nested key values
     * foo:\n  bar: car -> foo.car.foo.bar
     *
     * @param value any string think of provide qoute
     */
    public static boolean insertKeyIntoFile(final @NotNull YAMLFile yamlFile, final @Nullable String value, @NotNull String... keys) {
        final Pair<YAMLKeyValue, String[]> lastKeyStorage = findLastKnownKeyInFile(yamlFile, keys);

        if(lastKeyStorage.getSecond().length == 0) {
            return false;
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
            return false;
        }

        // append value to generate key value
        String chainedKey = YAMLElementGenerator.createChainedKey(Arrays.asList(lastKeyStorage.getSecond()), YAMLUtil.getIndentInThisLine(childOfType));
        if(value != null) {
            chainedKey += " " + value;
        }

        YAMLFile dummyFile = YAMLElementGenerator.getInstance(yamlFile.getProject()).createDummyYamlWithText(chainedKey);

        final YAMLKeyValue next = PsiTreeUtil.collectElementsOfType(dummyFile, YAMLKeyValue.class).iterator().next();
        if(next == null) {
            return false;
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
                return "Translation insertion";
            }
        }.execute();

        return true;
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
}
