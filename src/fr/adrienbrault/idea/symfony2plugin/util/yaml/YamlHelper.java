package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlHelper {

    static public Map<String, String> getLocalServiceMap(PsiElement psiElement) {
        return new YamlLocalServiceMap().getLocalServiceMap(psiElement);
    }

    static public Map<String, String> getLocalParameterMap(PsiElement psiElement) {
        return new YamlLocalServiceMap().getLocalParameterMap(psiElement);
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
