package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMarcoParser;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigUtil {

    public static String getControllerMethodShortcut(Method method) {

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(PhpIndex.getInstance(method.getProject()));

        // indexAction
        String methodName = method.getName();
        if(!methodName.endsWith("Action")) {
            return null;
        }

        PhpClass phpClass = method.getContainingClass();
        if(null == phpClass) {
            return null;
        }

        // defaultController
        // default/Folder/FolderController
        String className = phpClass.getName();
        if(!className.endsWith("Controller")) {
            return null;
        }

        SymfonyBundle symfonyBundle = symfonyBundleUtil.getContainingBundle(phpClass);
        if(symfonyBundle == null) {
            return null;
        }

        // find the bundle name of file
        PhpClass BundleClass = symfonyBundle.getPhpClass();
        if(null == BundleClass) {
            return null;
        }

        // check if files is in <Bundle>/Controller/*
        if(!phpClass.getNamespaceName().startsWith(BundleClass.getNamespaceName() + "Controller\\")) {
            return null;
        }

        // strip the controller folder name
        String templateFolderName = phpClass.getNamespaceName().substring(BundleClass.getNamespaceName().length() + 11);

        // HomeBundle:default:index
        // HomeBundle:default/Test:index
        templateFolderName = templateFolderName.replace("\\", "/");
        String shortcutName = symfonyBundle.getName() + ":" + templateFolderName + className.substring(0, className.lastIndexOf("Controller")) + ":" + methodName.substring(0, methodName.lastIndexOf("Action"));

        // we should support types later on
        // HomeBundle:default:index.html.twig
        return shortcutName + ".html.twig";
    }


    @Nullable
    public static String getTwigFileTransDefaultDomain(PsiFile psiFile) {

        String str = psiFile.getText();

        // {% trans_default_domain "app" %}
        String regex = "\\{%\\s?trans_default_domain\\s?['\"](\\w+)['\"]\\s?%}";
        Matcher matcher = Pattern.compile(regex).matcher(str.replace("\r\n", " ").replace("\n", " "));

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    /**
     * need a twig translation print block and search for default domain on parameter or trans_default_domain
     *
     * @param psiElement some print block like that 'a'|trans
     * @return matched domain or "messages" fallback
     */
    public static String getPsiElementTranslationDomain(PsiElement psiElement) {
        String domain = getDomainTrans(psiElement);
        if(domain == null) {
            domain = getTwigFileTransDefaultDomain(psiElement.getContainingFile());
        }

        return domain == null ? "messages" : domain;
    }

    @Nullable
    public static String getDomainTrans(PsiElement psiElement) {

        // we only get a PRINT_BLOCK with a huge flat list of psi elements
        // parsing this would be harder than use regex
        // {{ 'a<xxx>'|trans({'%foo%' : bar|default}, 'Domain') }}

        // @TODO: some more conditions needed here
        // search in twig project for regex
        // check for better solution; think of nesting

        String domainName = null;

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

    public static HashMap<String, String> getImportedMacros(PsiFile psiFile) {

        HashMap<String, String> macros = new HashMap<String, String>();

        String str = psiFile.getText();

        // {% from '@foo/bar.html.twig' import macro1, macro_foo_bar %}
        String regex = "\\{%\\s?from\\s?['\"](.*?)['\"]\\s?import\\s?(.*?)\\s?%}";
        Matcher matcher = Pattern.compile(regex).matcher(str.replace("\r\n", " ").replace("\n", " "));

        while (matcher.find()) {

            String templateName = matcher.group(1);
            for(String macroName : matcher.group(2).split(",")) {
                macros.put(macroName.trim(), templateName);
            }
        }

        return macros;

    }

    public static HashMap<String, String> getImportedMacrosNamespaces(PsiFile psiFile) {

        HashMap<String, String> macros = new HashMap<String, String>();

        String str = psiFile.getText();

        // {% import '@foo/bar.html.twig' as macro1 %}
        String regex = "\\{%\\s?import\\s?['\"](.*?)['\"]\\s?as\\s?(.*?)\\s?%}";
        Matcher matcher = Pattern.compile(regex).matcher(str.replace("\r\n", " ").replace("\n", " "));

        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(psiFile.getProject());
        while (matcher.find()) {

            String templateName = matcher.group(1);
            String asName = matcher.group(2);

            if(twigFilesByName.containsKey(templateName)) {
                for (Map.Entry<String, String> entry: new TwigMarcoParser().getMacros(twigFilesByName.get(templateName)).entrySet()) {
                    macros.put(asName + '.' + entry.getKey(), templateName);
                }
            }

        }

        return macros;

    }

}
