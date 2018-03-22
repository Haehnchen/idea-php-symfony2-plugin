package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigBlockTag;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.variable.collector.ControllerDocVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int offset, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !PlatformPatterns.psiElement().withLanguage(TwigLanguage.INSTANCE).accepts(psiElement)) {
            return null;
        }

        Collection<PsiElement> targets = new ArrayList<>();

        if (TwigPattern.getBlockTagPattern().accepts(psiElement)) {
            targets.addAll(TwigBlockUtil.getBlockOverwriteTargets(psiElement));
        }

        if (TwigPattern.getPathAfterLeafPattern().accepts(psiElement)) {
            targets.addAll(getRouteParameterGoTo(psiElement));
        }

        if(TwigPattern.getTemplateFileReferenceTagPattern().accepts(psiElement) || TwigPattern.getPrintBlockOrTagFunctionPattern("include", "source").accepts(psiElement)) {
            // support: {% include() %}, {{ include() }}
            targets.addAll(getTwigFiles(psiElement, offset));
        } else if (PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT).withText(PlatformPatterns.string().endsWith(".twig")).accepts(psiElement)) {
            // provide global twig file resolving
            // just if we dont match against known file references pattern
            targets.addAll(getTwigFiles(psiElement, offset));
        }

        if(TwigPattern.getAutocompletableRoutePattern().accepts(psiElement)) {
            targets.addAll(getRouteGoTo(psiElement));
        }

        // find trans('', {}, '|')
        // tricky way to get the function string trans(...)
        if (TwigPattern.getTransDomainPattern().accepts(psiElement)) {
            PsiElement psiElementTrans = PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")));
            if(psiElementTrans != null && TwigUtil.getTwigMethodString(psiElementTrans) != null) {
                targets.addAll(getTranslationDomainGoto(psiElement));
            }
        }

        // {% trans from "app" %}
        // {% transchoice from "app" %}
        if (TwigPattern.getTranslationTokenTagFromPattern().accepts(psiElement)) {
            targets.addAll(getTranslationDomainGoto(psiElement));
        }

        if (TwigPattern.getTranslationKeyPattern("trans", "transchoice").accepts(psiElement)) {
            targets.addAll(getTranslationKeyGoTo(psiElement));
        }

        if(TwigPattern.getPrintBlockOrTagFunctionPattern("controller").accepts(psiElement) || TwigPattern.getStringAfterTagNamePattern("render").accepts(psiElement)) {
            targets.addAll(getControllerGoTo(psiElement));
        }

        if(TwigPattern.getTransDefaultDomainPattern().accepts(psiElement)) {
            targets.addAll(TranslationUtil.getDomainPsiFiles(psiElement.getProject(), psiElement.getText()));
        }

        if(TwigPattern.getFilterPattern().accepts(psiElement)) {
            targets.addAll(getFilterGoTo(psiElement));
        }

        // {% if foo is ... %}
        // {% if foo is not ... %}
        if(PlatformPatterns.or(TwigPattern.getAfterIsTokenPattern(), TwigPattern.getAfterIsTokenWithOneIdentifierLeafPattern()).accepts(psiElement)) {
            targets.addAll(getAfterIsToken(psiElement));
        }

        // {{ goto_me() }}
        if (TwigPattern.getPrintBlockFunctionPattern().accepts(psiElement)) {
            targets.addAll(this.getMacros(psiElement));
        }

        // {% from 'boo.html.twig' import goto_me %}
        if (TwigPattern.getTemplateImportFileReferenceTagPattern().accepts(psiElement)) {
            targets.addAll(this.getMacros(psiElement));
        }

        // {% set foo  %}
        // {% set foo = bar %}
        if (PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK)
            ).withLanguage(TwigLanguage.INSTANCE).accepts(psiElement)) {

            targets.addAll(getSets(psiElement));
        }

        // {{ function( }}
        // {{ function }}
        if (PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withParent(PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK),
                PlatformPatterns.psiElement(TwigElementTypes.SET_TAG),

                PlatformPatterns.psiElement(TwigElementTypes.FUNCTION_CALL)
            )).withLanguage(TwigLanguage.INSTANCE).accepts(psiElement)) {

            targets.addAll(this.getFunctions(psiElement));
        }

        // {{ foo.fo<caret>o }}
        if(TwigPattern.getTypeCompletionPattern().accepts(psiElement)
            || TwigPattern.getPrintBlockFunctionPattern().accepts(psiElement)
            || TwigPattern.getVariableTypePattern().accepts(psiElement))
        {
            targets.addAll(getTypeGoto(psiElement));
        }

        if(TwigPattern.getTwigDocBlockMatchPattern(ControllerDocVariableCollector.DOC_PATTERN).accepts(psiElement)) {
            targets.addAll(getControllerNameGoto(psiElement));
        }

        // {{ parent() }}
        if(TwigPattern.getParentFunctionPattern().accepts(psiElement)) {
            targets.addAll(getParentGoto(psiElement));
        }

        // constant('Post::PUBLISHED')
        if(TwigPattern.getPrintBlockOrTagFunctionPattern("constant").accepts(psiElement)) {
            targets.addAll(getConstantGoto(psiElement));
        }

        // {# @var user \Foo #}
        if(TwigPattern.getTwigTypeDocBlockPattern().accepts(psiElement)) {
            targets.addAll(getVarClassGoto(psiElement));
        }

        // {# @see Foo.html.twig #}
        // {# @see \Class #}
        if(TwigPattern.getTwigDocSeePattern().accepts(psiElement)) {
            targets.addAll(getSeeDocTagTargets(psiElement));
        }

        // {% FOO_TOKEN %}
        if(TwigPattern.getTagTokenBlockPattern().accepts(psiElement)) {
            targets.addAll(getTokenTargets(psiElement));
        }

        return targets.toArray(new PsiElement[targets.size()]);
    }

    /**
     * {% if foo is ... %}
     */
    @NotNull
    private Collection<PsiElement> getAfterIsToken(@NotNull PsiElement psiElement) {
        // find text after if statement
        String text = StringUtils.trim(
            PhpElementsUtil.getPrevSiblingAsTextUntil(psiElement, TwigPattern.getAfterIsTokenTextPattern(), false) + psiElement.getText()
        );

        if(StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        Set<String> items = new HashSet<>(
            Collections.singletonList(text)
        );

        // support atleat one identifier after current caret position
        // "divisi<caret>ble by"
        PsiElement whitespace = psiElement.getNextSibling();
        if(whitespace instanceof PsiWhiteSpace) {
            PsiElement nextSibling = whitespace.getNextSibling();
            if(nextSibling != null && nextSibling.getNode().getElementType() == TwigTokenTypes.IDENTIFIER) {
                String identifier = nextSibling.getText();
                if(StringUtils.isNotBlank(identifier)) {
                    items.add(text + " " + identifier);
                }
            }
        }

        Collection<PsiElement> psiElements = new ArrayList<>();

        for (Map.Entry<String, TwigExtension> entry : new TwigExtensionParser(psiElement.getProject()).getSimpleTest().entrySet()) {
            for (String item : items) {
                if(entry.getKey().equalsIgnoreCase(item)) {
                    psiElements.addAll(Arrays.asList(
                        PhpElementsUtil.getPsiElementsBySignature(psiElement.getProject(), entry.getValue().getSignature()))
                    );
                }
            }
        }

        return psiElements;
    }

    @NotNull
    private Collection<PsiElement> getRouteParameterGoTo(@NotNull PsiElement psiElement) {
        String routeName = TwigUtil.getMatchingRouteNameOnParameter(psiElement);

        if(routeName == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(
            RouteHelper.getRouteParameterPsiElements(psiElement.getProject(), routeName, psiElement.getText())
        );
    }

    @NotNull
    private Collection<PsiElement> getControllerGoTo(@NotNull  PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        return Arrays.asList(RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), text));
    }

    @NotNull
    private Collection<PsiElement> getTwigFiles(@NotNull PsiElement psiElement, int offset) {
        int calulatedOffset = offset - psiElement.getTextRange().getStartOffset();
        if (calulatedOffset < 0) {
            calulatedOffset = 0;
        }

        return TwigUtil.getTemplateNavigationOnOffset(
            psiElement.getProject(),
            psiElement.getText(),
            calulatedOffset
        );
    }

    @NotNull
    private Collection<PsiElement> getFilterGoTo(@NotNull  PsiElement psiElement) {
        Map<String, TwigExtension> filters = new TwigExtensionParser(psiElement.getProject()).getFilters();

        if(!filters.containsKey(psiElement.getText())) {
            return Collections.emptyList();
        }

        String signature = filters.get(psiElement.getText()).getSignature();
        if(signature == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(PhpElementsUtil.getPsiElementsBySignature(psiElement.getProject(), signature));
    }

    @NotNull
    private Collection<PsiElement> getRouteGoTo(@NotNull PsiElement psiElement) {
        String text = PsiElementUtils.getText(psiElement);

        if(StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        PsiElement[] methods = RouteHelper.getMethods(psiElement.getProject(), text);
        if(methods.length > 0) {
            return Arrays.asList(methods);
        }

        return RouteHelper.getRouteDefinitionTargets(psiElement.getProject(), text);
    }

    @NotNull
    private Collection<PsiElement> getTranslationKeyGoTo(@NotNull PsiElement psiElement) {
        String translationKey = psiElement.getText();
        return Arrays.asList(
            TranslationUtil.getTranslationPsiElements(psiElement.getProject(), translationKey, TwigUtil.getPsiElementTranslationDomain(psiElement))
        );
    }

    @NotNull
    private Collection<PsiElement> getTranslationDomainGoto(@NotNull PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());

        if(StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        return new ArrayList<>(TranslationUtil.getDomainPsiFiles(psiElement.getProject(), text));
    }

    @NotNull
    private Collection<PsiElement> getConstantGoto(@NotNull PsiElement psiElement) {
        Collection<PsiElement> targetPsiElements = new ArrayList<>();

        String contents = psiElement.getText();
        if(StringUtils.isBlank(contents)) {
            return targetPsiElements;
        }

        // global constant
        if(!contents.contains(":")) {
            targetPsiElements.addAll(PhpIndex.getInstance(psiElement.getProject()).getConstantsByName(contents));
            return targetPsiElements;
        }

        // resolve class constants
        String[] parts = contents.split("::");
        if(parts.length != 2) {
            return targetPsiElements;
        }

        PhpClass phpClass = PhpElementsUtil.getClassInterface(psiElement.getProject(), parts[0].replace("\\\\", "\\"));
        if(phpClass == null) {
            return targetPsiElements;
        }

        Field field = phpClass.findFieldByName(parts[1], true);
        if(field != null) {
            targetPsiElements.add(field);
        }

        return targetPsiElements;
    }

    /**
     * Extract class from inline variables
     *
     * {# @var \AppBundle\Entity\Foo variable #}
     * {# @var variable \AppBundle\Entity\Foo #}
     */
    @NotNull
    private Collection<PhpClass> getVarClassGoto(@NotNull PsiElement psiElement) {
        String comment = psiElement.getText();

        if(StringUtils.isBlank(comment)) {
            return Collections.emptyList();
        }

        for(String pattern: TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE) {
            Matcher matcher = Pattern.compile(pattern).matcher(comment);
            if (matcher.find()) {
                String className = matcher.group("class");
                if(StringUtils.isNotBlank(className)) {
                    return PhpElementsUtil.getClassesInterface(psiElement.getProject(), className);
                }
            }
        }

        return Collections.emptyList();
    }

    @NotNull
    private Collection<PsiElement> getSeeDocTagTargets(@NotNull PsiElement psiElement) {
        String comment = psiElement.getText();
        if(StringUtils.isBlank(comment)) {
            return Collections.emptyList();
        }

        Collection<PsiElement> psiElements = new ArrayList<>();

        for(String pattern: new String[] {TwigPattern.DOC_SEE_REGEX, TwigUtil.DOC_SEE_REGEX_WITHOUT_SEE}) {
            Matcher matcher = Pattern.compile(pattern).matcher(comment);
            if (!matcher.find()) {
                continue;
            }

            String content = matcher.group(1);

            if(content.toLowerCase().endsWith(".twig")) {
                psiElements.addAll(TwigUtil.getTemplatePsiElements(psiElement.getProject(), content));
            }

            psiElements.addAll(PhpElementsUtil.getClassesInterface(psiElement.getProject(), content));
            ContainerUtil.addAll(psiElements, RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), content));

            PsiDirectory parent = psiElement.getContainingFile().getParent();
            if(parent != null) {
                VirtualFile relativeFile = VfsUtil.findRelativeFile(parent.getVirtualFile(), content.replace("\\", "/").split("/"));
                if(relativeFile != null) {
                    ContainerUtil.addIfNotNull(psiElements, PsiManager.getInstance(psiElement.getProject()).findFile(relativeFile));
                }
            }

            Matcher methodMatcher = Pattern.compile("([\\w\\\\-]+):+([\\w_\\-]+)").matcher(content);
            if (methodMatcher.find()) {
                for (PhpClass phpClass : PhpIndex.getInstance(psiElement.getProject()).getAnyByFQN(methodMatcher.group(1))) {
                    ContainerUtil.addIfNotNull(psiElements, phpClass.findMethodByName(methodMatcher.group(2)));
                }
            }
        }

        return psiElements;
    }

    @NotNull
    private Collection<PsiElement> getTypeGoto(@NotNull PsiElement psiElement) {
        Collection<PsiElement> targetPsiElements = new ArrayList<>();

        // class, class.method, class.method.method
        // click on first item is our class name
        Collection<String> beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(psiElement);
        if(beforeLeaf.size() == 0) {
            Collection<TwigTypeContainer> twigTypeContainers = TwigTypeResolveUtil.resolveTwigMethodName(psiElement, TwigTypeResolveUtil.formatPsiTypeNameWithCurrent(psiElement));
            for(TwigTypeContainer twigTypeContainer: twigTypeContainers) {
                if(twigTypeContainer.getPhpNamedElement() != null) {
                    targetPsiElements.add(twigTypeContainer.getPhpNamedElement());
                }
            }

        } else {
            Collection<TwigTypeContainer> types = TwigTypeResolveUtil.resolveTwigMethodName(psiElement, beforeLeaf);
            String text = psiElement.getText();
            if(StringUtils.isNotBlank(text)) {
                // provide method / field goto
                for(TwigTypeContainer twigTypeContainer: types) {
                    if(twigTypeContainer.getPhpNamedElement() != null) {
                        targetPsiElements.addAll(TwigTypeResolveUtil.getTwigPhpNameTargets(twigTypeContainer.getPhpNamedElement(), text));
                    }
                }
            }
        }

        return targetPsiElements;
    }

    @NotNull
    private Collection<PsiElement> getFunctions(@NotNull PsiElement psiElement) {
        Map<String, TwigExtension> functions = new TwigExtensionParser(psiElement.getProject()).getFunctions();

        String funcName = psiElement.getText();
        if(!functions.containsKey(funcName)) {
            return Collections.emptyList();
        }

        return Arrays.asList(PhpElementsUtil.getPsiElementsBySignature(psiElement.getProject(), functions.get(funcName).getSignature()));
    }

    @NotNull
    private Collection<PsiElement> getSets(@NotNull PsiElement psiElement) {
        String funcName = psiElement.getText();
        for(String twigSet: TwigUtil.getSetDeclaration(psiElement.getContainingFile())) {
            if(twigSet.equals(funcName)) {
                // @TODO: drop regex
                return Arrays.asList(PsiTreeUtil.collectElements(psiElement.getContainingFile(), psiElement1 ->
                    PlatformPatterns.psiElement(TwigTagWithFileReference.class)
                    .accepts(psiElement1) && psiElement1.getText()
                    .matches("\\{%\\s?set\\s?" + Pattern.quote(funcName) + "\\s?.*"))
                );
            }
        }

        return Collections.emptyList();
    }

    @NotNull
    private Collection<PsiElement> getMacros(@NotNull PsiElement psiElement) {
        String funcName = psiElement.getText();

        // check for complete file as namespace import {% import "file" as foo %}
        // {% import _self as foobar %}
        // {{ foobar.bar }}
        PsiElement prevSibling = psiElement.getPrevSibling();
        if(prevSibling != null && prevSibling.getNode().getElementType() == TwigTokenTypes.DOT) {
            PsiElement identifier = prevSibling.getPrevSibling();
            if(identifier == null || identifier.getNode().getElementType() != TwigTokenTypes.IDENTIFIER) {
                return Collections.emptyList();
            }

            return TwigUtil.getImportedMacrosNamespaces(
                psiElement.getContainingFile(),
                identifier.getText() + "." + funcName
            );
        }

        // {% from _self import foobar as input, foobar %}
        return TwigUtil.getImportedMacros(psiElement.getContainingFile(), funcName);
    }

    @NotNull
    private Collection<PsiElement> getControllerNameGoto(@NotNull PsiElement psiElement) {
        Pattern pattern = Pattern.compile(ControllerDocVariableCollector.DOC_PATTERN);

        Matcher matcher = pattern.matcher(psiElement.getText());
        if (!matcher.find()) {
            return Collections.emptyList();
        }

        String controllerName = matcher.group(1);

        return Arrays.asList(RouteHelper.getMethodsOnControllerShortcut(psiElement.getProject(), controllerName));
    }

    @NotNull
    private Collection<PsiElement> getParentGoto(@NotNull PsiElement psiElement) {
        // find printblock
        PsiElement printBlock = psiElement.getParent();
        if(printBlock == null || !PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK).accepts(printBlock)) {
            return Collections.emptyList();
        }

        // printblock need to be child block statement
        PsiElement blockStatement = printBlock.getParent();
        if(blockStatement == null || !PlatformPatterns.psiElement(TwigElementTypes.BLOCK_STATEMENT).accepts(blockStatement)) {
            return Collections.emptyList();
        }

        // BlockTag is first child of block statement
        PsiElement blockTag = blockStatement.getFirstChild();
        if(!(blockTag instanceof TwigBlockTag)) {
            return Collections.emptyList();
        }

        String blockName = ((TwigBlockTag) blockTag).getName();
        if(blockName == null) {
            return Collections.emptyList();
        }

        return TwigBlockUtil.getBlockOverwriteTargets(psiElement.getContainingFile(), blockName, false);
    }

    @NotNull
    private Collection<? extends PsiElement> getTokenTargets(@NotNull PsiElement psiElement) {
        String tagName = psiElement.getText();
        if(StringUtils.isBlank(tagName)) {
            return Collections.emptyList();
        }

        Collection<PsiElement> targets = new ArrayList<>();

        TwigUtil.visitTokenParsers(psiElement.getProject(), pair -> {
            if(tagName.equalsIgnoreCase(pair.getFirst())) {
                targets.add(pair.getSecond());
            }
        });

        return targets;
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
