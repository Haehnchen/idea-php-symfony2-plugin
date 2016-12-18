package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Field;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import com.jetbrains.twig.elements.TwigBlockTag;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMacro;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigSet;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.collector.ControllerDocVariableCollector;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.RegexPsiElementFilter;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigTemplateGoToLocalDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        List<PsiElement> psiElements = new ArrayList<>();

        // {{ goto_me() }}
        if (TwigHelper.getPrintBlockFunctionPattern().accepts(psiElement)) {
            psiElements.addAll(Arrays.asList(this.getMacros(psiElement)));
        }

        // {% from 'boo.html.twig' import goto_me %}
        if (TwigHelper.getTemplateImportFileReferenceTagPattern().accepts(psiElement)) {
            psiElements.addAll(Arrays.asList(this.getMacros(psiElement)));
        }

        // {% set foo  %}
        // {% set foo = bar %}
        if (PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK)
            ).withLanguage(TwigLanguage.INSTANCE).accepts(psiElement)) {

            psiElements.addAll(Arrays.asList(this.getSets(psiElement)));
        }

        // {{ function( }}
        if (PlatformPatterns
            .psiElement(TwigTokenTypes.IDENTIFIER)
            .beforeLeaf(PlatformPatterns.psiElement(TwigTokenTypes.LBRACE))
            .withParent(PlatformPatterns.or(
                PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK),
                PlatformPatterns.psiElement(TwigElementTypes.SET_TAG)
            )).withLanguage(TwigLanguage.INSTANCE).accepts(psiElement)) {

            psiElements.addAll(Arrays.asList(this.getFunctions(psiElement)));
        }

        /**
         * {{ foo.fo<caret>o }}
         */
        if(TwigHelper.getTypeCompletionPattern().accepts(psiElement)
            || TwigHelper.getPrintBlockFunctionPattern().accepts(psiElement)
            || TwigHelper.getVariableTypePattern().accepts(psiElement))
        {
            psiElements.addAll(Arrays.asList(this.getTypeGoto(psiElement)));
        }

        if(TwigHelper.getTwigDocBlockMatchPattern(ControllerDocVariableCollector.DOC_PATTERN).accepts(psiElement)) {
            psiElements.addAll(Arrays.asList(this.getControllerNameGoto(psiElement)));
        }

        // {{ parent() }}
        if(TwigHelper.getParentFunctionPattern().accepts(psiElement)) {
            psiElements.addAll(Arrays.asList(this.getParentGoto(psiElement)));
        }

        // constant('Post::PUBLISHED')
        if(TwigHelper.getPrintBlockOrTagFunctionPattern("constant").accepts(psiElement)) {
            psiElements.addAll(this.getConstantGoto(psiElement));
        }

        // {# @var user \Foo #}
        if(TwigHelper.getTwigTypeDocBlock().accepts(psiElement)) {
            psiElements.addAll(this.getVarClassGoto(psiElement));
        }

        // {# @see Foo.html.twig #}
        // {# @see \Class #}
        if(TwigHelper.getTwigDocSeePattern().accepts(psiElement)) {
            psiElements.addAll(this.getSeeDocTagTargets(psiElement));
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    private Collection<PsiElement> getConstantGoto(PsiElement psiElement) {

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

    private Collection<PhpClass> getVarClassGoto(PsiElement psiElement) {

        String comment = psiElement.getText();
        if(StringUtils.isBlank(comment)) {
            return Collections.emptyList();
        }

        for(String pattern: new String[] {TwigTypeResolveUtil.DEPRECATED_DOC_TYPE_PATTERN, TwigTypeResolveUtil.DOC_TYPE_PATTERN_SINGLE}) {
            Matcher matcher = Pattern.compile(pattern).matcher(comment);
            if (matcher.find()) {
                String className = matcher.group(2);
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

        for(String pattern: new String[] {TwigHelper.DOC_SEE_REGEX, TwigHelper.DOC_SEE_REGEX_WITHOUT_SEE}) {
            Matcher matcher = Pattern.compile(pattern).matcher(comment);
            if (!matcher.find()) {
                continue;
            }

            String content = matcher.group(1);

            if(content.toLowerCase().endsWith(".twig")) {
                ContainerUtil.addAll(psiElements, TwigHelper.getTemplatePsiElements(psiElement.getProject(), content));
            }

            psiElements.addAll(PhpElementsUtil.getClassesInterface(psiElement.getProject(), content));
            ContainerUtil.addIfNotNull(psiElements, ControllerIndex.getControllerMethod(psiElement.getProject(), content));

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

    private PsiElement[] getTypeGoto(PsiElement psiElement) {

        List<PsiElement> targetPsiElements = new ArrayList<>();

        // class, class.method, class.method.method
        // click on first item is our class name
        String[] beforeLeaf = TwigTypeResolveUtil.formatPsiTypeName(psiElement);
        if(beforeLeaf.length == 0) {
            Collection<TwigTypeContainer> twigTypeContainers = TwigTypeResolveUtil.resolveTwigMethodName(psiElement, TwigTypeResolveUtil.formatPsiTypeName(psiElement, true));
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

        return targetPsiElements.toArray(new PsiElement[targetPsiElements.size()]);
    }

    private PsiElement[] getFunctions(PsiElement psiElement) {
        Map<String, TwigExtension> functions = new TwigExtensionParser(psiElement.getProject()).getFunctions();

        String funcName = psiElement.getText();
        if(!functions.containsKey(funcName)) {
            return new PsiElement[0];
        }

        return PhpElementsUtil.getPsiElementsBySignature(psiElement.getProject(), functions.get(funcName).getSignature());
     }

    private PsiElement[] getSets(PsiElement psiElement) {
        String funcName = psiElement.getText();
        for(TwigSet twigSet: TwigUtil.getSetDeclaration(psiElement.getContainingFile())) {
           if(twigSet.getName().equals(funcName)) {
               return PsiTreeUtil.collectElements(psiElement.getContainingFile(), new RegexPsiElementFilter(
                   TwigTagWithFileReference.class,
                   "\\{%\\s?set\\s?" + Pattern.quote(funcName) + "\\s?.*")
               );
           }
        }

        return new PsiElement[0];
    }

    private PsiElement[] getMacros(@NotNull PsiElement psiElement) {
        String funcName = psiElement.getText();
        String funcNameSearch = funcName;

        List<TwigMacro> twigMacros;

        // check for complete file as namespace import {% import "file" as foo %}
        if(psiElement.getPrevSibling() != null && PlatformPatterns.psiElement(TwigTokenTypes.DOT).accepts(psiElement.getPrevSibling())) {
            PsiElement psiElement1 = psiElement.getPrevSibling().getPrevSibling();
            if(psiElement1 == null) {
                return null;
            }

            funcNameSearch = psiElement1.getText() + "." + funcName;
            twigMacros = TwigUtil.getImportedMacrosNamespaces(psiElement.getContainingFile());
        } else {
            twigMacros = TwigUtil.getImportedMacros(psiElement.getContainingFile());
        }

        for(TwigMacro twigMacro : twigMacros) {
            if(twigMacro.getName().equals(funcNameSearch)) {

                // switch to alias mode
                final String macroName = twigMacro.getOriginalName() == null ? funcName : twigMacro.getOriginalName();

                PsiFile psiFile;
                if("_self".equals(twigMacro.getTemplate())) {
                    psiFile = psiElement.getContainingFile();
                } else {
                    psiFile = TwigHelper.getTemplateFileByName(psiElement.getProject(), twigMacro.getTemplate());
                }

                if(psiFile != null) {
                    return PsiTreeUtil.collectElements(psiFile, new RegexPsiElementFilter(
                        TwigElementTypes.MACRO_TAG,
                        "\\{%\\s?macro\\s?" + Pattern.quote(macroName) + "\\s?\\(.*%}")
                    );
                }

            }
        }

        return new PsiElement[0];
    }


    private PsiElement[] getControllerNameGoto(PsiElement psiElement) {
        Pattern pattern = Pattern.compile(ControllerDocVariableCollector.DOC_PATTERN);
        Matcher matcher = pattern.matcher(psiElement.getText());
        if (!matcher.find()) {
            return new PsiElement[0];
        }

        String controllerName = matcher.group(1);

        Method method = ControllerIndex.getControllerMethod(psiElement.getProject(), controllerName);
        if(method == null) {
            return new PsiElement[0];
        }

        return new PsiElement[] { method };
    }

    private PsiElement[] getParentGoto(PsiElement psiElement) {

        // find printblock
        PsiElement printBlock = psiElement.getParent();
        if(printBlock == null || !PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK).accepts(printBlock)) {
            return new PsiElement[0];
        }

        // printblock need to be child block statement
        PsiElement blockStatement = printBlock.getParent();
        if(blockStatement == null || !PlatformPatterns.psiElement(TwigElementTypes.BLOCK_STATEMENT).accepts(blockStatement)) {
            return new PsiElement[0];
        }

        // BlockTag is first child of block statement
        PsiElement blockTag = blockStatement.getFirstChild();
        if(!(blockTag instanceof TwigBlockTag)) {
            return new PsiElement[0];
        }

        String blockName = ((TwigBlockTag) blockTag).getName();
        return TwigTemplateGoToDeclarationHandler.getBlockNameGoTo(psiElement.getContainingFile(), blockName);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
