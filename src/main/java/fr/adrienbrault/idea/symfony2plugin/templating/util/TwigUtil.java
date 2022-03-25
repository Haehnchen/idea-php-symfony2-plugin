package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.*;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import com.intellij.util.containers.ArrayListSet;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment;
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.phpunit.PhpUnitUtil;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.*;
import de.espend.idea.php.annotation.util.AnnotationUtil;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.action.comparator.ValueComparator;
import fr.adrienbrault.idea.symfony2plugin.asset.AssetDirectoryReader;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigFileUsage;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension;
import fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtensionParameter;
import fr.adrienbrault.idea.symfony2plugin.stubs.SymfonyProcessors;
import fr.adrienbrault.idea.symfony2plugin.stubs.dict.TemplateUsage;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.PhpTwigTemplateUsageStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigBlockIndexExtension;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigMacroFunctionStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.TemplateLookupElement;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.*;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigNamespaceSetting;
import fr.adrienbrault.idea.symfony2plugin.templating.path.TwigPath;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.twig.assets.TwigNamedAssetsServiceParser;
import fr.adrienbrault.idea.symfony2plugin.util.*;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil;
import fr.adrienbrault.idea.symfony2plugin.util.service.ServiceXmlParserFactory;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import icons.TwigIcons;
import kotlin.Triple;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLTokenTypes;
import org.jetbrains.yaml.YAMLUtil;
import org.jetbrains.yaml.psi.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern.captureVariableOrField;
import static fr.adrienbrault.idea.symfony2plugin.util.StringUtils.underscore;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigUtil {
    public static final String DOC_SEE_REGEX_WITHOUT_SEE  = "([-@\\./\\:\\w\\\\\\[\\]]+)[\\s]*";

    /**
     * Twig namespace for "non namespace"; its also a reserved value in Twig library
     */
    public static final String MAIN = "__main__";

    /**
     * Twig namespace types; mainly switch for its prefix
     */
    public enum NamespaceType {
        BUNDLE, ADD_PATH
    }

    private static final ExtensionPointName<TwigNamespaceExtension> TWIG_NAMESPACE_EXTENSIONS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.TwigNamespaceExtension"
    );

    private static final ExtensionPointName<TwigFileUsage> TWIG_FILE_USAGE_EXTENSIONS = new ExtensionPointName<>(
        "fr.adrienbrault.idea.symfony2plugin.extension.TwigFileUsage"
    );

    private static final Key<CachedValue<Map<String, Set<VirtualFile>>>> TEMPLATE_CACHE_TWIG = new Key<>("TEMPLATE_CACHE_TWIG");

    private static final Key<CachedValue<Map<String, Set<VirtualFile>>>> TEMPLATE_CACHE_ALL = new Key<>("TEMPLATE_CACHE_ALL");

    public static String[] CSS_FILES_EXTENSIONS = new String[] { "css", "less", "sass", "scss" };

    public static String[] JS_FILES_EXTENSIONS = new String[] { "js", "dart", "coffee" };

    public static String[] IMG_FILES_EXTENSIONS = new String[] { "png", "jpg", "jpeg", "gif", "svg"};

    public static String TEMPLATE_ANNOTATION_CLASS = "\\Sensio\\Bundle\\FrameworkExtraBundle\\Configuration\\Template";

    @NotNull
    public static String[] getControllerMethodShortcut(@NotNull Method method) {
        // indexAction
        String methodName = method.getName();

        PhpClass phpClass = method.getContainingClass();
        if(null == phpClass) {
            return new String[0];
        }

        // defaultController
        // default/Folder/FolderController
        String className = phpClass.getName();
        if(!className.endsWith("Controller")) {
            return new String[0];
        }

        SymfonyBundleUtil symfonyBundleUtil = new SymfonyBundleUtil(method.getProject());
        SymfonyBundle symfonyBundle = symfonyBundleUtil.getContainingBundle(phpClass);
        if(symfonyBundle == null) {
            return new String[0];
        }

        // check if files is in <Bundle>/Controller/*
        PhpClass bundleClass = symfonyBundle.getPhpClass();
        if(!phpClass.getNamespaceName().startsWith(bundleClass.getNamespaceName() + "Controller\\")) {
            return new String[0];
        }

        // strip the controller folder name
        String templateFolderName = phpClass.getNamespaceName().substring(bundleClass.getNamespaceName().length() + 11);

        // HomeBundle:default:indexes
        // HomeBundle:default/Test:indexes
        templateFolderName = templateFolderName.replace("\\", "/");

        String shortcutName;
        String shortcutNameForOldNotation;

        // Foobar without (.html.twig)
        String templateName = className.substring(0, className.lastIndexOf("Controller"));

        if(methodName.equals("__invoke")) {
            // AppBundle::foo_bar.html.twig
            shortcutName = String.format(
                "%s::%s%s",
                symfonyBundle.getName(),
                underscore(templateFolderName),
                underscore(templateName)
            );

            // AppBundle::FooBar.html.twig
            shortcutNameForOldNotation = String.format(
                "%s::%s%s",
                symfonyBundle.getName(),
                templateFolderName,
                templateName
            );
        } else {
            // FooBundle:foo_bar:foo_bar.html.twig
            shortcutName = String.format(
                "%s:%s%s:%s",
                symfonyBundle.getName(),
                underscore(templateFolderName),
                underscore(templateName),
                underscore(StringUtils.removeEnd(methodName, "Action"))
            );

            // FooBundle:FooBar:fooBar.html.twig
            shortcutNameForOldNotation = String.format(
                "%s:%s%s:%s",
                symfonyBundle.getName(),
                templateFolderName,
                templateName,
                StringUtils.removeEnd(methodName, "Action")
            );
        }

        // @TODO: we should support types later on; but nicer
        // HomeBundle:default:indexes.html.twig
        return new String[] {
            shortcutName + ".html.twig",
            shortcutName + ".json.twig",
            shortcutName + ".xml.twig",
            shortcutNameForOldNotation + ".html.twig",
            shortcutNameForOldNotation + ".json.twig",
            shortcutNameForOldNotation + ".xml.twig",
        };
    }

    /**
     * Extract Template names from PhpDocTag
     *
     * "@Template("foo.html.twig")"
     * "@Template(template="foo.html.twig")"
     */
    @Nullable
    public static Pair<String, Collection<PsiElement>> getTemplateAnnotationFiles(@NotNull PhpDocTag phpDocTag) {
        String template = AnnotationUtil.getPropertyValueOrDefault(phpDocTag, "template");
        if(template == null) {
            return null;
        }

        template = normalizeTemplateName(template);
        return Pair.create(template, new HashSet<>(getTemplatePsiElements(phpDocTag.getProject(), template)));
    }

    /**
     * Get templates on "@Template()" and on method attached to given PhpDocTag
     */
    @NotNull
    public static Map<String, Collection<PsiElement>> getTemplateAnnotationFilesWithSiblingMethod(@NotNull PhpDocTag phpDocTag) {
        Map<String, Collection<PsiElement>> targets = new HashMap<>();

        // template on direct PhpDocTag
        Pair<String, Collection<PsiElement>> templateAnnotationFiles = TwigUtil.getTemplateAnnotationFiles(phpDocTag);
        if(templateAnnotationFiles != null) {
            targets.put(templateAnnotationFiles.getFirst(), templateAnnotationFiles.getSecond());
        }

        // templates on "Method" of PhpDocTag
        PhpDocComment phpDocComment = PsiTreeUtil.getParentOfType(phpDocTag, PhpDocComment.class);
        if(phpDocComment != null) {
            PsiElement method = phpDocComment.getNextPsiSibling();
            if(method instanceof Method) {
                for (String name : TwigUtil.getControllerMethodShortcut((Method) method)) {
                    targets.put(name, new HashSet<>(getTemplatePsiElements(method.getProject(), name)));
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
        // {% embed 'foo.html.twig' with { foo: '<caret>'|trans } %}
        PsiElement parent = position.getParent();
        if(parent != null && parent.getNode().getElementType() == TwigElementTypes.LITERAL) {
            PsiElement parent2 = parent.getParent();
            if(parent2 != null && parent2.getNode().getElementType() == TwigElementTypes.EMBED_TAG) {
                PsiElement firstParent = PsiTreeUtil.findFirstParent(parent, true, psiElement -> {
                    IElementType elementType = psiElement.getNode().getElementType();
                    return elementType != TwigElementTypes.EMBED_TAG && elementType != TwigElementTypes.EMBED_STATEMENT;
                });

                if(firstParent != null) {
                    position = firstParent;
                }
            }
        }

        // find embed or file scope
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
                        if(TwigPattern.getTransDefaultDomainPattern().accepts(element)) {
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
     * "form(form.name)" => "form"
     */
    @Nullable
    public static PsiElement getTwigFunctionParameterIdentifierPsi(@NotNull PsiElement psiElement) {
        // "form(form.name)" => "form"
        TwigFieldReference childOfType = PsiTreeUtil.findChildOfType(psiElement, TwigFieldReference.class);
        if (childOfType != null) {
            TwigPsiReference owner = childOfType.getOwner();
            if (owner != null) {
                return owner.getFirstChild();
            }
        }

        // "form(form)" => "form"
        TwigVariableReference childOfType1 = PsiTreeUtil.findChildOfType(psiElement, TwigVariableReference.class);
        if (childOfType1 != null) {
            return childOfType1.getFirstChild();
        }

        return null;
    }

    /**
     * Search Twig element to find use trans_default_domain and returns given string parameter
     */
    @Nullable
    public static String getTransDefaultDomainOnScopeOrInjectedElement(@NotNull PsiElement position) {
        if(position.getContainingFile().getContainingFile().getFileType() == TwigFileType.INSTANCE) {
            return getTransDefaultDomainOnScope(position);
        }

        PsiElement element = getElementOnTwigViewProvider(position);
        if(element != null) {
            return getTransDefaultDomainOnScope(element);
        }

        return null;
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
            TwigElementTypes.LITERAL, TwigTokenTypes.LBRACE_SQ, TwigTokenTypes.RBRACE_SQ, TwigTokenTypes.IDENTIFIER,
            TwigElementTypes.VARIABLE_REFERENCE, TwigElementTypes.FIELD_REFERENCE
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
                    TwigElementTypes.VARIABLE_REFERENCE, TwigElementTypes.FIELD_REFERENCE,
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
                if (text != null && TwigPattern.getParameterAsStringPattern().accepts(text)) {
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

            Collection<PsiFile> foreignPsiFile = new HashSet<>();
            if ("_self".equals(twigMacro.getTemplate())) {
                foreignPsiFile.add(psiFile);
            } else {
                foreignPsiFile.addAll(getTemplatePsiElements(psiFile.getProject(), twigMacro.getTemplate()));
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
    private static void visitImportedMacrosNamespaces(@NotNull PsiFile psiFile, @NotNull Consumer<Pair<TwigMacro, PsiElement>> consumer) {
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

            PsiElement asVariable = PhpPsiUtil.getNextSiblingIgnoreWhitespace(pair.getSecond(), true);
            if(!(asVariable instanceof TwigPsiReference)) {
                continue;
            }

            String asName = asVariable.getText();
            String template = pair.getFirst();

            // resolve _self and template name
            if(template.equals("_self")) {
                macroFiles.add(psiFile);
            } else {
                macroFiles.addAll(getTemplatePsiElements(psiFile.getProject(), template));
            }

            if(macroFiles.size() > 0) {
                for (PsiFile macroFile : macroFiles) {
                    TwigUtil.visitMacros(macroFile, tagPair -> consumer.consume(Pair.create(
                        new TwigMacro(asName + '.' + tagPair.getFirst().getName(), template).withParameter(tagPair.getFirst().getParameters()),
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
    public static Collection<String> getSetDeclaration(@NotNull PsiFile psiFile) {
        Collection<String> sets = new ArrayList<>();

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

            PsiElement setVariable = PhpPsiUtil.getNextSiblingIgnoreWhitespace(tagName, true);
            if(!(setVariable instanceof TwigPsiReference)) {
                continue;
            }

            String text = setVariable.getText();
            if(StringUtils.isNotBlank(text)) {
                sets.add(text);
            }
        }

        return sets;
    }

    /**
     * Find a controller method which possibly rendered the tempalte
     *
     * Foobar/Bar.html.twig" => FoobarController::barAction
     * Foobar/Bar.html.twig" => FoobarController::bar
     * Foobar.html.twig" => FoobarController::__invoke
     */
    @NotNull
    public static Collection<Method> findTwigFileController(@NotNull TwigFile twigFile) {

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(twigFile.getProject()).getContainingBundle(twigFile);
        if(symfonyBundle == null) {
            return Collections.emptyList();
        }

        String relativePath = symfonyBundle.getRelativePath(twigFile.getVirtualFile());
        if(relativePath == null || !relativePath.startsWith("Resources/views/")) {
            return Collections.emptyList();
        }

        String viewPath = relativePath.substring("Resources/views/".length());

        String className = null;
        Collection<String> methodNames = new ArrayList<>();

        Matcher methodMatcher = Pattern.compile(".*/(\\w+)\\.\\w+\\.twig").matcher(viewPath);
        if(methodMatcher.find()) {
            // Foobar/Bar.html.twig" => FoobarController::barAction
            // Foobar/Bar.html.twig" => FoobarController::bar
            methodNames.add(methodMatcher.group(1) + "Action");
            methodNames.add(methodMatcher.group(1));

            className = String.format(
                "%sController\\%sController",
                symfonyBundle.getNamespaceName(),
                viewPath.substring(0, viewPath.lastIndexOf("/")).replace("/", "\\")
            );
        } else {
            // Foobar.html.twig" => FoobarController::__invoke
            Matcher invokeMatcher = Pattern.compile("^(\\w+)\\.\\w+\\.twig").matcher(viewPath);
            if(invokeMatcher.find()) {
                className = String.format(
                    "%sController\\%sController",
                    symfonyBundle.getNamespaceName(),
                    invokeMatcher.group(1)
                );

                methodNames.add("__invoke");
            }
        }

        // found not valid template name pattern
        if(className == null || methodNames.size() == 0) {
            return Collections.emptyList();
        }

        // find multiple targets
        Collection<Method> methods = new HashSet<>();
        for (String methodName : methodNames) {
            Method method = PhpElementsUtil.getClassMethod(twigFile.getProject(), className, methodName);
            if(method != null) {
                methods.add(method);
            }
        }

        return methods;
    }

    public static Map<String, PsiVariable> collectControllerTemplateVariables(@NotNull TwigFile twigFile) {
        Map<String, PsiVariable> vars = new HashMap<>();

        for (Method method : findTwigFileController(twigFile)) {
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
    public static Set<Function> getTwigFileMethodUsageOnIndex(@NotNull TwigFile twigFile) {
        return getTwigFileMethodUsageOnIndex(twigFile.getProject(), getTemplateNamesForFile(twigFile));
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

    @NotNull
    public static String getPresentableTemplateName(@NotNull PsiElement psiElement, boolean shortMode) {
        VirtualFile currentFile = psiElement.getContainingFile().getVirtualFile();

        List<String> templateNames = new ArrayList<>(
            getTemplateNamesForFile(psiElement.getProject(), currentFile)
        );

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

        String relativePath = VfsUtil.getRelativePath(currentFile, ProjectUtil.getProjectDir(psiElement), '/');
        return relativePath != null ? relativePath : currentFile.getPath();

    }

    /**
     * Generate a mapped template name file multiple relation:
     *
     * foo.html.twig => ["views/foo.html.twig", "templates/foo.html.twig"]
     */
    @NotNull
    public static Map<String, Set<VirtualFile>> getTemplateMap(@NotNull Project project) {
        return getTemplateMap(project, false);
    }

    /**
     * Generate a mapped template name file multiple relation:
     *
     * foo.html.twig => ["views/foo.html.twig", "templates/foo.html.twig"]
     */
    @NotNull
    public static synchronized Map<String, Set<VirtualFile>> getTemplateMap(@NotNull Project project, boolean usePhp) {
        Map<String, Set<VirtualFile>> templateMapProxy;

        // cache twig and all files,
        // only PHP files we dont need to cache
        if(!usePhp) {
            templateMapProxy = CachedValuesManager.getManager(project).getCachedValue(
                project,
                TEMPLATE_CACHE_TWIG,
                new MyAllTemplateFileMapCachedValueProvider(project),
                false
            );
        } else {
            // cache all files
            templateMapProxy = CachedValuesManager.getManager(project).getCachedValue(
                project,
                TEMPLATE_CACHE_ALL,
                new MyAllTemplateFileMapCachedValueProvider(project, true),
                false
            );
        }

        return templateMapProxy;
    }

    /**
     * Cache less visiting template files
     */
    @NotNull
    private static Map<String, Set<VirtualFile>> getTemplateMapProxy(@NotNull Project project, boolean usePhp) {
        List<TwigPath> twigPaths = new ArrayList<>(getTwigNamespaces(project));
        if(twigPaths.size() == 0) {
            return Collections.emptyMap();
        }

        Map<String, Set<VirtualFile>> templateNames = new HashMap<>();

        for (TwigPath twigPath : twigPaths) {
            if(!twigPath.isEnabled()) {
                continue;
            }

            VirtualFile virtualDirectoryFile = twigPath.getDirectory(project);
            if(virtualDirectoryFile == null) {
                continue;
            }

            Map<String, VirtualFile> visitor = MyLimitedVirtualFileVisitor.createResult(
                virtualDirectoryFile,
                project,
                twigPath,
                usePhp
            );

            for (Map.Entry<String, VirtualFile> entry : visitor.entrySet()) {
                if(!templateNames.containsKey(entry.getKey())) {
                    templateNames.put(entry.getKey(), new HashSet<>());
                }

                templateNames.get(entry.getKey()).add(entry.getValue());
            }
        }

        return templateNames;
    }

    @Nullable
    private static TwigNamespaceSetting findManagedTwigNamespace(@NotNull Project project, @NotNull TwigPath twigPath) {
        List<TwigNamespaceSetting> twigNamespaces = Settings.getInstance(project).twigNamespaces;
        if(twigNamespaces == null) {
            return null;
        }

        for(TwigNamespaceSetting twigNamespace: twigNamespaces) {
           if(twigNamespace.equals(project, twigPath)) {
                return twigNamespace;
           }
        }

        return null;
    }

    /**
     * Normalize incoming template names. Provide normalization on indexing and resolving
     *
     * BarBundle:Foo:steps/step_finish.html.twig
     * BarBundle:Foo/steps:step_finish.html.twig
     * "@!Bar/step_finish.html.twig"
     *
     * todo: provide setting for that
     */
    public static String normalizeTemplateName(@NotNull String templateName) {
        // force linux path style
        templateName = templateName.replace("\\", "/");

        if(templateName.startsWith("@") || !templateName.matches("^.*?:.*?:.*?/.*?$")) {
            // Symfony 3.4 overwrite
            // {% extends '@!FOSUser/layout.html.twig' %}
            if(templateName.startsWith("@!")) {
                templateName = "@" + templateName.substring(2);
            }

            return templateName;
        }

        templateName = templateName.replace(":", "/");

        int firstDoublePoint = templateName.indexOf("/");
        int lastDoublePoint = templateName.lastIndexOf("/");

        String bundle = templateName.substring(0, templateName.indexOf("/"));
        String subFolder = templateName.substring(firstDoublePoint, lastDoublePoint);
        String file = templateName.substring(templateName.lastIndexOf("/") + 1);

        return String.format("%s:%s:%s", bundle, StringUtils.strip(subFolder, "/"), file);
    }

    /**
     * Find file in a twig path collection
     *
     * @param project current project
     * @param templateName path known, should not be normalized
     * @return target files
     */
    @NotNull
    public static Collection<VirtualFile> getTemplateFiles(@NotNull Project project, @NotNull String templateName) {
        String normalizedTemplateName = normalizeTemplateName(templateName);

        Collection<VirtualFile> virtualFiles = new HashSet<>();
        for (TwigPath twigPath : getTwigNamespaces(project)) {
            if(!twigPath.isEnabled()) {
                continue;
            }

            if(normalizedTemplateName.startsWith("@")) {
                // @Namespace/base.html.twig
                // @Namespace/folder/base.html.twig
                if(normalizedTemplateName.length() > 1 && twigPath.getNamespaceType() != NamespaceType.BUNDLE) {
                    int i = normalizedTemplateName.indexOf("/");
                    if(i > 0) {
                        String templateNs = normalizedTemplateName.substring(1, i);
                        if(twigPath.getNamespace().equals(templateNs)) {
                            addFileInsideTwigPath(project, normalizedTemplateName.substring(i + 1), virtualFiles, twigPath);
                        }
                    }
                }
            } else if(normalizedTemplateName.startsWith(":")) {
                // ::base.html.twig
                // :Foo:base.html.twig
                if(normalizedTemplateName.length() > 1 && twigPath.getNamespaceType() == NamespaceType.BUNDLE && twigPath.isGlobalNamespace()) {
                    String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/"), "/");
                    addFileInsideTwigPath(project, templatePath, virtualFiles, twigPath);
                }
            } else {
                // FooBundle::base.html.twig
                // FooBundle:Bar:base.html.twig
                if(twigPath.getNamespaceType() == NamespaceType.BUNDLE) {
                    int i = normalizedTemplateName.indexOf(":");
                    if(i > 0) {
                        String templateNs = normalizedTemplateName.substring(0, i);
                        if(twigPath.getNamespace().equals(templateNs)) {
                            String templatePath = StringUtils.strip(normalizedTemplateName.substring(i + 1).replace(":", "/").replace("//", "/"), "/");
                            addFileInsideTwigPath(project, templatePath, virtualFiles, twigPath);
                        }

                    }
                }

                // form_div_layout.html.twig
                if(twigPath.isGlobalNamespace() && twigPath.getNamespaceType() == NamespaceType.ADD_PATH) {
                    String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/"), "/");
                    addFileInsideTwigPath(project, templatePath, virtualFiles, twigPath);
                }

                // Bundle overwrite:
                // FooBundle:index.html -> app/views/FooBundle:index.html
                if(twigPath.isGlobalNamespace() && !normalizedTemplateName.startsWith(":") && !normalizedTemplateName.startsWith("@")) {
                    String templatePath = StringUtils.strip(normalizedTemplateName.replace(":", "/").replace("//", "/"), "/");
                    addFileInsideTwigPath(project, templatePath, virtualFiles, twigPath);
                }
            }
        }

        return virtualFiles;
    }

    /**
     * Find file in a twig path collection
     *
     * @param project current project
     * @param templateName path known, should not be normalized
     * @return target files
     */
    @NotNull
    public static Collection<PsiFile> getTemplatePsiElements(@NotNull Project project, @NotNull String templateName) {
        return PsiElementUtils.convertVirtualFilesToPsiFiles(project, getTemplateFiles(project, templateName));
    }

    /**
     * Switch template file or path target on caret offset "foo/bar.html.twig" or "foo"
     *
     * "foo" "bar.html.twig"
     */
    @NotNull
    public static Collection<PsiElement> getTemplateNavigationOnOffset(@NotNull Project project, @NotNull String templateName, int offset) {
        Set<PsiElement> files = new HashSet<>();

        // try to find a path pattern on current offset after path normalization
        if(offset < templateName.length()) {
            String templateNameWithCaret = normalizeTemplateName(new StringBuilder(templateName).insert(offset, '\u0182').toString());
            offset = templateNameWithCaret.indexOf('\u0182');

            int i = StringUtils.strip(templateNameWithCaret.replace(String.valueOf('\u0182'), "").replace(":", "/"), "/").indexOf("/", offset);
            if(i > 0) {
                files.addAll(getTemplateTargetOnOffset(project, templateName, offset));
            }
        }

        // full filepath fallback: "foo/foo<caret>.html.twig"
        if(files.size() == 0) {
            files.addAll(getTemplatePsiElements(project, templateName));
        }

        return files;
    }

    /**
     * Switch template target on caret offset "foo/bar.html.twig". Resolve template name or directory structure:
     *
     * "foo" "bar.html.twig"
     */
    @NotNull
    public static Collection<PsiElement> getTemplateTargetOnOffset(@NotNull Project project, @NotNull String templateName, int offset) {
        // no match for length
        if(offset > templateName.length()) {
            return Collections.emptyList();
        }

        // please give use a normalized path:
        // Foo:foo:foo => foo/foo/foo
        String templatePathWithFileName = normalizeTemplateName(new StringBuilder(templateName).insert(offset, '\u0182').toString());
        offset = templatePathWithFileName.indexOf('\u0182');

        int indexOf = templatePathWithFileName.replace(":", "/").indexOf("/", offset);
        if(indexOf <= 0) {
            return Collections.emptyList();
        }

        String templatePath = StringUtils.strip(templatePathWithFileName.substring(0, indexOf).replace(String.valueOf('\u0182'), ""), "/");

        Set<VirtualFile> virtualFiles = new HashSet<>();

        for (TwigPath twigPath : getTwigNamespaces(project)) {
            if(!twigPath.isEnabled()) {
                continue;
            }

            if(templatePath.startsWith("@")) {
                // @Namespace/base.html.twig
                // @Namespace/folder/base.html.twig
                if(templatePath.length() > 1 && twigPath.getNamespaceType() != NamespaceType.BUNDLE) {
                    int x = templatePath.indexOf("/");

                    if(x < 0 && templatePath.substring(1).equals(twigPath.getNamespace())) {
                        // Click on namespace itself: "@Foobar"
                        VirtualFile relativeFile = twigPath.getDirectory(project);
                        if (relativeFile != null) {
                            virtualFiles.add(relativeFile);
                        }
                    } else if (x > 0 && templatePath.substring(1, x).equals(twigPath.getNamespace())) {
                        // Click on path: "@Foobar/Foo"
                        VirtualFile relativeFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), templatePath.substring(x + 1).split("/"));
                        if (relativeFile != null) {
                            virtualFiles.add(relativeFile);
                        }
                    }
                }
            } else if(templatePath.startsWith(":")) {
                // ::base.html.twig
                // :Foo:base.html.twig
                if(twigPath.getNamespaceType() == NamespaceType.BUNDLE && twigPath.isGlobalNamespace()) {
                    String replace = StringUtils.strip(templatePath.replace(":", "/"), "/");

                    VirtualFile relativeFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), replace.split("/"));
                    if(relativeFile != null) {
                        virtualFiles.add(relativeFile);
                    }
                }
            } else {
                // FooBundle::base.html.twig
                // FooBundle:Bar:base.html.twig
                if(twigPath.getNamespaceType() == NamespaceType.BUNDLE) {
                    templatePath = templatePath.replace(":", "/");
                    int x = templatePath.indexOf("/");

                    if(x < 0 && templatePath.equals(twigPath.getNamespace())) {
                        // Click on namespace itself: "FooBundle"
                        VirtualFile relativeFile = twigPath.getDirectory(project);
                        if (relativeFile != null) {
                            virtualFiles.add(relativeFile);
                        }
                    } else if(x > 0 && templatePath.substring(0, x).equals(twigPath.getNamespace())) {
                        // Click on path: "FooBundle/Foo"
                        VirtualFile relativeFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), templatePath.substring(x + 1).split("/"));
                        if (relativeFile != null) {
                            virtualFiles.add(relativeFile);
                        }
                    }
                }

                // form_div_layout.html.twig
                if(twigPath.isGlobalNamespace() && twigPath.getNamespaceType() == NamespaceType.ADD_PATH) {
                    VirtualFile relativeFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), templatePath.split("/"));
                    if(relativeFile != null) {
                        virtualFiles.add(relativeFile);
                    }
                }

                // Bundle overwrite:
                // FooBundle:index.html -> app/views/FooBundle:index.html
                if(twigPath.isGlobalNamespace() && !templatePath.startsWith(":") && !templatePath.startsWith("@")) {
                    // @TODO: support this later on; also its deprecated by Symfony
                }
            }
        }

        return virtualFiles
            .stream()
            .map(virtualFile -> PsiManager.getInstance(project).findDirectory(virtualFile))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    /**
     * Resolve TwigFile to its possible template names:
     *
     * "@Foo/test.html.twig"
     * "test.html.twig"
     * "::test.html.twig"
     */
    @NotNull
    public static Collection<String> getTemplateNamesForFile(@NotNull PsiFile twigFile) {
        VirtualFile virtualFile = twigFile.getVirtualFile();
        if(virtualFile == null) {
            return Collections.emptyList();
        }

        return getTemplateNamesForFile(twigFile.getProject(), virtualFile);
    }

    /**
     * Resolve VirtualFile to its possible template names:
     *
     * "@Foo/test.html.twig"
     * "test.html.twig"
     * "::test.html.twig"
     */
    @NotNull
    public static Collection<String> getTemplateNamesForFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        List<TwigPath> collect = getTwigNamespaces(project, true)
            .stream()
            .filter(TwigPath::isEnabled)
            .collect(Collectors.toList());

        return collect.stream()
            .map(twigPath -> getTemplateNameForTwigPath(project, twigPath, virtualFile))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Nullable
    static String getTemplateNameForTwigPath(@NotNull Project project, @NotNull TwigPath twigPath, @NotNull VirtualFile virtualFile) {
        VirtualFile directory = twigPath.getDirectory(project);
        if(directory == null) {
            return null;
        }

        String templatePath = VfsUtil.getRelativePath(virtualFile, directory, '/');
        if(templatePath == null) {
            return null;
        }

        String templateDirectory; // xxx:XXX:xxx
        String templateFile; // xxx:xxx:XXX

        if (templatePath.contains("/")) {
            int lastDirectorySeparatorIndex = templatePath.lastIndexOf("/");
            templateDirectory = templatePath.substring(0, lastDirectorySeparatorIndex);
            templateFile = templatePath.substring(lastDirectorySeparatorIndex + 1);
        } else {
            templateDirectory = "";
            templateFile = templatePath;
        }

        String namespace = twigPath.getNamespace().equals(MAIN) ? "" : twigPath.getNamespace();

        String templateFinalName;
        if(twigPath.getNamespaceType() == NamespaceType.BUNDLE) {
            templateFinalName = namespace + ":" + templateDirectory + ":" + templateFile;
        } else {
            templateFinalName = namespace + "/" + templateDirectory + "/" + templateFile;

            // remove empty path and check for root (global namespace)
            templateFinalName = templateFinalName.replace("//", "/");
            if(templateFinalName.startsWith("/")) {
                templateFinalName = templateFinalName.substring(1);
            } else {
                templateFinalName = "@" + templateFinalName;
            }
        }

        return templateFinalName;
    }

    private static void addFileInsideTwigPath(@NotNull Project project, @NotNull String templatePath, @NotNull Collection<VirtualFile> virtualFiles, @NotNull TwigPath twigPath) {
        VirtualFile virtualFile = VfsUtil.findRelativeFile(twigPath.getDirectory(project), templatePath.split("/"));

        if(virtualFile != null) {
            virtualFiles.add(virtualFile);
        }
    }

    public static List<TwigPath> getTwigNamespaces(@NotNull Project project) {
       return getTwigNamespaces(project, true);
    }

    @NotNull
    public static List<TwigPath> getTwigNamespaces(@NotNull Project project, boolean includeSettings) {
        List<TwigPath> twigPaths = new ArrayList<>();

        // load extension
        TwigNamespaceExtensionParameter parameter = new TwigNamespaceExtensionParameter(project);
        for (TwigNamespaceExtension namespaceExtension : TWIG_NAMESPACE_EXTENSIONS.getExtensions()) {
            twigPaths.addAll(namespaceExtension.getNamespaces(parameter));
        }

        // disable namespace explicitly disabled by user
        for(TwigPath twigPath: twigPaths) {
            TwigNamespaceSetting twigNamespaceSetting = findManagedTwigNamespace(project, twigPath);
            if(twigNamespaceSetting != null) {
                twigPath.setEnabled(false);
            }
        }

        twigPaths = getUniqueTwigTemplatesList(twigPaths);

        if(!includeSettings) {
            return twigPaths;
        }

        List<TwigNamespaceSetting> twigNamespaceSettings = Settings.getInstance(project).twigNamespaces;
        if(twigNamespaceSettings != null) {
            for(TwigNamespaceSetting twigNamespaceSetting: twigNamespaceSettings) {
                if(twigNamespaceSetting.isCustom()) {
                    twigPaths.add(new TwigPath(twigNamespaceSetting.getPath(), twigNamespaceSetting.getNamespace(), twigNamespaceSetting.getNamespaceType(), true).setEnabled(twigNamespaceSetting.isEnabled()));
                }
            }
        }

        return twigPaths;
    }

    /**
     * Build a unique path + namespace + type list
     * normalize also windows linux path
     */
    @NotNull
    public static List<TwigPath> getUniqueTwigTemplatesList(@NotNull Collection<TwigPath> origin) {
        List<TwigPath> twigPaths = new ArrayList<>();

        Set<String> hashes = new HashSet<>();
        for (TwigPath twigPath : origin) {
            // normalize hash; for same path element
            // TODO: move to path object itself
            String hash = twigPath.getNamespaceType() + twigPath.getNamespace() + twigPath.getPath().replace("\\", "/");
            if(hashes.contains(hash)) {
                continue;
            }

            twigPaths.add(twigPath);
            hashes.add(hash);
        }

        return twigPaths;
    }

    @Nullable
    public static String getTwigMethodString(@Nullable PsiElement transPsiElement) {
        if (transPsiElement == null) return null;

        ElementPattern<PsiElement> pattern = PlatformPatterns.psiElement(TwigTokenTypes.RBRACE);

        String currentText = transPsiElement.getText();
        for (PsiElement child = transPsiElement.getNextSibling(); child != null; child = child.getNextSibling()) {
            currentText = currentText + child.getText();
            if (pattern.accepts(child)) {
                //noinspection unchecked
                return currentText;
            }
        }

        return null;
    }

    @NotNull
    public static Collection<VirtualFile> resolveAssetsFiles(@NotNull Project project, @NotNull String assetName, @NotNull String... fileTypes) {
        Collection<VirtualFile> virtualFiles = new HashSet<>();

        // {% javascripts [...] @jquery_js2'%}
        if(assetName.startsWith("@") && assetName.length() > 1) {
            TwigNamedAssetsServiceParser twigPathServiceParser = ServiceXmlParserFactory.getInstance(project, TwigNamedAssetsServiceParser.class);
            String assetNameShortcut = assetName.substring(1);
            if(twigPathServiceParser.getNamedAssets().containsKey(assetNameShortcut)) {
                for (String s : twigPathServiceParser.getNamedAssets().get(assetNameShortcut)) {
                    VirtualFile fileByURL = VfsUtil.findFileByIoFile(new File(s), false);
                    if(fileByURL != null) {
                        virtualFiles.add(fileByURL);
                    }
                }
            }

        }

        virtualFiles.addAll(new AssetDirectoryReader(fileTypes, true).resolveAssetFile(project, assetName));

        return virtualFiles;
    }

    /**
     * twig lexer just giving use a flat psi list for a block. we need custom stuff to resolve this
     * path('route', {'<parameter>':
     * path('route', {'<parameter>': '', '<parameter2>': ''
     */
    @Nullable
    public static String getMatchingRouteNameOnParameter(@NotNull PsiElement startPsiElement) {
        PsiElement parent = startPsiElement.getParent();
        if(parent.getNode().getElementType() != TwigElementTypes.LITERAL) {
            return null;
        }

        final String[] text = {null};
        PsiElementUtils.getPrevSiblingOnCallback(parent, psiElement -> {
            IElementType elementType = psiElement.getNode().getElementType();
            if(elementType == TwigTokenTypes.STRING_TEXT) {

                // Only valid string parameter "('foobar',"
                if(TwigPattern.getFirstFunctionParameterAsStringPattern().accepts(psiElement)){
                    text[0] = psiElement.getText();
                }

                return false;
            }

            // exit on invalid items
            return
                psiElement instanceof PsiWhiteSpace ||
                elementType == TwigTokenTypes.WHITE_SPACE ||
                elementType == TwigTokenTypes.COMMA ||
                elementType == TwigTokenTypes.SINGLE_QUOTE  ||
                elementType == TwigTokenTypes.DOUBLE_QUOTE
            ;
        });

        return text[0];
    }

    @NotNull
    public static Set<String> getTwigMacroSet(Project project) {
        return SymfonyProcessors.createResult(project, TwigMacroFunctionStubIndex.KEY);
    }

    public static Collection<PsiElement> getTwigMacroTargets(final Project project, final String name) {

        final Collection<PsiElement> targets = new ArrayList<>();

        FileBasedIndex.getInstance().getFilesWithKey(TwigMacroFunctionStubIndex.KEY, new HashSet<>(Collections.singletonList(name)), virtualFile -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            if (psiFile != null) {
                PsiTreeUtil.processElements(psiFile, psiElement -> {
                    if (TwigPattern.getTwigMacroNameKnownPattern(name).accepts(psiElement)) {
                        targets.add(psiElement);
                    }

                    return true;

                });
            }

            return true;
        }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE));

        return targets;
    }

    /**
     * Lookup elements for Twig files
     */
    @NotNull
    public static Collection<LookupElement> getTwigLookupElements(@NotNull Project project) {
        VirtualFile baseDir = ProjectUtil.getProjectDir(project);

        return getTemplateMap(project).entrySet()
            .stream()
            .filter(entry -> entry.getValue().size() > 0)
            .map((java.util.function.Function<Map.Entry<String, Set<VirtualFile>>, LookupElement>) entry ->
                new TemplateLookupElement(entry.getKey(), entry.getValue().iterator().next(), baseDir)
            )
            .collect(Collectors.toList());
    }

    /**
     * Lookup elements for Twig and PHP template files
     */
    @NotNull
    public static Collection<LookupElement> getAllTemplateLookupElements(@NotNull Project project) {
        VirtualFile baseDir = ProjectUtil.getProjectDir(project);

        return getTemplateMap(project, true).entrySet()
            .stream()
            .filter(entry -> entry.getValue().size() > 0)
            .map((java.util.function.Function<Map.Entry<String, Set<VirtualFile>>, LookupElement>) entry ->
                new TemplateLookupElement(entry.getKey(), entry.getValue().iterator().next(), baseDir)
            )
            .collect(Collectors.toList());
    }

    /**
     * {% include 'foo.html.twig' %}
     * {% include ['foo.html.twig', 'foo_1.html.twig'] %}
     */
    @NotNull
    public static Collection<String> getIncludeTagStrings(@NotNull TwigTagWithFileReference twigTagWithFileReference) {

        if(twigTagWithFileReference.getNode().getElementType() != TwigElementTypes.INCLUDE_TAG) {
            return Collections.emptySet();
        }

        Collection<String> strings = new LinkedHashSet<>();
        PsiElement firstChild = twigTagWithFileReference.getFirstChild();
        if(firstChild == null) {
            return strings;
        }

        // {% include 'foo.html.twig' %}
        PsiElement psiSingleString = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    TwigPattern.STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
                )
        );

        // single match dont need to go deeper in conditional check, so stop here
        if(psiSingleString != null) {
            String text = psiSingleString.getText();
            if(StringUtils.isNotBlank(text)) {
                strings.add(text);
            }
            return strings;
        }

        // {% include ['foo.html.twig', 'foo_1.html.twig'] %}
        PsiElement arrayMatch = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_SQ));
        if(arrayMatch != null) {
            visitStringInArray(arrayMatch, pair ->
                strings.add(pair.getFirst())
            );
        }

        PsiElement psiQuestion = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.QUESTION));
        if(psiQuestion != null) {
            strings.addAll(getTernaryStrings(psiQuestion));
        }

        return strings;
    }

    /**
     * Visit string values of given array start brace
     *
     * ["foobar", "foobar"]
     * {"foobar", "foobar"}
     */
    private static void visitStringInArray(@NotNull PsiElement arrayStartBrace, @NotNull Consumer<Pair<String, PsiElement>> pair) {
        // match: "([,)''(,])"
        Collection<PsiElement> questString = PsiElementUtils.getNextSiblingOfTypes(arrayStartBrace, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    TwigPattern.STRING_WRAP_PATTERN,
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_SQ),
                        PlatformPatterns.psiElement(TwigTokenTypes.LBRACE_CURL)
                   )
                )
                .beforeLeafSkipping(
                    TwigPattern.STRING_WRAP_PATTERN,
                    PlatformPatterns.or(
                        PlatformPatterns.psiElement(TwigTokenTypes.COMMA),
                        PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_SQ),
                        PlatformPatterns.psiElement(TwigTokenTypes.RBRACE_CURL)
                    )
                )
        );

        for (PsiElement psiElement : questString) {
            String text = psiElement.getText();
            if(StringUtils.isNotBlank(text)) {
                pair.consume(Pair.create(text, psiElement));
            }
        }
    }

    /**
     * Collect all block names in file
     *
     * {% block sds %}, {% block 'sds' %}, {% block "sds" %}, {%- block sds -%}
     * {{ block('foobar') }}
     */
    @NotNull
    public static Collection<TwigBlock> getBlocksInFile(@NotNull TwigFile twigFile) {
        // prefilter elements; dont visit until leaf elements fpr performance
        PsiElement[] blocks = PsiTreeUtil.collectElements(twigFile, psiElement ->
            psiElement instanceof TwigBlockTag || (psiElement instanceof TwigCompositeElement && psiElement.getNode().getElementType() == TwigElementTypes.PRINT_BLOCK)
        );

        Collection<TwigBlock> block = new ArrayList<>();

        for (PsiElement psiElement : blocks) {
            final PsiElement[] target = new PsiElement[1];

            if(psiElement instanceof TwigBlockTag) {
                // {% block sds %}, {% block "sds" %}
                psiElement.acceptChildren(new PsiRecursiveElementVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if(target[0] == null && TwigPattern.getBlockTagPattern().accepts(element)) {
                            target[0] = element;
                        }
                        super.visitElement(element);
                    }
                });

            } else if(psiElement instanceof TwigCompositeElement) {
                // {{ block('foobar') }}
                psiElement.acceptChildren(new PsiRecursiveElementVisitor() {
                    @Override
                    public void visitElement(PsiElement element) {
                        if(target[0] == null && TwigPattern.getPrintBlockOrTagFunctionPattern("block").accepts(element)) {
                            target[0] = element;
                        }
                        super.visitElement(element);
                    }
                });
            }

            if(target[0] == null) {
                continue;
            }

            String blockName = target[0].getText();
            if(StringUtils.isBlank(blockName)) {
                continue;
            }

            block.add(new TwigBlock(blockName, target[0]));
        }

        return block;
    }

    /**
     * Find "extends" template in twig TwigExtendsTag
     *
     * {% extends '::base.html.twig' %}
     * {% extends request.ajax ? "base_ajax.html" : "base.html" %}
     *
     * @param twigExtendsTag Extends tag
     * @return valid template names
     */
    @NotNull
    public static Collection<String> getTwigExtendsTagTemplates(@NotNull TwigExtendsTag twigExtendsTag) {

        Collection<String> strings = new HashSet<>();
        PsiElement firstChild = twigExtendsTag.getFirstChild();
        if(firstChild == null) {
            return strings;
        }

        // single {% extends '::base.html.twig'
        PsiElement psiSingleString = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    TwigPattern.STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
                )
        );

        // single match dont need to go deeper in conditional check, so stop here
        if(psiSingleString != null) {
            String text = psiSingleString.getText();
            if(StringUtils.isNotBlank(text)) {
                strings.add(text);
            }
            return strings;
        }

        PsiElement psiQuestion = PsiElementUtils.getNextSiblingOfType(firstChild, PlatformPatterns.psiElement(TwigTokenTypes.QUESTION));
        if(psiQuestion != null) {
            strings.addAll(getTernaryStrings(psiQuestion));
        }

        return strings;
    }

    /**
     * "foo ? 'foo' : 'bar'"
     */
    private static Collection<String> getTernaryStrings(@NotNull PsiElement psiQuestion) {
        Collection<String> strings = new TreeSet<>();

        // match ? "foo" :
        PsiElement questString = PsiElementUtils.getNextSiblingOfType(psiQuestion, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    TwigPattern.STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.QUESTION)
                )
                .beforeLeafSkipping(
                    TwigPattern.STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.COLON)
                )
        );

        if(questString != null) {
            String text = questString.getText();
            if(StringUtils.isNotBlank(text)) {
                strings.add(text);
            }
        }

        // : "foo"
        PsiElement colonString = PsiElementUtils.getNextSiblingOfType(psiQuestion, PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
                .afterLeafSkipping(
                    TwigPattern.STRING_WRAP_PATTERN,
                    PlatformPatterns.psiElement(TwigTokenTypes.COLON)
                )
        );

        if(colonString != null) {
            String text = colonString.getText();
            if(StringUtils.isNotBlank(text)) {
                strings.add(text);
            }
        }

        return strings;
    }

    /**
     * Find block scope "embed" with self search or file context with foreign extends search
     *
     * {% embed "template.twig" %}{% block <caret> %}
     * {% block <caret> %}
     */
    @NotNull
    public static Pair<Collection<PsiFile>, Boolean> findScopedFile(@NotNull PsiElement psiElement) {

        // {% embed "template.twig" %}{% block <caret> %}
        PsiElement firstParent = getTransDefaultDomainScope(psiElement);

        // {% embed "template.twig" %}
        if(firstParent != null && firstParent.getNode().getElementType() == TwigElementTypes.EMBED_STATEMENT) {
            PsiElement embedTag = firstParent.getFirstChild();
            if(embedTag != null) {
                String templateName = getTemplateNameForEmbedTag(embedTag);
                if(templateName != null) {
                    return Pair.create(getTemplatePsiElements(psiElement.getProject(), templateName), true);
                }
            }

            return Pair.create(Collections.emptyList(), true);
        }

        return Pair.create(Collections.singletonList(psiElement.getContainingFile()), false);
    }

    /**
     * Extract template name from embed tag
     *
     * {% embed "teasers_skeleton.html.twig" %}
     */
    @Nullable
    private static String getTemplateNameForEmbedTag(@NotNull PsiElement embedTag) {
        if(embedTag.getNode().getElementType() == TwigElementTypes.EMBED_TAG) {
            PsiElement fileReference = ContainerUtil.find(YamlHelper.getChildrenFix(embedTag), psiElement ->
                TwigPattern.getTemplateFileReferenceTagPattern().accepts(psiElement)
            );

            if(fileReference != null && TwigUtil.isValidStringWithoutInterpolatedOrConcat(fileReference)) {
                String text = fileReference.getText();
                if(StringUtils.isNotBlank(text)) {
                    return text;
                }
            }
        }

        return null;
    }

    /**
     * Collects Twig path in given yaml configuration
     *
     * twig:
     *  paths:
     *   "%kernel.root_dir%/../src/vendor/bundle/Resources/views": core
     */
    @NotNull
    public static Collection<Pair<String, String>> getTwigPathFromYamlConfig(@NotNull YAMLFile yamlFile) {
        YAMLKeyValue yamlKeyValue = YAMLUtil.getQualifiedKeyInFile(yamlFile, "twig", "paths");
        if(yamlKeyValue == null) {
            return Collections.emptyList();
        }

        YAMLValue value = yamlKeyValue.getValue();
        if(!(value instanceof YAMLMapping)) {
            return Collections.emptyList();
        }

        Collection<Pair<String, String>> pair = new ArrayList<>();

        for (YAMLPsiElement element : value.getYAMLElements()) {
            if(!(element instanceof YAMLKeyValue)) {
                continue;
            }

            String keyText = ((YAMLKeyValue) element).getKeyText();
            if(StringUtils.isBlank(keyText)) {
                continue;
            }

            keyText = keyText.replace("\\", "/").replaceAll("/+", "/");

            // empty value is empty string on out side:
            // "foo: "
            String valueText = "";

            YAMLValue yamlValue = ((YAMLKeyValue) element).getValue();
            if (yamlValue != null) {
                valueText = ((YAMLKeyValue) element).getValueText();
            } else {
                // workaround for foo: !foobar
                // as we got tag element
                PsiElement key = ((YAMLKeyValue) element).getKey();
                if(key != null) {
                    PsiElement nextSiblingOfType = PsiElementUtils.getNextSiblingOfType(key, PlatformPatterns.psiElement(YAMLTokenTypes.TAG));
                    if(nextSiblingOfType != null) {
                        String text = nextSiblingOfType.getText();
                        if(text.startsWith("!")) {
                            valueText = StringUtils.stripStart(text, "!");
                        }
                    }
                }
            }

            // Symfony 3.4 / 4.0: namespace overwrite: "@!Foo" => "@Foo"
            valueText = StringUtils.stripStart(valueText, "!");

            // normalize null value
            if(valueText.equals("~")) {
                valueText = "";
            }

            pair.add(Pair.create(valueText, keyText));
        }

        return pair;
    }

    /**
     * Replaces parameters and relative replaces strings
     *
     * "%kernel.root_dir%/../src/vendor/bundle/Resources/views": core
     * "%kernel.root_dir%" => "/app/../src/vendor/bundle/Resources/views"
     */
    @NotNull
    public static Collection<Pair<String, String>> getTwigPathFromYamlConfigResolved(@NotNull YAMLFile yamlFile) {
        VirtualFile baseDir = ProjectUtil.getProjectDir(yamlFile);

        Collection<Pair<String, String>> paths = new ArrayList<>();

        for (Pair<String, String> pair : getTwigPathFromYamlConfig(yamlFile)) {
            // normalize path; just to be error prune
            String namespacePath = pair.getSecond().replace("\\", "/").replaceAll("/+", "/");
            String namespace = pair.getFirst();

            if(namespacePath.startsWith("%kernel.root_dir%")) {
                // %kernel.root_dir%/../app
                // %kernel.root_dir%/foo
                for (VirtualFile appDir : FilesystemUtil.getAppDirectories(yamlFile.getProject())) {
                    String path = StringUtils.stripStart(namespacePath.substring("%kernel.root_dir%".length()), "/");

                    VirtualFile relativeFile = VfsUtil.findRelativeFile(appDir, path.split("/"));
                    if(relativeFile != null) {
                        String relativePath = VfsUtil.getRelativePath(relativeFile, baseDir, '/');
                        if(relativePath != null) {
                            paths.add(Pair.create(namespace, relativePath));
                        }
                    }
                }
            } else if(namespacePath.startsWith("%kernel.project_dir%")) {
                // '%kernel.root_dir%/test'
                String path = StringUtils.stripStart(namespacePath.substring("%kernel.project_dir%".length()), "/");

                VirtualFile relativeFile = VfsUtil.findRelativeFile(baseDir, path.split("/"));
                if(relativeFile != null) {
                    String relativePath = VfsUtil.getRelativePath(relativeFile, baseDir, '/');
                    if(relativePath != null) {
                        paths.add(Pair.create(namespace, relativePath));
                    }
                }
            } else if(!namespacePath.startsWith("/") && !namespacePath.startsWith("\\")) {
                // 'test/foo'
                VirtualFile relativeFile = VfsUtil.findRelativeFile(baseDir, namespacePath.split("/"));
                if(relativeFile != null) {
                    String relativePath = VfsUtil.getRelativePath(relativeFile, baseDir, '/');
                    if(relativePath != null) {
                        paths.add(Pair.create(namespace, relativePath));
                    }
                }
            }
        }

         return paths;
    }

    /**
     * Collects Twig globals in given yaml configuration
     *
     * twig:
     *    globals:
     *       ga_tracking: '%ga_tracking%'
     *       user_management: '@AppBundle\Service\UserManagement'
     */
    @NotNull
    public static Map<String, String> getTwigGlobalsFromYamlConfig(@NotNull YAMLFile yamlFile) {
        YAMLKeyValue yamlKeyValue = YAMLUtil.getQualifiedKeyInFile(yamlFile, "twig", "globals");
        if(yamlKeyValue == null) {
            return Collections.emptyMap();
        }

        YAMLValue value = yamlKeyValue.getValue();
        if(!(value instanceof YAMLMapping)) {
            return Collections.emptyMap();
        }

        Map<String, String> pair = new HashMap<>();

        for (YAMLPsiElement element : value.getYAMLElements()) {
            if(!(element instanceof YAMLKeyValue)) {
                continue;
            }

            String keyText = ((YAMLKeyValue) element).getKeyText();
            if(StringUtils.isBlank(keyText)) {
                continue;
            }

            String valueText = ((YAMLKeyValue) element).getValueText();
            if(StringUtils.isBlank(valueText)) {
                continue;
            }

            pair.put(keyText, valueText);
        }

        return pair;
    }

    /**
     * {% trans with {'%name%': 'Fabien'} from "aa" %}
     */
    @Nullable
    public static String getDomainFromTranslationTag(@NotNull TwigCompositeElement twigCompositeElement) {
        // getChildren fix
        PsiElement firstChild = twigCompositeElement.getFirstChild();
        if(firstChild == null) {
            return null;
        }

        // with from identifier and get text value
        PsiElement childrenOfType = PsiElementUtils.getNextSiblingOfType(firstChild, TwigPattern.getTranslationTokenTagFromPattern());
        if(childrenOfType == null) {
            return null;
        }

        String text = childrenOfType.getText();
        if(StringUtils.isBlank(text)) {
            return null;
        }

        return text;
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
    private static TwigCreateContainer getOnCreateTemplateElements(@NotNull final Project project, @NotNull VirtualFile startDirectory) {

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
                        for (String s : getTwigExtendsTagTemplates((TwigExtendsTag) psiElement)) {
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
     * {% include "foo/#{segment.typeKey}.html.twig" with {'segment': segment} %}
     * {% include "foo/#{1 + 2}.html.twig" %}
     * {% include "foo/" ~ segment.typeKey ~ ".html.twig" %}
     */
    public static boolean isValidStringWithoutInterpolatedOrConcat(@NotNull PsiElement element) {
        String templateName = element.getText();

        if(fr.adrienbrault.idea.symfony2plugin.util.StringUtils.isInterpolatedString(templateName)) {
            return false;
        }

        return !(PlatformPatterns.psiElement()
            .afterLeafSkipping(
                TwigPattern.STRING_WRAP_PATTERN,
                PlatformPatterns.psiElement(TwigTokenTypes.CONCAT)
            ).accepts(element) ||
            PlatformPatterns.psiElement().beforeLeafSkipping(
                TwigPattern.STRING_WRAP_PATTERN,
                PlatformPatterns.psiElement(TwigTokenTypes.CONCAT)
            ).accepts(element));
    }

    @NotNull
    public static Collection<String> getCreateAbleTemplatePaths(@NotNull Project project, @NotNull String templateName) {
        templateName = normalizeTemplateName(templateName);

        Collection<String> paths = new HashSet<>();

        for (TwigPath twigPath : getTwigNamespaces(project)) {
            if(!twigPath.isEnabled()) {
                continue;
            }

            if(templateName.startsWith("@")) {
                int i = templateName.indexOf("/");
                if(i > 0 && templateName.substring(1, i).equals(twigPath.getNamespace())) {
                    paths.add(twigPath.getRelativePath(project) + "/" + templateName.substring(i + 1));
                }
            } else if(twigPath.getNamespaceType() == NamespaceType.BUNDLE && templateName.matches("^\\w+Bundle:.*")) {

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
     * Collects all files that extends a given files
     */
    @NotNull
    public static Collection<VirtualFile> getTemplatesExtendingFile(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        Set<VirtualFile> twigChild = new HashSet<>();
        getTemplatesExtendingFile(project, virtualFile, twigChild, 8);
        return twigChild;
    }

    private static void getTemplatesExtendingFile(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull Set<VirtualFile> twigChild, int depth) {
        if(depth <= 0) {
            return;
        }

        // use set here, we have multiple shortcut on one file, but only one is required
        HashSet<VirtualFile> virtualFiles = new LinkedHashSet<>();

        for(String templateName: getTemplateNamesForFile(project, virtualFile)) {
            // getFilesWithKey dont support keyset with > 1 items (bug?), so we cant merge calls
            FileBasedIndex.getInstance().getFilesWithKey(TwigExtendsStubIndex.KEY, new HashSet<>(Collections.singletonList(templateName)), myVirtualFile -> {
                virtualFiles.add(myVirtualFile);
                return true;
            }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE));
        }

        // finally resolve virtual file to twig files
        for(VirtualFile myVirtualFile: virtualFiles) {
            // stop if already inside
            if(twigChild.contains(myVirtualFile)) {
                continue;
            }

            twigChild.add(myVirtualFile);
            getTemplatesExtendingFile(project, myVirtualFile, twigChild, --depth);
        }
    }

    @NotNull
    public static Map<VirtualFile, Collection<String>> getBlockNamesForFiles(@NotNull Project project, @NotNull Collection<VirtualFile> virtualFiles) {
        Map<VirtualFile, Collection<String>> blocks = new HashMap<>();

        for (VirtualFile virtualFile : virtualFiles) {
            FileBasedIndex.getInstance().getValues(TwigBlockIndexExtension.KEY, "block", GlobalSearchScope.fileScope(project, virtualFile))
                .forEach(strings -> {
                    blocks.putIfAbsent(virtualFile, new HashSet<>());
                    blocks.get(virtualFile).addAll(strings);
                });
        }

        return blocks;
    }

    /**
     * Visit all possible Twig include file pattern
     */
    public static void visitTemplateIncludes(@NotNull TwigFile twigFile, @NotNull Consumer<TemplateInclude> consumer) {
        PsiTreeUtil.collectElements(twigFile, psiElement -> {
            if(psiElement instanceof TwigTagWithFileReference) {
                // {% include %}
                if(psiElement.getNode().getElementType() == TwigElementTypes.INCLUDE_TAG) {
                    for (String templateName : getIncludeTagStrings((TwigTagWithFileReference) psiElement)) {
                        if(StringUtils.isNotBlank(templateName)) {
                            consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.INCLUDE));
                        }
                    }
                }

                // {% import "foo.html.twig"
                PsiElement importTag = PsiElementUtils.getChildrenOfType(psiElement, TwigPattern.getTagNameParameterPattern(TwigElementTypes.IMPORT_TAG, "import"));
                if(importTag != null) {
                    String templateName = importTag.getText();
                    if(StringUtils.isNotBlank(templateName)) {
                        consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.IMPORT));
                    }
                }

                // {% from 'forms.html' import ... %}
                PsiElement fromTag = PsiElementUtils.getChildrenOfType(psiElement, TwigPattern.getTagNameParameterPattern(TwigElementTypes.IMPORT_TAG, "from"));
                if(fromTag != null) {
                    String templateName = fromTag.getText();
                    if(StringUtils.isNotBlank(templateName)) {
                        consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.IMPORT));
                    }
                }
            } else if(psiElement instanceof TwigCompositeElement) {
                // {{ include() }}
                // {{ source() }}
                PsiElement includeTag = PsiElementUtils.getChildrenOfType(psiElement, TwigPattern.getPrintBlockOrTagFunctionPattern("include", "source"));
                if(includeTag != null) {
                    String templateName = includeTag.getText();
                    if(StringUtils.isNotBlank(templateName)) {
                        consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.INCLUDE_FUNCTION));
                    }
                }

                // {% embed "foo.html.twig"
                PsiElement embedTag = PsiElementUtils.getChildrenOfType(psiElement, TwigPattern.getEmbedPattern());
                if(embedTag != null) {
                    String templateName = embedTag.getText();
                    if(StringUtils.isNotBlank(templateName)) {
                        consumer.consume(new TemplateInclude(psiElement, templateName, TemplateInclude.TYPE.EMBED));
                    }
                }

                if(psiElement.getNode().getElementType() == TwigElementTypes.TAG) {
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

                            // {% form_theme form.child with ['form/fields_child.html.twig'] %}
                            PsiElement withElement = PsiElementUtils.getNextSiblingOfType(tagElement, PlatformPatterns.psiElement().withElementType(TwigTokenTypes.IDENTIFIER).withText("with"));
                            if(withElement != null) {
                                // find LITERAL "[", "{"
                                PsiElement arrayStart = PsiElementUtils.getNextSiblingAndSkip(tagElement, TwigElementTypes.LITERAL,
                                    TwigTokenTypes.IDENTIFIER, TwigTokenTypes.SINGLE_QUOTE, TwigTokenTypes.DOUBLE_QUOTE, TwigTokenTypes.DOT
                                );

                                if(arrayStart != null) {
                                    PsiElement firstChild = arrayStart.getFirstChild();
                                    if(firstChild != null) {
                                        visitStringInArray(firstChild, pair ->
                                            consumer.consume(new TemplateInclude(psiElement, pair.getFirst(), TemplateInclude.TYPE.FORM_THEME))
                                        );
                                    }
                                }
                            }
                        }
                    }
                }

                for (TwigFileUsage extension : TWIG_FILE_USAGE_EXTENSIONS.getExtensions()) {
                    if (extension.isIncludeTemplate(psiElement)) {
                        for (String template : extension.getIncludeTemplate(psiElement)) {
                            consumer.consume(new TemplateInclude(psiElement, template, TemplateInclude.TYPE.INCLUDE));
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
    public static Collection<TwigMacroTagInterface> getMacros(@NotNull PsiFile file) {
        Collection<TwigMacroTagInterface> macros = new ArrayList<>();

        Collection<String> keys = new ArrayList<>();

        FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
        fileBasedIndex.processAllKeys(TwigMacroFunctionStubIndex.KEY, s -> {
            keys.add(s);
            return true;
        }, GlobalSearchScope.fileScope(file), null);

        for (String key : keys) {
            macros.addAll(fileBasedIndex.getValues(TwigMacroFunctionStubIndex.KEY, key, GlobalSearchScope.fileScope(file)));
        }

        return macros;
    }

    /**
     * Get all macros inside file
     *
     * {% macro foobar %}{% endmacro %}
     * {% macro input(name, value, type, size) %}{% endmacro %}
     */
    public static void visitMacros(@NotNull PsiFile file, Consumer<Pair<TwigMacroTag, PsiElement>> consumer) {
        PsiElement[] psiElements = PsiTreeUtil.collectElements(file, psiElement ->
            psiElement.getNode().getElementType() == TwigElementTypes.MACRO_TAG
        );

        for (PsiElement psiElement : psiElements) {
            Pair<String, String> macro = getTwigMacroNameAndParameter(psiElement);
            if(macro == null) {
                continue;
            }

            consumer.consume(Pair.create(new TwigMacroTag(macro.getFirst(), macro.getSecond()), psiElement));
        }
    }

    /**
     * Extract parameter from a Twig macro tag
     *
     * @param psiElement A MACRO_TAG node iElementType type
     * @return "(foo, foobar)"
     */
    @Nullable
    public static Pair<String, String> getTwigMacroNameAndParameter(@NotNull PsiElement psiElement) {
        PsiElement firstChild = psiElement.getFirstChild();
        if(firstChild == null) {
            return null;
        }

        PsiElement macroNamePsi = PsiElementUtils.getNextSiblingAndSkip(firstChild, TwigTokenTypes.IDENTIFIER, TwigTokenTypes.TAG_NAME);
        if(macroNamePsi == null) {
            return null;
        }

        String macroName = macroNamePsi.getText();

        String parameter = null;
        PsiElement nextSiblingAndSkip = PsiElementUtils.getNextSiblingAndSkip(macroNamePsi, TwigTokenTypes.LBRACE);
        if(nextSiblingAndSkip != null) {
            PsiElement nextSiblingOfType = PsiElementUtils
                .getNextSiblingOfType(nextSiblingAndSkip, PlatformPatterns.psiElement()
                    .withElementType(TwigTokenTypes.RBRACE));

            if(nextSiblingOfType != null) {
                parameter = psiElement.getContainingFile().getText().substring(
                    nextSiblingAndSkip.getTextOffset(),
                    nextSiblingOfType.getTextOffset() + 1
                );
            }
        }

        return Pair.create(macroName, parameter);
    }

    /**
     * Get all Twig extension implementations:

     * \Twig_ExtensionInterface
     * \Twig\Extension\ExtensionInterface
     */
    @NotNull
    public static Collection<PhpClass> getTwigExtensionClasses(@NotNull Project project) {
        // \Twig\Extension\ExtensionInterface
        Collection<PhpClass> allSubclasses = new HashSet<>();

        // Twig 1
        allSubclasses.addAll(PhpIndex.getInstance(project).getAllSubclasses("\\Twig_ExtensionInterface"));

        // Twig 2+
        allSubclasses.addAll(PhpIndex.getInstance(project).getAllSubclasses("\\Twig\\Extension\\ExtensionInterface"));

        // Filter units tests and use HashSet as interfaces are nested for BC
        return allSubclasses.stream()
            .filter(phpClass -> !PhpUnitUtil.isPhpUnitTestFile(phpClass.getContainingFile()))
            .collect(Collectors.toCollection(HashSet::new));
    }

    @Nullable
    public static PsiElement getInjectedTwigElement(@NotNull PsiFile psiFile, @NotNull Editor editor) {
        int caretOffset = editor.getCaretModel().getOffset();
        if(caretOffset < 0) {
            return null;
        }

        // last and first element of file are tricky :)
        if (caretOffset == 0) {
            return psiFile;
        }

        PsiElement psiElement = psiFile.findElementAt(caretOffset - 1);
        if(psiElement == null) {
            return null;
        }

        return TwigUtil.getElementOnTwigViewProvider(psiElement);
    }

    /**
     * Resolve html language injection
     */
    @Nullable
    public static PsiElement getElementOnTwigViewProvider(@NotNull PsiElement element) {
        PsiFile file = element.getContainingFile();
        TextRange textRange = element.getTextRange();
        return file.getViewProvider().findElementAt(textRange.getStartOffset(), TwigLanguage.INSTANCE);
    }

    /**
     * Collect appearance for Twig translation domains
     */
    @NotNull
    private static TreeMap<String, Integer> getPossibleDomainTreeMap(@NotNull PsiFile psiFile) {
        Map<String, Integer> found = new HashMap<>();

        // visit every trans or transchoice to get possible domain names
        PsiTreeUtil.collectElements(psiFile, psiElement -> {
            if (TwigPattern.getTransDomainPattern().accepts(psiElement)) {
                PsiElement psiElementTrans = PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")));
                if (psiElementTrans != null && getTwigMethodString(psiElementTrans) != null) {
                    String text = psiElement.getText();
                    if (StringUtils.isNotBlank(text)) {
                        if (found.containsKey(text)) {
                            found.put(text, found.get(text) + 1);
                        } else {
                            found.put(text, 1);
                        }
                    }
                }
            }

            return false;
        });

        // sort in found integer value
        TreeMap<String, Integer> sortedMap = new TreeMap<>(new ValueComparator(found));
        sortedMap.putAll(found);

        return sortedMap;
    }

    @NotNull
    public static DomainScope getTwigFileDomainScope(@NotNull PsiElement psiElement) {
        String defaultDomain = TwigUtil.getTransDefaultDomainOnScope(psiElement);
        if(defaultDomain == null) {
            defaultDomain = "messages";
        }

        TreeMap<String, Integer> sortedMap = getPossibleDomainTreeMap(psiElement.getContainingFile());

        // we want to have mostly used domain preselected
        String domain = defaultDomain;
        if(sortedMap.size() > 0) {
            domain = sortedMap.firstKey();
        }

        return new DomainScope(defaultDomain, domain);
    }

    /**
     * Visit Twig TokenParser
     * eg. {% my_token %}
     */
    public static void visitTokenParsers(@NotNull Project project, @NotNull Consumer<Triple<String, PsiElement, Method>> consumer) {
        Set<PhpClass> allSubclasses = new HashSet<>();

        PhpIndex phpIndex = PhpIndex.getInstance(project);

        allSubclasses.addAll(phpIndex.getAllSubclasses("\\Twig_TokenParserInterface"));
        allSubclasses.addAll(phpIndex.getAllSubclasses("\\Twig\\TokenParser\\TokenParserInterface"));

        for (PhpClass allSubclass : allSubclasses) {

            // we dont want to see test extension like "§"
            if(allSubclass.getName().endsWith("Test") || allSubclass.getContainingFile().getVirtualFile().getNameWithoutExtension().endsWith("Test")) {
                continue;
            }

            Method getTag = allSubclass.findMethodByName("getTag");
            if(getTag == null) {
                continue;
            }

            // get string return value
            PhpReturn childrenOfType = PsiTreeUtil.findChildOfType(getTag, PhpReturn.class);
            if(childrenOfType != null) {
                PhpPsiElement returnValue = childrenOfType.getFirstPsiChild();
                if(returnValue instanceof StringLiteralExpression) {
                    String contents = ((StringLiteralExpression) returnValue).getContents();
                    if(StringUtils.isNotBlank(contents)) {
                        consumer.consume(new Triple<>(contents, returnValue, getTag));
                    }
                }
            }
        }
    }

    /**
     * Visit all template variables which are completion in Twig file rendering call as array
     */
    public static void visitTemplateVariables(@NotNull TwigFile scope, @NotNull Consumer<Pair<String, PsiElement>> consumer) {
        visitTemplateVariables(scope, consumer, 3);
    }


    /**
     * Check if Twig pathes should be filled with the Bundle naming strategy "FooBar:Foo:Test"
     */
    public static boolean hasBundleNamespaceSupport(@NotNull Project project) {
        return Settings.getInstance(project).twigBundleNamespaceSupport;
    }

    /**
     * Proxy to consume every given scope
     */
    private static void visitTemplateVariables(@NotNull PsiElement scope, @NotNull Consumer<Pair<String, PsiElement>> consumer, int includeDepth) {
        for (PsiElement psiElement : scope.getChildren()) {
            IElementType elementType = psiElement.getNode().getElementType();

            if (elementType == TwigElementTypes.IF_STATEMENT || elementType == TwigElementTypes.SET_STATEMENT) {
                // {% if foobar
                // {% set foobar
                PsiElement firstChild = psiElement.getFirstChild();
                IElementType firstChildElementType = firstChild.getNode().getElementType();

                if (firstChildElementType == TwigElementTypes.IF_TAG || firstChildElementType == TwigElementTypes.SET_TAG) {
                    PsiElement nextSiblingOfType = PsiElementUtils.getNextSiblingOfType(
                        firstChild.getFirstChild(),
                        captureVariableOrField().afterLeafSkipping(
                            PlatformPatterns.psiElement(PsiWhiteSpace.class),
                            PlatformPatterns.psiElement(TwigTokenTypes.TAG_NAME)
                        )
                    );

                    // {% if foo and foobar
                    // {% if foo or foobar
                    if (firstChildElementType == TwigElementTypes.IF_TAG) {
                        PsiElement firstChild1 = firstChild.getFirstChild();
                        PsiElement nextSiblingOfType1 = PsiElementUtils.getNextSiblingOfType(
                            firstChild1,
                            captureVariableOrField().afterLeafSkipping(
                                PlatformPatterns.psiElement(PsiWhiteSpace.class),
                                PlatformPatterns.or(PlatformPatterns.psiElement(TwigTokenTypes.AND), PlatformPatterns.psiElement(TwigTokenTypes.OR))
                            )
                        );

                        visitTemplateVariablesConsumer(nextSiblingOfType1, consumer);
                    }

                    visitTemplateVariablesConsumer(nextSiblingOfType, consumer);
                }

                // nested "if" or "set" statement is supported
                visitTemplateVariables(psiElement, consumer, includeDepth);

            } else if (elementType == TwigElementTypes.PRINT_BLOCK) {
                // {{ foobar }}

                PsiElement nextSiblingOfType = PsiElementUtils.getNextSiblingOfType(
                    psiElement.getFirstChild(),
                    captureVariableOrField().afterLeaf(PlatformPatterns.or(
                        PlatformPatterns.psiElement(PsiWhiteSpace.class),
                        PlatformPatterns.psiElement(TwigTokenTypes.PRINT_BLOCK_START)
                    ))
                );

                // not match function {{ foobar() }}
                if(!PlatformPatterns.psiElement().beforeLeafSkipping(PlatformPatterns.psiElement(PsiWhiteSpace.class), PlatformPatterns.psiElement(TwigTokenTypes.LBRACE)).accepts(nextSiblingOfType)) {
                    visitTemplateVariablesConsumer(nextSiblingOfType, consumer);
                }
            } else if (elementType == TwigElementTypes.FOR_STATEMENT) {
                // {% for foo in bar %}{% endfor %}

                // we do not collect nested variables in this scope
                // because there can be new variables inside for statement itself

                PsiElement firstChild = psiElement.getFirstChild();
                if(firstChild.getNode().getElementType() == TwigElementTypes.FOR_TAG) {
                    PsiElement afterIn = PsiElementUtils.getNextSiblingOfType(
                        firstChild.getFirstChild(),
                        TwigPattern.getForTagInVariablePattern()
                    );

                    visitTemplateVariablesConsumer(afterIn, consumer);
                }
            } else if (psiElement instanceof PsiComment) {
                // {# @var foobar Class #}
                // {# @var Class foobar #}
                String text = psiElement.getText();

                if(StringUtils.isNotBlank(text)) {
                    for (Pattern pattern : TwigTypeResolveUtil.INLINE_DOC_REGEX) {
                        Matcher matcher = pattern.matcher(text);
                        while (matcher.find()) {
                            consumer.consume(Pair.create(matcher.group("var"), psiElement));
                        }
                    }
                }
            } else if (psiElement instanceof TwigTagWithFileReference && includeDepth-- >= 0) {
                // we dont support "with" and "context" modification for now
                // {% include 'foo.html.twig' only %}
                // {% include 'foo.html.twig' with {} %}

                if(PsiElementUtils.getChildrenOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("with", "only"))) == null) {
                    // collect includes unique and visit until given "includeDepth"

                    Set<TwigFile> files = getIncludeTagStrings((TwigTagWithFileReference) psiElement).stream()
                        .map(template -> getTemplatePsiElements(psiElement.getProject(), template))
                        .filter(psiFile -> psiFile instanceof TwigFile)
                        .map(psiFile -> (TwigFile) psiFile)
                        .collect(Collectors.toCollection(ArrayListSet::new));

                    for (TwigFile file : files) {
                        visitTemplateVariables(file, consumer, includeDepth);
                    }
                }
            } else if (elementType == TwigElementTypes.BLOCK_STATEMENT) {
                // visit block statement
                visitTemplateVariables(psiElement, consumer, includeDepth);
            }
        }
    }

    /**
     * Consume if not empty element provided
     */
    private static void visitTemplateVariablesConsumer(@Nullable PsiElement nextSiblingOfType, @NotNull Consumer<Pair<String, PsiElement>> consumer) {
        if (nextSiblingOfType != null) {
            String text = nextSiblingOfType.getText();
            if (StringUtils.isNotBlank(text)) {
                consumer.consume(Pair.create(text, nextSiblingOfType));
            }
        }
    }

    /**
     * Collect all parent Twig on "extends" tag
     */
    @NotNull
    public static Map<TwigFile, String> getExtendsTemplates(@NotNull TwigFile twigFile) {
        return getExtendsTemplates(twigFile, 15);
    }

    @NotNull
    private static Map<TwigFile, String> getExtendsTemplates(@NotNull TwigFile twigFile, int depth) {
        if (depth < 0) {
            return Collections.emptyMap();
        }

        Map<TwigFile, String> templates = new HashMap<>();

        for (TwigExtendsTag extendsTag : PsiTreeUtil.getChildrenOfTypeAsList(twigFile, TwigExtendsTag.class)) {
            for (String extendsTemplate : getTwigExtendsTagTemplates(extendsTag)) {
                String templateName = normalizeTemplateName(extendsTemplate);

                for (PsiFile psiFile : getTemplatePsiElements(twigFile.getProject(), templateName)) {
                    if (psiFile instanceof TwigFile && !templates.containsKey(psiFile)) {
                        templates.put((TwigFile) psiFile, templateName);
                        templates.putAll(getExtendsTemplates((TwigFile) psiFile, --depth));
                    }
                }
            }
        }

        return templates;
    }

    /**
     * Collect block in virtual files via index
     */
    @NotNull
    public static Collection<LookupElement> getBlockLookupElements(@NotNull Project project, @NotNull Collection<VirtualFile> files) {
        Collection<LookupElement> lookupElements = new ArrayList<>();

        Map<VirtualFile, Collection<String>> templateNames = new HashMap<>();

        for (Map.Entry<VirtualFile, Collection<String>> block : getBlockNamesForFiles(project, files).entrySet()) {
            // performance optimize to not resolve too many elements
            VirtualFile virtualFile = block.getKey();
            if(!templateNames.containsKey(virtualFile)) {
                templateNames.put(virtualFile, TwigUtil.getTemplateNamesForFile(project, virtualFile));
            }

            for (String blockName : block.getValue()) {
                LookupElementBuilder lookupElementBuilder = LookupElementBuilder
                    .create(blockName)
                    .withIcon(TwigIcons.TwigFileIcon);

                Collection<String> names = templateNames.getOrDefault(virtualFile, Collections.emptyList());
                if(names.size() > 0) {
                    lookupElementBuilder = lookupElementBuilder.withTypeText(names.iterator().next(), true);
                }

                lookupElements.add(lookupElementBuilder);
            }
        }

        return lookupElements;
    }

    public static void visitTemplateExtends(@NotNull TwigFile twigFile,@NotNull Consumer<Pair<String, PsiElement>> consumer) {
        twigFile.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof TwigExtendsTag) {
                    for (String s : TwigUtil.getTwigExtendsTagTemplates((TwigExtendsTag) element)) {
                        consumer.consume(Pair.create(TwigUtil.normalizeTemplateName(s), element));
                    }
                }

                for (TwigFileUsage extension : TWIG_FILE_USAGE_EXTENSIONS.getExtensions()) {
                    if (!extension.isExtendsTemplate(element)) {
                       continue;
                    }

                    for (String template : extension.getExtendsTemplate(element)) {
                        consumer.consume(Pair.create(TwigUtil.normalizeTemplateName(template), element));
                    }
                }

                super.visitElement(element);
            }
        });
    }

    /**
     * Visit each "controller()" with its normalized parameter
     */
    public static void visitControllerFunctions(@NotNull PsiFile psiFile,@NotNull Consumer<Pair<String, PsiElement>> consumer) {
        //
        PsiElement[] psiElements = PsiTreeUtil.collectElements(psiFile, psiElement -> {
            if (psiElement.getNode().getElementType() != TwigElementTypes.FUNCTION_CALL) {
                return false;
            }

            PsiElement firstChild = psiElement.getFirstChild();
            if (firstChild.getNode().getElementType() != TwigTokenTypes.IDENTIFIER) {
                return false;
            }

            return "controller".equalsIgnoreCase(firstChild.getText());
        });

        // find parameter: controller("foobar::action")
        for (PsiElement functionCall: psiElements) {
            PsiElement includeTag = PsiElementUtils.getChildrenOfType(functionCall, TwigPattern.getPrintBlockOrTagFunctionPattern("controller"));
            if(includeTag != null) {
                String controllerName = includeTag.getText();
                if(StringUtils.isNotBlank(controllerName)) {
                    // escaping
                    // "Foobar\\Test"
                    String replace = StringUtils.stripStart(controllerName.replace("\\\\", "\\"), "\\");
                    consumer.consume(Pair.create(replace, includeTag));
                }
            }
        }
    }

    public static class DomainScope {
        @NotNull
        private final String defaultDomain;

        @NotNull
        private final String domain;

        DomainScope(@NotNull String defaultDomain, @NotNull String domain) {
            this.defaultDomain = defaultDomain;
            this.domain = domain;
        }

        /**
         * trans_default_domain for scope
         */
        @NotNull
        public String getDefaultDomain() {
            return defaultDomain;
        }

        /**
         * Domain with most file scope appearance
         */
        @NotNull
        public String getDomain() {
            return domain;
        }
    }

    private static class MyAllTemplateFileMapCachedValueProvider implements CachedValueProvider<Map<String, Set<VirtualFile>>> {
        @NotNull
        private final Project project;

        private final boolean includePhpFiles;

        MyAllTemplateFileMapCachedValueProvider(@NotNull Project project) {
            this(project, false);
        }

        MyAllTemplateFileMapCachedValueProvider(@NotNull Project project, boolean includePhpFiles) {
            this.project = project;
            this.includePhpFiles = includePhpFiles;
        }

        @Nullable
        @Override
        public Result<Map<String, Set<VirtualFile>>> compute() {
            return Result.create(getTemplateMapProxy(project, includePhpFiles), PsiModificationTracker.MODIFICATION_COUNT);
        }
    }

    /**
     * Twig template visitor, which scan given TwigPath for template names
     *
     * "@Foo/foo.html.twig"
     * "foo.html.twig"
     * "FooBundle:foo:foo.html.twig"
     */
    private static class MyLimitedVirtualFileVisitor extends VirtualFileVisitor {
        @NotNull
        private final TwigPath twigPath;

        @NotNull
        private final Project project;

        @NotNull
        private Map<String, VirtualFile> results = new HashMap<>();

        final private boolean withPhp;

        private int childrenAllowToVisit;

        @NotNull
        private Set<String> workedOn = new HashSet<>();

        private MyLimitedVirtualFileVisitor(@NotNull Project project, @NotNull TwigPath twigPath, boolean withPhp, int maxDepth, int maxDirs) {
            super(VirtualFileVisitor.limit(maxDepth));

            this.project = project;
            this.twigPath = twigPath;
            this.withPhp = withPhp;
            this.childrenAllowToVisit = maxDirs;
        }

        @Override
        public boolean visitFile(@NotNull VirtualFile virtualFile) {
            // per path directory limit
            if (virtualFile.isDirectory() && childrenAllowToVisit-- <= 0) {
                return false;
            }

            processFile(virtualFile);

            return super.visitFile(virtualFile);
        }

        private void processFile(VirtualFile virtualFile) {
            // @TODO make file types more dynamically like eg js
            if(!this.isProcessable(virtualFile)) {
                return;
            }

            // prevent process double file if processCollection and ContentIterator is used in one instance
            String filePath = virtualFile.getPath();
            if(workedOn.contains(filePath)) {
                return;
            }

            workedOn.add(filePath);

            String templateName = getTemplateNameForTwigPath(project, twigPath, virtualFile);
            if(templateName != null) {
                results.put(templateName, virtualFile);
            }
        }

        private boolean isProcessable(VirtualFile virtualFile) {
            if(virtualFile.isDirectory()) {
                return false;
            }

            if(virtualFile.getFileType() instanceof TwigFileType) {
                return true;
            }

            if(withPhp && virtualFile.getFileType() instanceof PhpFileType) {
                return true;
            }

            return false;
        }

        @NotNull
        private Map<String, VirtualFile> getResults() {
            return results;
        }

        @NotNull
        static Map<String, VirtualFile> createResult(@NotNull VirtualFile virtualFile, @NotNull Project project, @NotNull TwigPath twigPath, boolean withPhp) {
            MyLimitedVirtualFileVisitor visitor = new MyLimitedVirtualFileVisitor(project, twigPath, withPhp, 5, 150);
            VfsUtil.visitChildrenRecursively(virtualFile, visitor);
            return visitor.getResults();
        }
    }
}
