package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.psi.YAMLArray;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;
import org.jetbrains.yaml.psi.impl.YAMLPsiElementImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class YamlHelper {

    static public Map<String, String> getLocalServiceMap(PsiElement psiElement) {
        return new YamlLocalServiceMap().getLocalServiceMap(psiElement);
    }

    static public Map<String, String> getLocalParameterMap(PsiElement psiElement) {
        return new YamlLocalServiceMap().getLocalParameterMap(psiElement);
    }

    /**
     * getChildren eg on YamlArray is empty, provide workaround
     */
    static public PsiElement[] getChildrenFix(PsiElement psiElement) {
        ArrayList<PsiElement> psiElements = new ArrayList<PsiElement>();

        psiElements.add(psiElement.getFirstChild());

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

            keys.add(parameterPsiElement);

        }

        return keys;
    }

    @Nullable
    public static YAMLKeyValue getYamlKeyValue(@Nullable PsiElement yamlCompoundValue, String keyName) {

        if(yamlCompoundValue == null) {
            return null;
        }

        YAMLKeyValue classKeyValue = PsiElementUtils.getChildrenOfType(yamlCompoundValue, PlatformPatterns.psiElement(YAMLKeyValue.class).withName(keyName));

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
                            map.put(yamlParameterArray.getKeyText(), yamlParameterArray.getValue().getText());
                        }
                    }

                }
            }

            return map;

        }

        public Map<String, String> getLocalServiceMap(PsiElement psiElement) {

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
                if(yamlConfigKey != null && yamlConfigKey.equals("services")) {

                    YAMLKeyValue yamlServices[] = PsiTreeUtil.getChildrenOfType(yamlKeyValue.getValue(),YAMLKeyValue.class);
                    if(yamlServices != null) {
                        for(YAMLKeyValue yamlServiceKeyValue : yamlServices) {
                            String serviceName = yamlServiceKeyValue.getName();
                            String serviceClass = "";

                            YAMLKeyValue[] yamlServiceKeys = PsiTreeUtil.getChildrenOfType(yamlServiceKeyValue.getValue(),YAMLKeyValue.class);
                            if(yamlServiceKeys != null) {
                                String serviceClassName = this.getClassValue(yamlServiceKeyValue);
                                if(serviceClassName != null) {
                                    serviceClass = serviceClassName;
                                }
                            }

                            map.put(serviceName, serviceClass);
                        }
                    }
                }
            }

            return map;

        }

        @Nullable
        private String getClassValue(YAMLKeyValue yamlServiceKeyValue) {

            YAMLKeyValue yamlServiceKeys[] = PsiTreeUtil.getChildrenOfType(yamlServiceKeyValue.getValue(),YAMLKeyValue.class);

            if(yamlServiceKeys == null) {
                return null;
            }

            for(YAMLKeyValue yamlServiceDefKeyValue : yamlServiceKeys) {
                String name = yamlServiceDefKeyValue.getName();
                if(name != null && name.equals("class")) {
                    return yamlServiceDefKeyValue.getValue().getText();
                }
            }

            return null;
        }
    }

}
