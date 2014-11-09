package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigBlockTag;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigExtendsTag;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.*;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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

        // HomeBundle:default:indexes
        // HomeBundle:default/Test:indexes
        templateFolderName = templateFolderName.replace("\\", "/");
        String shortcutName = symfonyBundle.getName() + ":" + templateFolderName + className.substring(0, className.lastIndexOf("Controller")) + ":" + methodName.substring(0, methodName.lastIndexOf("Action"));

        // we should support types later on
        // HomeBundle:default:indexes.html.twig
        return shortcutName + ".html.twig";
    }

    @NotNull
    public static Map<String, PsiElement> getTemplateAnnotationFiles(PhpDocTag phpDocTag) {

        // @TODO: @Template(template="templatename")
        // Also replace "Matcher" with annotation psi elements; now possible
        // Wait for "annotation plugin" update; to not implement whole stuff here again?

        Map<String, PsiElement> templateFiles = new HashMap<String, PsiElement>();

        // find template name on annotation parameter
        // @Template("templatename")
        PhpPsiElement phpDocAttrList = phpDocTag.getFirstPsiChild();
        if(phpDocAttrList == null) {
            return templateFiles;
        }

        String tagValue = phpDocAttrList.getText();
        Matcher matcher = Pattern.compile("\\(\"(.*)\"").matcher(tagValue);

        if (matcher.find()) {
            // @TODO: only one should possible; refactor getTemplatePsiElements
            PsiElement[] psiElement = TwigHelper.getTemplatePsiElements(phpDocTag.getProject(), matcher.group(1));
            if(psiElement.length > 0) {
                templateFiles.put(matcher.group(1), psiElement[0]);
            }
        }

        return templateFiles;
    }

    public static Map<String, PsiElement> getTemplateAnnotationFilesWithSiblingMethod(PhpDocTag phpDocTag) {
        Map<String, PsiElement> targets = TwigUtil.getTemplateAnnotationFiles(phpDocTag);

        PhpDocComment phpDocComment = PsiTreeUtil.getParentOfType(phpDocTag, PhpDocComment.class);
        if(phpDocComment != null) {
            PsiElement method = phpDocComment.getNextPsiSibling();
            if(method instanceof Method) {
                String templateName = TwigUtil.getControllerMethodShortcut((Method) method);
                if(templateName != null) {
                    for(PsiElement psiElement: TwigHelper.getTemplatePsiElements(method.getProject(), templateName)) {
                        targets.put(templateName, psiElement);
                    }
                }
            }
        }

        return targets;
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
    @NotNull
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

        PsiElement parentPsiElement = psiElement.getParent();
        if(parentPsiElement == null) {
            return null;
        }

        String str = parentPsiElement.getText();

        // @TODO: in another life dont use regular expression to find twig parameter :)

        String regex = "\\|\\s*trans\\s*\\(\\s*\\{.*?\\}\\s*,\\s*['\"]([\\w-]+)['\"]\\s*\\)";
        Matcher matcher = Pattern.compile(regex).matcher(str.replace("\r\n", " ").replace("\n", " "));

        if (matcher.find()) {
            return matcher.group(1);
        }

        regex = "\\|\\s*transchoice\\s*\\(\\s*\\d+\\s*,\\s*\\{.*?\\}\\s*,\\s*['\"]([\\w-]+)['\"]\\s*\\)";
        matcher = Pattern.compile(regex).matcher(str.replace("\r\n", " ").replace("\n", " "));

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
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

        Map<String, VirtualFile> twigFilesByName = TwigHelper.getTwigFilesByName(psiFile.getProject());
        while (matcher.find()) {

            String templateName = matcher.group(1);
            String asName = matcher.group(2);

            if(twigFilesByName.containsKey(templateName)) {
                VirtualFile virtualFile = twigFilesByName.get(templateName);
                PsiFile twigFile = PsiManager.getInstance(psiFile.getProject()).findFile(virtualFile);
                if(twigFile != null) {
                    for (Map.Entry<String, String> entry: new TwigMarcoParser().getMacros(twigFile).entrySet()) {
                        macros.add(new TwigMacro(asName + '.' + entry.getKey(), templateName));
                    }
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

    public static Map<String, VirtualFile> getTemplateName(TwigFile twigFile, Map<String, VirtualFile> templateMap) {

        Map<String, VirtualFile> map = new HashMap<String, VirtualFile>();

        for(Map.Entry<String, VirtualFile> entry: templateMap.entrySet()) {
            if(twigFile.getVirtualFile().equals(entry.getValue())) {
                map.put(entry.getKey(), twigFile.getVirtualFile());
            }
        }

        return map;
    }

    public static Map<String, VirtualFile> getTemplateName(TwigFile twigFile) {
        return getTemplateName(twigFile, TwigHelper.getTemplateFilesByName(twigFile.getProject(), true, false));
    }

    public static HashMap<String, PsiVariable> collectControllerTemplateVariables(PsiElement psiElement) {

        HashMap<String, PsiVariable> vars = new HashMap<String, PsiVariable>();

        PsiFile psiFile = psiElement.getContainingFile();
        if(!(psiFile instanceof TwigFile)) {
            return vars;
        }

        Method method = findTwigFileController((TwigFile) psiFile);
        if(method != null) {
            return PhpMethodVariableResolveUtil.collectMethodVariables(method);
        }

        final Set<Method> methods = getTwigFileMethodUsageOnIndex((TwigFile) psiFile);

        HashMap<String, PsiVariable> stringPsiVariableHashMap = new HashMap<String, PsiVariable>();
        for(Method methodIndex : methods) {
            stringPsiVariableHashMap.putAll(PhpMethodVariableResolveUtil.collectMethodVariables(methodIndex));
        }

        return stringPsiVariableHashMap;

    }

    @NotNull
    public static Set<Method> getTwigFileMethodUsageOnIndex(@NotNull TwigFile psiFile) {

        Map<String, VirtualFile> templateName = TwigUtil.getTemplateName(psiFile);
        if(templateName.size() == 0) {
            return Collections.emptySet();
        }

        final Set<String> keys = templateName.keySet();
        final Set<VirtualFile> virtualFiles = new HashSet<VirtualFile>();

        // find virtual files
        for(String key: keys) {
            FileBasedIndexImpl.getInstance().getFilesWithKey(PhpTwigTemplateUsageStubIndex.KEY, new HashSet<String>(Arrays.asList(key)), new Processor<VirtualFile>() {
                @Override
                public boolean process(VirtualFile virtualFile) {
                    virtualFiles.add(virtualFile);
                    return true;
                }
            }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(psiFile.getProject()), PhpFileType.INSTANCE));
        }

        final Set<Method> methods = new HashSet<Method>();

        for(VirtualFile virtualFile : virtualFiles) {

            PsiFile psiTemplate = PsiManager.getInstance(psiFile.getProject()).findFile(virtualFile);
            if(psiTemplate == null) {
                continue;
            }

            psiTemplate.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if(element instanceof StringLiteralExpression && element.getParent() instanceof ParameterList && element.getParent().getParent() instanceof MethodReference && keys.contains(((StringLiteralExpression) element).getContents())) {

                        PsiElement methodReference = element.getParent().getParent();
                        if(methodReference instanceof MethodReference && PhpTwigTemplateUsageStubIndex.RENDER_METHODS.contains(((MethodReference) methodReference).getName())) {
                            Method method = PsiTreeUtil.getParentOfType(element, Method.class);
                            if(method != null) {
                                methods.add(method);
                            }
                        }
                    }

                    super.visitElement(element);
                }
            });

        }

        return methods;
    }

    @Nullable
    public static String getFoldingTemplateNameOrCurrent(@Nullable String templateName) {
        String foldingName = getFoldingTemplateName(templateName);
        return foldingName != null ? foldingName : templateName;
    }

    @Nullable
    public static String getFoldingTemplateName(@Nullable String content) {
        if(content == null || content.length() == 0) return null;

        String templateShortcutName = null;
        if(content.endsWith(".html.twig") && content.length() > 10) {
            templateShortcutName = content.substring(0, content.length() - 10);
        } else if(content.endsWith(".html.php") && content.length() > 9) {
            templateShortcutName = content.substring(0, content.length() - 9);
        }

        if(templateShortcutName == null || templateShortcutName.length() == 0) {
            return null;
        }

        // template FooBundle:Test:edit.html.twig
        if(templateShortcutName.length() <= "Bundle:".length()) {
            return templateShortcutName;
        }

        int split = templateShortcutName.indexOf("Bundle:");
        if(split > 0) {
            templateShortcutName = templateShortcutName.substring(0, split) + templateShortcutName.substring("Bundle".length() + split);
        }

        return templateShortcutName;
    }

    public static String getPresentableTemplateName(Map<String, VirtualFile> files, PsiElement psiElement) {
        return getPresentableTemplateName(files, psiElement, false);
    }

    public static String getPresentableTemplateName(Map<String, VirtualFile> files, PsiElement psiElement, boolean shortMode) {

        VirtualFile currentFile = psiElement.getContainingFile().getVirtualFile();

        List<String> templateNames = new ArrayList<String>();
        for(Map.Entry<String, VirtualFile> entry: files.entrySet()) {
            if(entry.getValue().equals(currentFile)) {
                templateNames.add(entry.getKey());
            }
        }

        if(templateNames.size() > 0) {

            // bundle names wins
            if(templateNames.size() > 1) {
                Collections.sort(templateNames, new TwigUtil.TemplateStringComparator());
            }

            String templateName = templateNames.iterator().next();
            if(shortMode) {
                String shortName = getFoldingTemplateName(templateName);
                if(shortName != null) {
                    return shortName;
                }
            }

            return templateName;
        }

        String relativePath = VfsUtil.getRelativePath(currentFile, psiElement.getProject().getBaseDir(), '/');
        return relativePath != null ? relativePath : currentFile.getPath();

    }

    private static class TemplateStringComparator implements Comparator<String> {
        @Override
        public int compare(String o1, String o2) {

            if(o1.startsWith("@") && o2.startsWith("@")) {
                return 0;
            }

            if(!o1.startsWith("@") && o2.startsWith("@")) {
                return -1;
            }

            return 1;
        }
    }

    /**
     * Collections "extends" and "blocks" an path and and sort them on appearance
     */
    public static TwigCreateContainer getOnCreateTemplateElements(@NotNull final Project project, @NotNull VirtualFile startDirectory) {

        final TwigCreateContainer containerElement = new TwigCreateContainer();

        VfsUtil.processFilesRecursively(startDirectory, new Processor<VirtualFile>() {
            @Override
            public boolean process(VirtualFile virtualFile) {

                if(virtualFile.getFileType() != TwigFileType.INSTANCE) {
                    return true;
                }

                PsiFile twigFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(twigFile instanceof TwigFile) {
                    collect((TwigFile) twigFile);
                }

                return true;
            }

            private void collect(TwigFile twigFile) {
                for(PsiElement psiElement: twigFile.getChildren()) {
                    if(psiElement instanceof TwigExtendsTag) {
                        Matcher matcher = Pattern.compile(TwigBlockParser.EXTENDS_TEMPLATE_NAME_PATTERN).matcher(psiElement.getText());
                        if(matcher.find()){
                            String group = matcher.group(1);
                            if(StringUtils.isNotBlank(group)) {
                                containerElement.addExtend(group);
                            }
                        }
                    } else if(psiElement.getNode().getElementType() == TwigElementTypes.BLOCK_STATEMENT) {
                        PsiElement blockTag = psiElement.getFirstChild();
                        if(blockTag instanceof TwigBlockTag) {
                            String name = ((TwigBlockTag) blockTag).getName();
                            if(StringUtils.isNotBlank(name)) {
                                containerElement.addBlock(name);
                            }
                        }

                    }

                }
            }

        });

        return containerElement;

    }

    /**
     *  Build twig template file content on path with help of TwigCreateContainer
     */
    @Nullable
    public static String buildStringFromTwigCreateContainer(@NotNull Project project, @Nullable VirtualFile virtualTargetDir) {

        if(virtualTargetDir == null) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder();
        TwigCreateContainer container = TwigUtil.getOnCreateTemplateElements(project, virtualTargetDir);
        String extend = container.getExtend();
        if(extend != null) {
            stringBuilder.append("{% extends '").append(extend).append("' %}").append("\n\n");
        }

        for(String blockName: container.getBlockNames(2)) {
            stringBuilder.append("{% block ").append(blockName).append(" %}\n\n").append("{% endblock %}").append("\n\n");
        }

        String s = stringBuilder.toString();
        return StringUtils.isNotBlank(s) ? s : null;

    }

}
