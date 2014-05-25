package fr.adrienbrault.idea.symfony2plugin.util.yaml;

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
import org.jetbrains.yaml.psi.YAMLArray;
import org.jetbrains.yaml.psi.YAMLCompoundValue;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLPsiElementImpl;

import java.util.*;

public class YamlHelper {

    static public Map<String, ContainerService> getLocalServiceMap(PsiElement psiElement) {
        return new YamlLocalServiceMap().getLocalServiceMap(psiElement);
    }

    @Nullable
    static public PsiElement getLocalServiceName(PsiFile psiFile, String findServiceName) {
        return new YamlLocalServiceMap().getLocalServiceName(psiFile, findServiceName);
    }

    static public Map<String, String> getLocalParameterMap(PsiElement psiElement) {
        return new YamlLocalServiceMap().getLocalParameterMap(psiElement);
    }

    /**
     * getChildren eg on YamlArray is empty, provide workaround
     */
    static public PsiElement[] getChildrenFix(PsiElement psiElement) {
        ArrayList<PsiElement> psiElements = new ArrayList<PsiElement>();

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
     *  Trx to find psi value which match shoukd be a array value and filter out comma, whitespace...
     *  [@service, "@lunamas.app_manager2", [""]];
     */
    static public ArrayList<PsiElement> getYamlArrayValues(YAMLArray yamlArray) {


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
        ArrayList<PsiElement> keys = new ArrayList<PsiElement>();
        for(Map.Entry<Integer, ArrayList<PsiElement>> psiEntry: argumentSplitter.entrySet()) {
            PsiElement parameterPsiElement = null;
            for(PsiElement psiElement: psiEntry.getValue()) {
                if(PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).accepts(psiElement) || PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).accepts(psiElement)) {
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

        public Map<String, String> getLocalParameterMap(PsiElement psiElement) {

            Map<String, String> map = new HashMap<String, String>();

            if(!(psiElement.getContainingFile().getFirstChild() instanceof YAMLDocument)) {
                return map;
            }

            YAMLDocument yamlDocument = (YAMLDocument) psiElement.getContainingFile().getFirstChild();

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
                if(yamlConfigKey != null && yamlConfigKey.equals("services")) {

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
        public Map<String, ContainerService> getLocalServiceMap(PsiElement psiElement) {

            Map<String, ContainerService> services = new HashMap<String, ContainerService>();

            if(!(psiElement.getContainingFile().getFirstChild() instanceof YAMLDocument)) {
                return services;
            }

            YAMLDocument yamlDocument = (YAMLDocument) psiElement.getContainingFile().getFirstChild();

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

        if(psiFile.getFirstChild() instanceof YAMLDocument) {
            return YamlKeyFinder.find(psiFile.getFirstChild(), keyName);
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
    public static YAMLKeyValue getYamlKeyValue(@Nullable YAMLKeyValue yamlKeyValue, String keyName) {
        return getYamlKeyValue(yamlKeyValue, keyName, false);
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

}
