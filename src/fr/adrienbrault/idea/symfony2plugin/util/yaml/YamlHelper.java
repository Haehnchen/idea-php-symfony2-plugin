package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.dic.ContainerService;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.*;
import org.jetbrains.yaml.psi.impl.YAMLPsiElementImpl;

import java.util.*;

public class YamlHelper {

    static public Map<String, ContainerService> getLocalServiceMap(PsiFile psiElement) {
        return new YamlLocalServiceMap().getLocalServiceMap(psiElement);
    }

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
     */
    static public List<PsiElement> getYamlArrayValues(YAMLArray yamlArray) {


        // split possible element at comma sperator
        HashMap<Integer, ArrayList<PsiElement>> argumentSplitter = new HashMap<Integer, ArrayList<PsiElement>>();
        int currentParameter = 0;
        argumentSplitter.put(currentParameter, new ArrayList<PsiElement>());
        for(PsiElement psiElement: getChildrenFix(yamlArray)) {
            if(psiElement.getText().equals(",")) {
                argumentSplitter.put(++currentParameter, new ArrayList<PsiElement>());
            } else {
                if(!(psiElement instanceof PsiWhiteSpace)) {
                    argumentSplitter.get(currentParameter).add(psiElement);
                }
            }
        }

        // search for valid psi argument value
        List<PsiElement> keys = new ArrayList<PsiElement>();
        for(Map.Entry<Integer, ArrayList<PsiElement>> psiEntry: argumentSplitter.entrySet()) {
            PsiElement parameterPsiElement = null;
            for(PsiElement psiElement: psiEntry.getValue()) {
                if(PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement) || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement) || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_STRING).accepts(psiElement) || PlatformPatterns.psiElement(YAMLTokenTypes.QUESTION).accepts(psiElement)) {
                    parameterPsiElement = psiElement;
                } else if(psiElement instanceof YAMLPsiElementImpl) {
                    parameterPsiElement = psiElement;
                }

            }

            if(parameterPsiElement != null) {
                keys.add(parameterPsiElement);
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

        if(yamlCompoundValue == null) {
            return null;
        }

        YAMLKeyValue classKeyValue;
        if(ignoreCase) {
            classKeyValue = PsiElementUtils.getChildrenOfType(yamlCompoundValue, PlatformPatterns.psiElement(YAMLKeyValue.class).withName(PlatformPatterns.string().oneOfIgnoreCase(keyName)));
        } else {
            classKeyValue = PsiElementUtils.getChildrenOfType(yamlCompoundValue, PlatformPatterns.psiElement(YAMLKeyValue.class).withName(keyName));
        }

        if(classKeyValue == null) {
            return null;
        }

        return classKeyValue;
    }




    public static int getYamlParameter(YAMLArray yamlArray, PsiElement psiKeyElement) {
        int parameter = -1;

        for(PsiElement psiElement: getYamlArrayValues(yamlArray)) {
            parameter++;
            if(psiElement != null && psiElement.equals(psiKeyElement)) {
                return parameter;
            }
        }

        return parameter;
    }

    private static class YamlLocalServiceMap {

        public Map<String, String> getLocalParameterMap(PsiFile psiFile) {

            Map<String, String> map = new HashMap<String, String>();

            YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
            if(yamlDocument == null) {
                return map;
            }

            // get services or parameter key
            YAMLKeyValue[] yamlKeys = PsiTreeUtil.getChildrenOfType(yamlDocument, YAMLKeyValue.class);
            if(yamlKeys == null) {
                return map;
            }

            for(YAMLKeyValue yamlKeyValue : yamlKeys) {
                String yamlConfigKey = yamlKeyValue.getName();
                if(yamlConfigKey != null && yamlConfigKey.equals("parameters")) {

                    YAMLKeyValue yamlParameter[] = PsiTreeUtil.getChildrenOfType(yamlKeyValue.getValue(),YAMLKeyValue.class);
                    if(yamlParameter != null) {

                        for(YAMLKeyValue yamlParameterArray:  yamlParameter) {
                            String keyName = yamlParameterArray.getKeyText();
                            if(StringUtils.isNotBlank(keyName)) {
                                PsiElement value = yamlParameterArray.getValue();
                                if(value != null) {
                                    String valueText = value.getText();
                                    if(StringUtils.isNotBlank(valueText)) {
                                        map.put(keyName.toLowerCase(), PsiElementUtils.trimQuote(valueText));
                                    }
                                }
                            }
                        }
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
        private PsiElement getYamlKeyPath(PsiFile psiFile, String findServiceName, String rootKey) {

            if(!(psiFile.getFirstChild() instanceof YAMLDocument)) {
                return null;
            }

            YAMLDocument yamlDocument = (YAMLDocument) psiFile.getFirstChild();

            // get services or parameter key
            YAMLKeyValue[] yamlKeys = PsiTreeUtil.getChildrenOfType(yamlDocument, YAMLKeyValue.class);
            if(yamlKeys == null) {
                return null;
            }

            for(YAMLKeyValue yamlKeyValue : yamlKeys) {
                String yamlConfigKey = yamlKeyValue.getName();
                if(yamlConfigKey != null && yamlConfigKey.equals(rootKey)) {

                    YAMLKeyValue yamlServices[] = PsiTreeUtil.getChildrenOfType(yamlKeyValue.getValue(),YAMLKeyValue.class);
                    if(yamlServices != null) {
                        for(YAMLKeyValue yamlServiceKeyValue : yamlServices) {
                            String serviceName = yamlServiceKeyValue.getName();
                            if(serviceName != null && serviceName.equals(findServiceName)) {
                                return yamlServiceKeyValue;
                            }

                        }
                    }
                }
            }

            return null;
        }

        @NotNull
        public Map<String, ContainerService> getLocalServiceMap(PsiFile psiFile) {

            Map<String, ContainerService> services = new HashMap<String, ContainerService>();

            YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
            if(yamlDocument == null) {
                return services;
            }

            // get services or parameter key
            YAMLKeyValue[] yamlKeys = PsiTreeUtil.getChildrenOfType(yamlDocument, YAMLKeyValue.class);
            if(yamlKeys == null) {
                return services;
            }

            for(YAMLKeyValue yamlKeyValue : yamlKeys) {
                String yamlConfigKey = yamlKeyValue.getName();
                if(yamlConfigKey != null && yamlConfigKey.equals("services")) {

                    YAMLKeyValue yamlServices[] = PsiTreeUtil.getChildrenOfType(yamlKeyValue.getValue(),YAMLKeyValue.class);
                    if(yamlServices != null) {
                        for(YAMLKeyValue yamlServiceKeyValue : yamlServices) {
                            String serviceName = yamlServiceKeyValue.getName();
                            String serviceClass = null;
                            boolean isPrivate = false;

                            YAMLKeyValue[] yamlServiceKeys = PsiTreeUtil.getChildrenOfType(yamlServiceKeyValue.getValue(),YAMLKeyValue.class);
                            if(yamlServiceKeys != null) {

                                String serviceClassName = this.getKeyValue(yamlServiceKeyValue, "class");
                                if(serviceClassName != null) {
                                    serviceClass = PsiElementUtils.trimQuote(serviceClassName);

                                    // after trim check empty string again
                                    if(StringUtils.isBlank(serviceClass)) {
                                        serviceClass = null;
                                    }

                                }

                                String serviceIsPublic = this.getKeyValue(yamlServiceKeyValue, "public");
                                if(serviceIsPublic != null && serviceIsPublic.equals("false")) {
                                    isPrivate = true;
                                }

                                String serviceAlias = this.getKeyValue(yamlServiceKeyValue, "alias");
                                if(serviceAlias != null && serviceAlias.length() > 0) {
                                    serviceName = serviceAlias;

                                    // if aliased service is in current file use value; not nice here but a simple workaound
                                    if(serviceClass == null && services.containsKey(serviceName)) {
                                        serviceClass = services.get(serviceName).getClassName();
                                    }
                                }

                            }

                            if(StringUtils.isNotBlank(serviceName)) {
                                services.put(serviceName, new ContainerService(serviceName, serviceClass, true, isPrivate));
                            }

                        }
                    }
                }
            }

            return services;

        }

        @Nullable
        private String getKeyValue(YAMLKeyValue yamlServiceKeyValue, String keyName) {

            YAMLKeyValue yamlServiceKeys[] = PsiTreeUtil.getChildrenOfType(yamlServiceKeyValue.getValue(),YAMLKeyValue.class);

            if(yamlServiceKeys == null) {
                return null;
            }

            for(YAMLKeyValue yamlServiceDefKeyValue : yamlServiceKeys) {
                String name = yamlServiceDefKeyValue.getName();
                if(name != null && name.equals(keyName)) {
                    return yamlServiceDefKeyValue.getValue().getText();
                }
            }

            return null;
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

    @Nullable
    public static YAMLKeyValue getRootKey(PsiFile psiFile, String keyName) {

        YAMLDocument yamlDocument = PsiTreeUtil.getChildOfType(psiFile, YAMLDocument.class);
        if(yamlDocument != null) {
            return YamlKeyFinder.find(yamlDocument, keyName);
        }

        return null;
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

    public static String trimSpecialSyntaxServiceName(String serviceName) {

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
    public static YAMLKeyValue getYamlKeyValue(@NotNull YAMLHash yamlHash, String keyName) {
        return getYamlKeyValue(yamlHash, keyName, false);
    }

    @Nullable
    public static String getYamlKeyValueAsString(@NotNull YAMLHash yamlHash, String keyName) {
        YAMLKeyValue yamlKeyValue = getYamlKeyValue(yamlHash, keyName, false);
        if(yamlKeyValue == null) {
            return null;
        }

        String valueText = yamlKeyValue.getValueText();
        if(StringUtils.isBlank(valueText)) {
            return null;
        }

        String s = PsiElementUtils.trimQuote(valueText);
        if(StringUtils.isBlank(s)) {
            return null;
        }

        return s;
    }


    @Nullable
    public static YAMLKeyValue getYamlKeyValue(@Nullable YAMLKeyValue yamlKeyValue, String keyName) {
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
        if(valueText == null) {
            return null;
        }

        return PsiElementUtils.trimQuote(valueText);
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
                    holder.registerProblem(((YAMLKeyValue) psiElement).getKey(), "Duplicate Key", ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
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

        YAMLCompoundValue yamlCompoundValue = PsiTreeUtil.getParentOfType(psiElement, YAMLCompoundValue.class);
        if(yamlCompoundValue == null) {
            return null;
        }

        yamlCompoundValue = PsiTreeUtil.getParentOfType(yamlCompoundValue, YAMLCompoundValue.class);
        if(yamlCompoundValue == null) {
            return null;
        }

        YAMLKeyValue aClass = YamlHelper.getYamlKeyValue(yamlCompoundValue, "class");
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
     */
    @Nullable
    public static List<PsiElement> getYamlArrayOnSequenceOrArrayElements(@NotNull YAMLCompoundValue yamlCompoundValue) {

        PsiElement firstChild = yamlCompoundValue.getFirstChild();

        if(firstChild instanceof YAMLArray) {
            return YamlHelper.getYamlArrayValues((YAMLArray) firstChild);
        } else if(firstChild instanceof YAMLSequence) {
            return new ArrayList<PsiElement>(PsiTreeUtil.getChildrenOfTypeAsList(yamlCompoundValue, YAMLSequence.class));
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
        if(serviceSubKeyCompound instanceof YAMLHash) {
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
                        if("services".equals(((YAMLKeyValue) servicesKey).getName())) {
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
        if(!(tagsCompound instanceof YAMLCompoundValue)) {
            return null;
        }

        Set<String> tags = new HashSet<String>();

        for (YAMLSequence yamlSequence : PsiTreeUtil.getChildrenOfTypeAsList(tagsCompound, YAMLSequence.class)) {

            YAMLHash yamlHash = PsiTreeUtil.getChildOfType(yamlSequence, YAMLHash.class);
            if(yamlHash == null) {
                continue;
            }

            String name = YamlHelper.getYamlKeyValueAsString(yamlHash, "name");
            if(name != null) {
                tags.add(name);
            }

        }

        return tags;
    }

}
