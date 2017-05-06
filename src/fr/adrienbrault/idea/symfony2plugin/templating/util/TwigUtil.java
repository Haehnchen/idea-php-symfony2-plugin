package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpPsiElement;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.*;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateUsage;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.*;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPathIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigUtil {

    @Nullable
    public static String[] getControllerMethodShortcut(Method method) {

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

        // @TODO: we should support types later on; but nicer
        // HomeBundle:default:indexes.html.twig
        return new String[] {
            shortcutName + ".html.twig",
            shortcutName + ".json.twig",
            shortcutName + ".xml.twig",
        };
    }

    @NotNull
    public static Map<String, PsiElement> getTemplateAnnotationFiles(PhpDocTag phpDocTag) {

        // @TODO: @Template(template="templatename")
        // Also replace "Matcher" with annotation psi elements; now possible
        // Wait for "annotation plugin" update; to not implement whole stuff here again?

        Map<String, PsiElement> templateFiles = new HashMap<>();

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
                String[] templateNames = TwigUtil.getControllerMethodShortcut((Method) method);
                if(templateNames != null) {
                    for (String name : templateNames) {
                        for(PsiElement psiElement: TwigHelper.getTemplatePsiElements(method.getProject(), name)) {
                            targets.put(name, psiElement);
                        }
                    }
                }
            }
        }

        return targets;
    }

    /**
     *  Finds a trans_default_domain definition in twig file
     *
     * "{% trans_default_domain "validators" %}"
     *
     * @param position current scope to search for: Twig file or embed scope
     * @return file translation domain
     */
    @Nullable
    public static String getTransDefaultDomainOnScope(@NotNull PsiElement position) {
        PsiElement scope = getTransDefaultDomainScope(position);
        if(scope == null) {
            return null;
        }

        for (PsiElement psiElement : scope.getChildren()) {

            // filter parent trans_default_domain, it should be in file context
            if(psiElement instanceof TwigCompositeElement && psiElement.getNode().getElementType() == TwigElementTypes.TAG) {

                final String[] fileTransDomain = {null};
                psiElement.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if(TwigHelper.getTransDefaultDomainPattern().accepts(element)) {
                            String text = PsiElementUtils.trimQuote(element.getText());
                            if(StringUtils.isNotBlank(text)) {
                                fileTransDomain[0] = text;
                            }
                        }
                        super.visitElement(element);
                    }
                });

                if(fileTransDomain[0] != null) {
                    return fileTransDomain[0];
                }

            }
        }

        return null;
    }

    /**
     * Search Twig element to find use trans_default_domain and returns given string parameter
     */
    @Nullable
    public static String getTransDefaultDomainOnScopeOrInjectedElement(@NotNull PsiElement position, int caretOffset) {
        if(position.getContainingFile().getContainingFile() == TwigFileType.INSTANCE) {
            return getTransDefaultDomainOnScope(position);
        }

        PsiElement element = getInjectedTwigElement(position.getContainingFile(), caretOffset);
        if(element != null) {
            return getTransDefaultDomainOnScope(element);
        }

        return null;
    }

    /**
     * Html in Twig is injected trx to find an real Twig element
     * TODO: there must be some nicer solution
     *
     * {% block %}<html/>{% endblock %}
     */
    @Nullable
    public static PsiElement getInjectedTwigElement(@NotNull PsiFile psiFile, int caretOffset) {
        PsiElement elementAt;

        int limit = 20;
        do {
            caretOffset = caretOffset - 5;
            elementAt = psiFile.findElementAt(caretOffset);
        } while (limit-- > 0 && caretOffset > 0 && elementAt != null && elementAt.getContainingFile().getFileType() != TwigFileType.INSTANCE);

        return elementAt;
    }

    /**
     * File Scope:
     * {% trans_default_domain "foo" %}
     *
     * Embed:
     * {embed 'foo.html.twig'}{% trans_default_domain "foo" %}{% endembed %}
     */
    @Nullable
    public static PsiElement getTransDefaultDomainScope(@NotNull PsiElement psiElement) {
        return PsiTreeUtil.findFirstParent(psiElement, psiElement1 ->
            psiElement1 instanceof PsiFile ||
            (psiElement1 instanceof TwigCompositeElement && psiElement1.getNode().getElementType() == TwigElementTypes.EMBED_STATEMENT)
        );
    }

    /**
     * need a twig translation print block and search for default domain on parameter or trans_default_domain
     *
     * @param psiElement some print block like that 'a'|trans
     * @return matched domain or "messages" fallback
     */
    @NotNull
    public static String getPsiElementTranslationDomain(@NotNull PsiElement psiElement) {
        String domain = getDomainTrans(psiElement);
        if(domain == null) {
            domain = getTransDefaultDomainOnScope(psiElement);
        }

        return domain == null ? "messages" : domain;
    }

    /**
     * Extract translation domain parameter
     * trans({}, 'Domain')
     * transchoice(2, {}, 'Domain')
     */
    @Nullable
    public static String getDomainTrans(@NotNull PsiElement psiElement) {
        PsiElement filter = PsiElementUtils.getNextSiblingAndSkip(psiElement, TwigTokenTypes.FILTER, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE);
        if(!PsiElementAssertUtil.isNotNullAndIsElementType(filter, TwigTokenTypes.FILTER)) {
            return null;
        }

        PsiElement filterName = PsiTreeUtil.nextVisibleLeaf(filter);
        if(!PsiElementAssertUtil.isNotNullAndIsElementType(filterName, TwigTokenTypes.IDENTIFIER)) {
            return null;
        }

        // Elements that match a simple parameter foo(<caret>,)
        IElementType[] skipArrayElements = {
            TwigElementTypes.LITERAL, TwigTokenTypes.LBRACE_SQ, TwigTokenTypes.RBRACE_SQ, TwigTokenTypes.IDENTIFIER
        };

        String filterNameText = filterName.getText();
        if("trans".equalsIgnoreCase(filterNameText)) {
            PsiElement brace = PsiTreeUtil.nextVisibleLeaf(filterName);
            if (PsiElementAssertUtil.isNotNullAndIsElementType(brace, TwigTokenTypes.LBRACE)) {
                PsiElement comma = PsiElementUtils.getNextSiblingAndSkip(brace, TwigTokenTypes.COMMA, skipArrayElements);
                if(comma != null) {
                    String text = extractDomainFromParameter(comma);
                    if (text != null) {
                        return text;
                    }
                }
            }
        } else if ("transchoice".equalsIgnoreCase(filterNameText)) {
            PsiElement brace = PsiTreeUtil.nextVisibleLeaf(filterName);
            if (PsiElementAssertUtil.isNotNullAndIsElementType(brace, TwigTokenTypes.LBRACE)) {
                // skip elements which are possible a parameter variable and are between commas
                IElementType[] skipElements = {
                    TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE, TwigTokenTypes.NUMBER,
                    TwigTokenTypes.STRING_TEXT, TwigTokenTypes.DOT, TwigTokenTypes.IDENTIFIER,
                    TwigTokenTypes.CONCAT, TwigTokenTypes.PLUS, TwigTokenTypes.MINUS,
                };

                PsiElement comma1 = PsiElementUtils.getNextSiblingAndSkip(brace, TwigTokenTypes.COMMA, skipElements);
                if(comma1 != null) {
                    PsiElement comma2 = PsiElementUtils.getNextSiblingAndSkip(comma1, TwigTokenTypes.COMMA, skipArrayElements);
                    if(comma2 != null) {
                        String text = extractDomainFromParameter(comma2);
                        if (text != null) {
                            return text;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * ({}, "foobar", )
     */
    @Nullable
    private static String extractDomainFromParameter(@NotNull PsiElement comma) {
        if (PsiElementAssertUtil.isNotNullAndIsElementType(comma, TwigTokenTypes.COMMA)) {
            PsiElement quote = PsiTreeUtil.nextVisibleLeaf(comma);
            if (PsiElementAssertUtil.isNotNullAndIsElementType(quote, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE)) {
                PsiElement text = PsiTreeUtil.nextVisibleLeaf(quote);
                if (text != null && TwigHelper.getParameterAsStringPattern().accepts(text)) {
                    return text.getText();
                }
            }
        }

        return null;
    }

    /**
     * {% import _self as %}
     * {% import 'foobar.html.twig' as %}
     *
     * {% from _self import %}
     * {% from 'forms.html' import %}
     */
    private static Pair<String, PsiElement> getTemplateNameOnStringAndSelfWithNextPsiElement(@NotNull PsiElement tagPsiElement, @NotNull IElementType elementType) {
        PsiElement lastElementMatch = null;
        String template = null;

        PsiElement selfPsi = PsiElementUtils.getNextSiblingAndSkip(tagPsiElement, TwigTokenTypes.RESERVED_ID);
        if(selfPsi != null && "_self".equals(selfPsi.getText())) {
            // {% import _self as foobar %}
            template = "_self";
            lastElementMatch = PsiElementUtils.getNextSiblingAndSkip(selfPsi, elementType);
        } else {
            // {% import 'foobar.html.twig' as foobar %}
            PsiElement templateString = PsiElementUtils.getNextSiblingAndSkip(tagPsiElement, TwigTokenTypes.STRING_TEXT, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE);
            if(templateString != null) {
                String templateName = templateString.getText();
                if(StringUtils.isNotBlank(templateName)) {
                    template = templateName;
                    lastElementMatch = PsiElementUtils.getNextSiblingAndSkip(templateString, elementType, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE);
                }
            }
        }

        if(lastElementMatch == null | lastElementMatch == null) {
            return null;
        }

        return Pair.create(template, lastElementMatch);
    }

    /**
     * {% from _self import foobar as input, foobar %}
     * {% from 'foobar.html.twig' import foobar_twig %}
     */
    @NotNull
    public static Collection<TwigMacro> getImportedMacros(@NotNull PsiFile psiFile) {
        PsiElement[] importPsiElements = PsiTreeUtil.collectElements(psiFile, paramPsiElement ->
            PlatformPatterns.psiElement(TwigElementTypes.IMPORT_TAG).accepts(paramPsiElement)
        );

        if(importPsiElements.length == 0) {
            return Collections.emptyList();
        }

        Collection<TwigMacro> macros = new ArrayList<>();

        for(PsiElement psiImportTag: importPsiElements) {
            PsiElement firstChild = psiImportTag.getFirstChild();
            if(firstChild == null) {
                continue;
            }

            PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(firstChild, TwigTokenTypes.TAG_NAME);
            if(tagName == null || !"from".equals(tagName.getText())) {
                continue;
            }

            Pair<String, PsiElement> pair = getTemplateNameOnStringAndSelfWithNextPsiElement(tagName, TwigTokenTypes.IMPORT_KEYWORD);
            if(pair == null) {
                continue;
            }

            String templateName = pair.getFirst();

            // find end block to extract variables
            PsiElement endBlock = PsiElementUtils.getNextSiblingOfType(
                pair.getSecond(),
                PlatformPatterns.psiElement().withElementType(TwigTokenTypes.STATEMENT_BLOCK_END)
            );

            if(endBlock == null) {
                continue;
            }

            String substring = psiFile.getText().substring(pair.getSecond().getTextRange().getEndOffset(), endBlock.getTextOffset()).trim();

            for(String macroName : substring.split(",")) {
                // not nice here search for as "macro as macro_alias"
                Matcher asMatcher = Pattern.compile("(\\w+)\\s+as\\s+(\\w+)").matcher(macroName.trim());
                if(asMatcher.find()) {
                    macros.add(new TwigMacro(asMatcher.group(2), templateName, asMatcher.group(1)));
                } else {
                    macros.add(new TwigMacro(macroName.trim(), templateName));
                }
            }
        }

        return macros;
    }

    /**
     * Get targets for macro imports
     *
     * {% from _self import foobar as input, foobar %}
     * {% from 'foobar.html.twig' import foobar_twig %}
     */
    @NotNull
    public static Collection<PsiElement> getImportedMacros(@NotNull PsiFile psiFile, @NotNull String funcName) {
        Collection<PsiElement> psiElements = new ArrayList<>();

        for (TwigMacro twigMacro : TwigUtil.getImportedMacros(psiFile)) {
            if (!twigMacro.getName().equals(funcName)) {
                continue;
            }

            // switch to alias mode
            String macroName = twigMacro.getOriginalName() == null ? funcName : twigMacro.getOriginalName();

            PsiFile[] foreignPsiFile;
            if ("_self".equals(twigMacro.getTemplate())) {
                foreignPsiFile = new PsiFile[] {psiFile};
            } else {
                foreignPsiFile = TwigHelper.getTemplatePsiElements(psiFile.getProject(), twigMacro.getTemplate());
            }

            for (PsiFile file : foreignPsiFile) {
                visitMacros(file, pair -> {
                    if(macroName.equals(pair.getFirst().getName())) {
                        psiElements.add(pair.getSecond());
                    }
                });
            }
        }

        return psiElements;
    }

    /**
     * {% import _self as foobar %}
     * {% import 'foobar.html.twig' as foobar %}
     */
    public static Collection<TwigMacro> getImportedMacrosNamespaces(@NotNull PsiFile psiFile) {
        Collection<TwigMacro> macros = new ArrayList<>();

        visitImportedMacrosNamespaces(psiFile, pair -> macros.add(pair.getFirst()));

        return macros;
    }

    /**
     * Find targets for given macros, alias supported
     *
     * {% import _self as foobar %}
     * {{ foobar.bar() }}
     */
    public static Collection<PsiElement> getImportedMacrosNamespaces(@NotNull PsiFile psiFile, @NotNull String macroName) {
        Collection<PsiElement> macros = new ArrayList<>();

        visitImportedMacrosNamespaces(psiFile, pair -> {
            if(pair.getFirst().getName().equals(macroName)) {
                macros.add(pair.getSecond());
            }
        });

        return macros;
    }

    /**
     * {% import _self as foobar %}
     * {% import 'foobar.html.twig' as foobar %}
     */
    public static void visitImportedMacrosNamespaces(@NotNull PsiFile psiFile, @NotNull Consumer<Pair<TwigMacro, PsiElement>> consumer) {
        PsiElement[] importPsiElements = PsiTreeUtil.collectElements(psiFile, psiElement ->
            psiElement.getNode().getElementType() == TwigElementTypes.IMPORT_TAG
        );

        for (PsiElement importPsiElement : importPsiElements) {
            PsiElement firstChild = importPsiElement.getFirstChild();
            if(firstChild == null) {
                continue;
            }

            PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(firstChild, TwigTokenTypes.TAG_NAME);
            if(tagName == null || !"import".equals(tagName.getText())) {
                continue;
            }

            Collection<PsiFile> macroFiles = new HashSet<>();


            Pair<String, PsiElement> pair = getTemplateNameOnStringAndSelfWithNextPsiElement(tagName, TwigTokenTypes.AS_KEYWORD);
            if(pair == null) {
                continue;
            }

            PsiElement asVariable = PsiElementUtils.getNextSiblingAndSkip(pair.getSecond(), TwigTokenTypes.IDENTIFIER);
            if(asVariable == null) {
                continue;
            }

            String asName = asVariable.getText();
            String template = pair.getFirst();

            // resolve _self and template name
            if(template.equals("_self")) {
                macroFiles.add(psiFile);
            } else {
                macroFiles.addAll(Arrays.asList(TwigHelper.getTemplatePsiElements(psiFile.getProject(), template)));
            }

            if(macroFiles.size() > 0) {
                for (PsiFile macroFile : macroFiles) {
                    TwigUtil.visitMacros(macroFile, tagPair -> consumer.consume(Pair.create(
                        new TwigMacro(asName + '.' + tagPair.getFirst().getName(), template),
                        tagPair.getSecond()
                    )));
                }
            }
        }
    }

    /**
     * {% set foobar = 'foo' %}
     * {% set foo %}{% endset %}
     *
     * TODO: {% set foo, bar = 'foo', 'bar' %}
     */
    @NotNull
    public static Collection<TwigSet> getSetDeclaration(@NotNull PsiFile psiFile) {
        Collection<TwigSet> sets = new ArrayList<>();

        PsiElement[] psiElements = PsiTreeUtil.collectElements(psiFile, psiElement ->
            psiElement.getNode().getElementType() == TwigElementTypes.SET_TAG
        );

        for (PsiElement psiElement : psiElements) {
            PsiElement firstChild = psiElement.getFirstChild();
            if(firstChild == null) {
                continue;
            }

            PsiElement tagName = PsiElementUtils.getNextSiblingAndSkip(firstChild, TwigTokenTypes.TAG_NAME);
            if(tagName == null || !"set".equals(tagName.getText())) {
                continue;
            }

            PsiElement setVariable = PsiElementUtils.getNextSiblingAndSkip(tagName, TwigTokenTypes.IDENTIFIER);
            if(setVariable == null) {
                continue;
            }

            String text = setVariable.getText();
            if(StringUtils.isNotBlank(text)) {
                sets.add(new TwigSet(text));
            }
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

    @NotNull
    public static Set<String> getTemplateName(@NotNull VirtualFile virtualFile, @NotNull TemplateFileMap map) {
        return map.getNames(virtualFile);
    }

    @NotNull
    public static Set<String> getTemplateName(@NotNull TwigFile twigFile) {
        return getTemplateName(twigFile.getVirtualFile(), TwigHelper.getTemplateMap(twigFile.getProject(), true, false));
    }

    public static Map<String, PsiVariable> collectControllerTemplateVariables(@NotNull TwigFile twigFile) {
        Map<String, PsiVariable> vars = new HashMap<>();

        Method method = findTwigFileController(twigFile);
        if(method != null) {
            vars.putAll(PhpMethodVariableResolveUtil.collectMethodVariables(method));
        }

        for(Function methodIndex : getTwigFileMethodUsageOnIndex(twigFile)) {
            vars.putAll(PhpMethodVariableResolveUtil.collectMethodVariables(methodIndex));
        }

        return vars;

    }

    /**
     * Collect function variables scopes for given Twig file
     */
    @NotNull
    public static Set<Function> getTwigFileMethodUsageOnIndex(@NotNull TwigFile psiFile) {
        return getTwigFileMethodUsageOnIndex(psiFile.getProject(), TwigUtil.getTemplateName(psiFile));
    }

    /**
     * Collect function scopes to search for Twig variable of given template names: "foo.html.twig"
     */
    @NotNull
    public static Set<Function> getTwigFileMethodUsageOnIndex(@NotNull Project project, @NotNull Collection<String> keys) {
        if(keys.size() == 0) {
            return Collections.emptySet();
        }

        final Set<String> fqn = new HashSet<>();
        for(String key: keys) {
            for (TemplateUsage usage : FileBasedIndex.getInstance().getValues(PhpTwigTemplateUsageStubIndex.KEY, key, GlobalSearchScope.allScope(project))) {
                fqn.addAll(usage.getScopes());
            }
        }

        final Set<Function> methods = new HashSet<>();

        for (String s : fqn) {
            // function: "\foo"
            if(!s.contains(".")) {
                methods.addAll(PhpIndex.getInstance(project).getFunctionsByFQN("\\" + s));
                continue;
            }

            // classes: "\foo.action"
            String[] split = s.split("\\.");
            if(split.length != 2) {
                continue;
            }

            Method method = PhpElementsUtil.getClassMethod(project, split[0], split[1]);
            if(method == null) {
                continue;
            }

            methods.add(method);
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

        List<String> templateNames = new ArrayList<>();
        for(Map.Entry<String, VirtualFile> entry: files.entrySet()) {
            if(entry.getValue().equals(currentFile)) {
                templateNames.add(entry.getKey());
            }
        }

        if(templateNames.size() > 0) {

            // bundle names wins
            if(templateNames.size() > 1) {
                templateNames.sort(new TemplateStringComparator());
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
                        for (String s : TwigHelper.getTwigExtendsTagTemplates((TwigExtendsTag) psiElement)) {
                            containerElement.addExtend(s);
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

    /**
     * Gets a template name from "app" or bundle getParent overwrite
     *
     * app/Resources/AcmeBlogBundle/views/Blog/index.html.twig
     * src/Acme/UserBundle/Resources/views/index.html.twig
     */
    @Nullable
    public static String getTemplateNameByOverwrite(@NotNull Project project, @NotNull VirtualFile virtualFile) {

        String relativePath = VfsUtil.getRelativePath(virtualFile, project.getBaseDir());
        if(relativePath == null) {
            return null;
        }

        // app/Resources/AcmeBlogBundle/views/Blog/index.html.twig
        Matcher matcher = Pattern.compile("app/Resources/([^/]*Bundle)/views/(.*)$").matcher(relativePath);
        if (matcher.find()) {
            return TwigHelper.normalizeTemplateName(matcher.group(1) + ":" + matcher.group(2));
        }

        // src/Acme/UserBundle/Resources/views/index.html.twig
        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(project);
        SymfonyBundle containingBundle = symfonyBundleUtil.getContainingBundle(virtualFile);
        if(containingBundle == null) {
            return null;
        }

        String relative = containingBundle.getRelative(virtualFile);
        if(relative == null) {
            return null;
        }

        if(!relative.startsWith("Resources/views/")) {
            return null;
        }

        String parentBundleName = containingBundle.getParentBundleName();
        if(parentBundleName == null) {
            return null;
        }

        return TwigHelper.normalizeTemplateName(containingBundle.getName() + ":" + relative.substring("Resources/views/".length(), relative.length()));
    }

    /**
     * {% include "foo/#{segment.typeKey}.html.twig" with {'segment': segment} %}
     * {% include "foo/#{1 + 2}.html.twig" %}
     * {% include "foo/" ~ segment.typeKey ~ ".html.twig" %}
     */
    public static boolean isValidTemplateString(@NotNull PsiElement element) {
        String templateName = element.getText();

        if(templateName.matches(".*#\\{.*\\}.*")) {
            return false;
        }

        if(PlatformPatterns.psiElement()
            .afterLeafSkipping(
                TwigHelper.STRING_WRAP_PATTERN,
                PlatformPatterns.psiElement(TwigTokenTypes.CONCAT)
            ).accepts(element) ||
            PlatformPatterns.psiElement().beforeLeafSkipping(
                TwigHelper.STRING_WRAP_PATTERN,
                PlatformPatterns.psiElement(TwigTokenTypes.CONCAT)
            ).accepts(element)) {

            return false;
        }

        return true;
    }

    @NotNull
    public static Collection<String> getCreateAbleTemplatePaths(@NotNull Project project, @NotNull String templateName) {
        templateName = TwigHelper.normalizeTemplateName(templateName);

        Collection<String> paths = new HashSet<>();

        for (TwigPath twigPath : TwigHelper.getTwigNamespaces(project)) {
            if(!twigPath.isEnabled()) {
                continue;
            }

            if(templateName.startsWith("@")) {
                int i = templateName.indexOf("/");
                if(i > 0 && templateName.substring(1, i).equals(twigPath.getNamespace())) {
                    paths.add(twigPath.getRelativePath(project) + "/" + templateName.substring(i + 1));
                }
            } else if(twigPath.getNamespaceType() == TwigPathIndex.NamespaceType.BUNDLE && templateName.matches("^\\w+Bundle:.*")) {

                int i = templateName.indexOf("Bundle:");
                String substring = templateName.substring(0, i + 6);
                if(substring.equals(twigPath.getNamespace())) {
                    paths.add(twigPath.getRelativePath(project) + "/" + templateName.substring(templateName.indexOf(":") + 1).replace(":", "/"));
                }
            } else if(twigPath.isGlobalNamespace() && !templateName.contains(":") && !templateName.contains("@")) {
                paths.add(twigPath.getRelativePath(project) + "/" + StringUtils.stripStart(templateName, "/"));
            }

        }

        return paths;
    }

    /**
     * Collects all files that include, extends, ... a given files
     */
    @NotNull
    public static Collection<PsiFile> getTemplateFileReferences(@NotNull final PsiFile psiFile, @NotNull TemplateFileMap files) {
        List<PsiFile> twigChild = new ArrayList<>();
        getTemplateFileReferences(files.getTemplates(), psiFile, twigChild, 8);
        return twigChild;
    }

    private static void getTemplateFileReferences(@NotNull Map<String, VirtualFile> files, @NotNull final PsiFile psiFile, @NotNull final List<PsiFile> twigChild, int depth) {
        if(depth <= 0) {
            return;
        }

        // use set here, we have multiple shortcut on one file, but only one is required
        final HashSet<VirtualFile> virtualFiles = new LinkedHashSet<>();

        for(Map.Entry<String, VirtualFile> entry: files.entrySet()) {

            // getFilesWithKey dont support keyset with > 1 items (bug?), so we cant merge calls
            if(entry.getValue().equals(psiFile.getVirtualFile())) {
                String key = entry.getKey();
                FileBasedIndexImpl.getInstance().getFilesWithKey(TwigExtendsStubIndex.KEY, new HashSet<>(Collections.singletonList(key)), virtualFile -> {
                    virtualFiles.add(virtualFile);
                    return true;
                }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(psiFile.getProject()), TwigFileType.INSTANCE));

            }

        }

        // finally resolve virtual file to twig files
        for(VirtualFile virtualFile: virtualFiles) {
            PsiFile resolvedPsiFile = PsiManager.getInstance(psiFile.getProject()).findFile(virtualFile);
            if(resolvedPsiFile != null) {
                twigChild.add(resolvedPsiFile);
                getTemplateFileReferences(files, resolvedPsiFile, twigChild, --depth);
            }

        }
    }

    /**
     * Visit all possible Twig include file pattern
     */
    public static void visitTemplateIncludes(@NotNull TwigFile twigFile, @NotNull Consumer<TemplateInclude> consumer) {
        visitTemplateIncludes(
            twigFile,
            consumer,
            TemplateInclude.TYPE.EMBED, TemplateInclude.TYPE.INCLUDE, TemplateInclude.TYPE.INCLUDE_FUNCTION, TemplateInclude.TYPE.FROM, TemplateInclude.TYPE.IMPORT, TemplateInclude.TYPE.FORM_THEME
        );
    }

    public static void visitTemplateIncludes(@NotNull TwigFile twigFile, @NotNull Consumer<TemplateInclude> consumer, @NotNull TemplateInclude.TYPE... types) {
        if(types.length == 0) {
            return;
        }

        List<TemplateInclude.TYPE> myTypes = Arrays.asList(types);

        PsiTreeUtil.collectElements(twigFile, psiElement -> {
            if(psiElement instanceof TwigTagWithFileReference) {
                // {% include %}
                if(myTypes.contains(TemplateInclude.TYPE.INCLUDE)) {
                    if(psiElement.getNode().getElementType() == TwigElementTypes.INCLUDE_TAG) {
                        for (String templateName : TwigHelper.getIncludeTagStrings((TwigTagWithFileReference) psiElement)) {
                            if(StringUtils.isNotBlank(templateName)) {
                                consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.INCLUDE));
                            }
                        }
                    }
                }

                // {% import "foo.html.twig"
                if(myTypes.contains(TemplateInclude.TYPE.IMPORT)) {
                    PsiElement embedTag = PsiElementUtils.getChildrenOfType(psiElement, TwigHelper.getTagNameParameterPattern(TwigElementTypes.IMPORT_TAG, "import"));
                    if(embedTag != null) {
                        String templateName = embedTag.getText();
                        if(StringUtils.isNotBlank(templateName)) {
                            consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.IMPORT));
                        }
                    }
                }

                // {% from 'forms.html' import ... %}
                if(myTypes.contains(TemplateInclude.TYPE.FROM)) {
                    PsiElement embedTag = PsiElementUtils.getChildrenOfType(psiElement, TwigHelper.getTagNameParameterPattern(TwigElementTypes.IMPORT_TAG, "from"));
                    if(embedTag != null) {
                        String templateName = embedTag.getText();
                        if(StringUtils.isNotBlank(templateName)) {
                            consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.IMPORT));
                        }
                    }
                }
            } else if(psiElement instanceof TwigCompositeElement) {
                // {{ include() }}
                // {{ source() }}
                if(myTypes.contains(TemplateInclude.TYPE.INCLUDE_FUNCTION)) {
                    PsiElement includeTag = PsiElementUtils.getChildrenOfType(psiElement, TwigHelper.getPrintBlockFunctionPattern("include", "source"));
                    if(includeTag != null) {
                        String templateName = includeTag.getText();
                        if(StringUtils.isNotBlank(templateName)) {
                            consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.INCLUDE_FUNCTION));
                        }
                    }
                }

                // {% embed "foo.html.twig"
                if(myTypes.contains(TemplateInclude.TYPE.EMBED)) {
                    PsiElement embedTag = PsiElementUtils.getChildrenOfType(psiElement, TwigHelper.getEmbedPattern());
                    if(embedTag != null) {
                        String templateName = embedTag.getText();
                        if(StringUtils.isNotBlank(templateName)) {
                            consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.EMBED));
                        }
                    }
                }

                if(myTypes.contains(TemplateInclude.TYPE.FORM_THEME) && psiElement.getNode().getElementType() == TwigElementTypes.TAG) {
                    PsiElement tagElement = PsiElementUtils.getChildrenOfType(psiElement, PlatformPatterns.psiElement().withElementType(TwigTokenTypes.TAG_NAME));
                    if(tagElement != null) {
                        String text = tagElement.getText();
                        if("form_theme".equals(text)) {
                            // {% form_theme form.child 'form/fields_child.html.twig' %}
                            PsiElement childrenOfType = PsiElementUtils.getNextSiblingAndSkip(tagElement, TwigTokenTypes.STRING_TEXT,
                                TwigTokenTypes.IDENTIFIER, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE, TwigTokenTypes.DOT
                            );

                            if(childrenOfType != null) {
                                String templateName = childrenOfType.getText();
                                if(StringUtils.isNotBlank(templateName)) {
                                    consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.FORM_THEME));
                                }
                            }

                            // {% form_theme form.child 'form/fields_child.html.twig' %}
                            PsiElement withElement = PsiElementUtils.getNextSiblingOfType(tagElement, PlatformPatterns.psiElement().withElementType(TwigTokenTypes.IDENTIFIER).withText("with"));
                            if(withElement != null) {
                                PsiElement arrayStart = PsiElementUtils.getNextSiblingAndSkip(tagElement, TwigTokenTypes.LBRACE_SQ,
                                    TwigTokenTypes.IDENTIFIER, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE, TwigTokenTypes.DOT
                                );

                                if(arrayStart != null) {
                                    TwigHelper.visitStringInArray(arrayStart, pair ->
                                        consumer.consume(new TemplateInclude(psiElement, pair.getFirst(), TemplateInclude.TYPE.FORM_THEME))
                                    );
                                }
                            }
                        }
                    }
                }
            }

            return false;
        });
    }

    /**
     * Get all macros inside file
     *
     * {% macro foobar %}{% endmacro %}
     * {% macro input(name, value, type, size) %}{% endmacro %}
     */
    @NotNull
    public static Collection<TwigMacroTag> getMacros(@NotNull PsiFile file) {
        Collection<TwigMacroTag> macros = new ArrayList<>();

        visitMacros(file, pair -> macros.add(pair.getFirst()));

        return macros;
    }

    /**
     * Get all macros inside file
     *
     * {% macro foobar %}{% endmacro %}
     * {% macro input(name, value, type, size) %}{% endmacro %}
     */
    @NotNull
    private static void visitMacros(@NotNull PsiFile file, Consumer<Pair<TwigMacroTag, PsiElement>> consumer) {
        PsiElement[] psiElements = PsiTreeUtil.collectElements(file, psiElement ->
            psiElement.getNode().getElementType() == TwigElementTypes.MACRO_TAG
        );

        for (PsiElement psiElement : psiElements) {
            PsiElement firstChild = psiElement.getFirstChild();
            if(firstChild == null) {
                continue;
            }

            PsiElement macroNamePsi = PsiElementUtils.getNextSiblingAndSkip(firstChild, TwigTokenTypes.IDENTIFIER, TwigTokenTypes.TAG_NAME);
            if(macroNamePsi == null) {
                continue;
            }

            String macroName = macroNamePsi.getText();

            String parameter = null;
            PsiElement nextSiblingAndSkip = PsiElementUtils.getNextSiblingAndSkip(macroNamePsi, TwigTokenTypes.LBRACE);
            if(nextSiblingAndSkip != null) {
                PsiElement nextSiblingOfType = PsiElementUtils
                    .getNextSiblingOfType(nextSiblingAndSkip, PlatformPatterns.psiElement()
                        .withElementType(TwigTokenTypes.RBRACE));

                if(nextSiblingOfType != null) {
                    parameter = file.getText().substring(nextSiblingAndSkip.getTextOffset(), nextSiblingOfType.getTextOffset() + 1);
                }
            }

            consumer.consume(Pair.create(new TwigMacroTag(macroName, parameter), psiElement));
        }
    }
}
