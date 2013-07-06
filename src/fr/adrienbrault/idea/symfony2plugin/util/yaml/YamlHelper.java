package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.psi.YAMLDocument;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlHelper {

    public static String getDomainTrans(PsiElement psiElement) {

        // we only get a PRINT_BLOCK with a huge flat list of psi elements
        // parsing this would be harder than use regex
        // {{ 'a<xxx>'|trans({'%foo%' : bar|default}, 'Domain') }}

        // @TODO: some more conditions needed here
        // search in twig project for regex
        // check for better solution; think of nesting

        String domainName = "messages";

        PsiElement parentPsiElement = psiElement.getParent();
        if(parentPsiElement == null) {
            return domainName;
        }

        String str = parentPsiElement.getText();

        String regex = "\\|\\s?trans\\s?\\(\\{.*?\\},\\s?['\"](\\w+)['\"]\\s?\\)";
        Matcher matcher = Pattern.compile(regex).matcher(str.replace("\r\n", " ").replace("\n", " "));

        if (matcher.find()) {
            return matcher.group(1);
        }

        regex = "\\|\\s?transchoice\\s?\\(\\d+\\s?,\\s?\\{.*?\\},\\s?['\"](\\w+)['\"]\\s?\\)";
        matcher = Pattern.compile(regex).matcher(str.replace("\r\n", " ").replace("\n", " "));

        if (matcher.find()) {
            return matcher.group(1);
        }

        return domainName;
    }

    static public Map<String, String> getLocalServiceMap(PsiElement psiElement) {
        return new YamlLocalServiceMap().getLocalServiceMap(psiElement);
    }

    private static class YamlLocalServiceMap {

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
