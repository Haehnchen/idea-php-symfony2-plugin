package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMacro;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMarcoParser;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigSet;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwigUtil {

    @Nullable
    public static String getControllerMethodShortcut(Method method) {

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

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(PhpIndex.getInstance(method.getProject()));
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

    public static PsiElement[] getTemplateAnnotationFiles(PhpDocTag phpDocTag) {

        // find template name on annotation parameter
        // @Template("templatename")
        PhpPsiElement phpDocAttrList = phpDocTag.getFirstPsiChild();
        if(phpDocAttrList == null) {
            return null;
        }

        String tagValue = phpDocAttrList.getText();
        Matcher matcher = Pattern.compile("\\(\"(.*)\"").matcher(tagValue);

        if (matcher.find()) {
            return TwigHelper.getTemplatePsiElements(phpDocTag.getProject(), matcher.group(1));
        }

        return new PsiElement[0];
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

    public static ArrayList<TwigMacro> getImportedMacros(PsiFile psiFile) {

        ArrayList<TwigMacro> macros = new ArrayList<TwigMacro>();

        PsiElement[] importPsiElements = PsiTreeUtil.collectElements(psiFile, new PsiElementFilter() {
            @Override
            public boolean isAccepted(PsiElement paramPsiElement) {
                return PlatformPatterns.psiElement(TwigElementTypes.IMPORT_TAG).accepts(paramPsiElement);
            }
        });

        for(PsiElement psiImportTag: importPsiElements) {
            String regex = "\\{%\\s?from\\s?['\"](.*?)['\"]\\s?import\\s?(.*?)\\s?%}";
            Matcher matcher = Pattern.compile(regex).matcher(psiImportTag.getText().replace("\n", " "));

            while (matcher.find()) {

                String templateName = matcher.group(1);
                for(String macroName : matcher.group(2).split(",")) {

                    // not nice here search for as "macro as macro_alias"
                    Matcher asMatcher = Pattern.compile("(\\w+)\\s+as\\s+(\\w+)").matcher(macroName.trim());
                    if(asMatcher.find()) {
                        macros.add(new TwigMacro(asMatcher.group(2), templateName, asMatcher.group(1)));
                    } else {
                        macros.add(new TwigMacro(macroName.trim(), templateName));
                    }

                }
            }

        }

        return macros;

    }

    public static ArrayList<TwigMacro> getImportedMacrosNamespaces(PsiFile psiFile) {

        ArrayList<TwigMacro> macros = new ArrayList<TwigMacro>();

        String str = psiFile.getText();

        // {% import '@foo/bar.html.twig' as macro1 %}
        String regex = "\\{%\\s?import\\s?['\"](.*?)['\"]\\s?as\\s?(.*?)\\s?%}";
        Matcher matcher = Pattern.compile(regex).matcher(str.replace("\n", " "));

        Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(psiFile.getProject());
        while (matcher.find()) {

            String templateName = matcher.group(1);
            String asName = matcher.group(2);

            if(twigFilesByName.containsKey(templateName)) {
                for (Map.Entry<String, String> entry: new TwigMarcoParser().getMacros(twigFilesByName.get(templateName)).entrySet()) {
                    macros.add(new TwigMacro(asName + '.' + entry.getKey(), templateName));
                }
            }

        }

        return macros;

    }

    public static ArrayList<TwigSet> getSetDeclaration(PsiFile psiFile) {

        ArrayList<TwigSet> sets = new ArrayList<TwigSet>();
        String str = psiFile.getText();

        // {% set foo = 'foo' %}
        // {% set foo %}
        String regex = "\\{%\\s?set\\s?(.*?)\\s.*?%}";
        Matcher matcher = Pattern.compile(regex).matcher(str.replace("\n", " "));

        while (matcher.find()) {
            sets.add(new TwigSet(matcher.group(1)));
        }

        return sets;

    }

    @Nullable
    public static Method findTwigFileController(TwigFile twigFile) {

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(twigFile.getProject()).getContainingBundle(twigFile);
        if(symfonyBundle == null) {
            return null;
        }

        String relativePath = symfonyBundle.getRelativePath(twigFile.getVirtualFile());
        if(relativePath == null || !relativePath.startsWith("Resources/views/")) {
            return null;
        }

        String viewPath = relativePath.substring("Resources/views/".length());

        Matcher simpleFilter = Pattern.compile(".*/(\\w+)\\.\\w+\\.twig").matcher(viewPath);
        if(!simpleFilter.find()) {
            return null;
        }

        String methodName = simpleFilter.group(1) + "Action";
        String className = symfonyBundle.getNamespaceName() + "Controller\\" + viewPath.substring(0, viewPath.lastIndexOf("/")).replace("/", "\\") + "Controller";

        return PhpElementsUtil.getClassMethod(twigFile.getProject(), className, methodName);

    }

    public static HashMap<String, Set<String>> collectControllerTemplateVariables(PsiElement psiElement) {

        HashMap<String, Set<String>> vars = new HashMap<String, Set<String>>();

        PsiFile psiFile = psiElement.getContainingFile();
        if(!(psiFile instanceof TwigFile)) {
            return vars;
        }

        Method method = findTwigFileController((TwigFile) psiFile);
        if(method == null) {
            return vars;
        }

        return PhpMethodVariableResolveUtil.collectMethodVariables(method);

    }

}
