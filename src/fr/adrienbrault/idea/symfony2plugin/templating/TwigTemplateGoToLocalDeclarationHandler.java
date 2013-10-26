package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.twig.*;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigMacro;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigSet;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.RegexPsiElementFilter;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public class TwigTemplateGoToLocalDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        ArrayList<PsiElement> psiElements = new ArrayList<PsiElement>();

        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

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
            .withParent(
                PlatformPatterns.psiElement(TwigElementTypes.PRINT_BLOCK)
            ).withLanguage(TwigLanguage.INSTANCE).accepts(psiElement)) {

            psiElements.addAll(Arrays.asList(this.getFunctions(psiElement)));
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    private PsiElement[] getFunctions(PsiElement psiElement) {
        HashMap<String, TwigExtension> functions = new TwigExtensionParser(psiElement.getProject()).getFunctions();

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

    private PsiElement[] getMacros(PsiElement psiElement) {
        String funcName = psiElement.getText();
        String funcNameSearch = funcName;

        ArrayList<TwigMacro> twigMacros;

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

                Map<String, TwigFile> twigFilesByName = TwigHelper.getTwigFilesByName(psiElement.getProject());

                if(twigFilesByName.containsKey(twigMacro.getTemplate())) {
                    TwigFile twigFile = twigFilesByName.get(twigMacro.getTemplate());

                    return PsiTreeUtil.collectElements(twigFile, new RegexPsiElementFilter(
                        TwigElementTypes.MACRO_TAG,
                        "\\{%\\s?macro\\s?" + Pattern.quote(funcName) + "\\s?\\(.*%}")
                    );
                }

            }
        }

        return new PsiElement[0];
    }


    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
