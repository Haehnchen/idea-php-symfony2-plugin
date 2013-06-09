package fr.adrienbrault.idea.symfony2plugin.util.yaml;

import com.intellij.psi.PsiElement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlHelper {

    public static String getDomainTrans(PsiElement psiElement) {

        String domainName = "messages";

        PsiElement parentPsiElement = psiElement.getParent();
        if(parentPsiElement == null) {
            return domainName;
        }

        String str = parentPsiElement.getText();

        String regex = "\\|\\s?trans\\s?\\(\\{.*?\\},\\s?['\"](\\w+)['\"]\\s?\\)";
        Matcher matcher = Pattern.compile(regex).matcher(str.replace("\r\n", " ").replace("\n", " "));

        while (matcher.find()) {
            domainName = matcher.group(1);
        }

        return domainName;
    }

}
