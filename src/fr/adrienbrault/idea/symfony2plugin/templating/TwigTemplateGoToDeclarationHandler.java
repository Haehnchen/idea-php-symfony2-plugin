package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlock;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigBlockParser;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TwigExtension;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigExtensionParser;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import fr.adrienbrault.idea.symfony2plugin.util.controller.ControllerIndex;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author Adrien Brault <adrien.brault@gmail.com>
 */
public class TwigTemplateGoToDeclarationHandler implements GotoDeclarationHandler {

    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(PsiElement psiElement, int i, Editor editor) {

        if(!Symfony2ProjectComponent.isEnabled(psiElement) || !PlatformPatterns.psiElement().withLanguage(TwigLanguage.INSTANCE).accepts(psiElement)) {
            return null;
        }

        if (TwigHelper.getBlockTagPattern().accepts(psiElement)) {
            return getBlockGoTo(psiElement);
        }

        if (TwigHelper.getPathAfterLeafPattern().accepts(psiElement)) {
            PsiElement[] psiElements = this.getRouteParameterGoTo(psiElement);
            if(psiElements.length > 0) {
                return psiElements;
            }
        }

        // support: {% include() %}, {{ include() }}
        if(TwigHelper.getTemplateFileReferenceTagPattern().accepts(psiElement) || TwigHelper.getPrintBlockFunctionPattern("include", "source").accepts(psiElement)) {
            return this.getTwigFiles(psiElement);
        }

        if(TwigHelper.getAutocompletableRoutePattern().accepts(psiElement)) {
            return this.getRouteGoTo(psiElement);
        }

        // find trans('', {}, '|')
        // tricky way to get the function string trans(...)
        if (TwigHelper.getTransDomainPattern().accepts(psiElement)) {
            PsiElement psiElementTrans = PsiElementUtils.getPrevSiblingOfType(psiElement, PlatformPatterns.psiElement(TwigTokenTypes.IDENTIFIER).withText(PlatformPatterns.string().oneOf("trans", "transchoice")));
            if(psiElementTrans != null && TwigHelper.getTwigMethodString(psiElementTrans) != null) {
                return getTranslationDomainGoto(psiElement);
            }
        }

        // {% trans from "app" %}
        // {% transchoice from "app" %}
        if (TwigHelper.getTranslationTokenTagFromPattern().accepts(psiElement)) {
            return getTranslationDomainGoto(psiElement);
        }

        if (TwigHelper.getTranslationPattern("trans", "transchoice").accepts(psiElement)) {
            return getTranslationKeyGoTo(psiElement);
        }

        // provide global twig file resolving
        if (PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT)
            .withText(PlatformPatterns.string().endsWith(".twig")).accepts(psiElement)) {

            return this.getTwigFiles(psiElement);
        }

        if(TwigHelper.getPrintBlockOrTagFunctionPattern("controller").accepts(psiElement) || TwigHelper.getStringAfterTagNamePattern("render").accepts(psiElement)) {
            PsiElement controllerMethod = this.getControllerGoTo(psiElement);
            if(controllerMethod != null) {
                return new PsiElement[] { controllerMethod };
            }
        }

        if(TwigHelper.getTransDefaultDomainPattern().accepts(psiElement)) {
            List<PsiFile> domainPsiFiles = TranslationUtil.getDomainPsiFiles(psiElement.getProject(), psiElement.getText());
            return domainPsiFiles.toArray(new PsiElement[domainPsiFiles.size()]);
        }

        if(TwigHelper.getFilterPattern().accepts(psiElement)) {
            return getFilterGoTo(psiElement);
        }

        return null;
    }

    private PsiElement[] getRouteParameterGoTo(PsiElement psiElement) {

        String routeName = TwigHelper.getMatchingRouteNameOnParameter(psiElement);
        if(routeName == null) {
            return new PsiElement[0];
        }

        return RouteHelper.getRouteParameterPsiElements(psiElement.getProject(), routeName, psiElement.getText());
    }

    private PsiElement getControllerGoTo(PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        return ControllerIndex.getControllerMethod(psiElement.getProject(), text);
    }

    @Nullable
    private PsiElement[] getTwigFiles(PsiElement psiElement) {

        String templateName = psiElement.getText();
        PsiElement[] psiElements = TwigHelper.getTemplatePsiElements(psiElement.getProject(), templateName);

        if(psiElements.length == 0) {
            return null;
        }

        return psiElements;
    }

    private PsiElement[] getFilterGoTo(PsiElement psiElement) {
        Map<String, TwigExtension> filters = new TwigExtensionParser(psiElement.getProject()).getFilters();
        if(!filters.containsKey(psiElement.getText())) {
            return new PsiElement[0];
        }

        String signature = filters.get(psiElement.getText()).getSignature();
        if(signature == null) {
            return new PsiElement[0];
        }

        return PhpElementsUtil.getPsiElementsBySignature(psiElement.getProject(), signature);
    }

    @NotNull
    public static PsiElement[] getBlockGoTo(@NotNull PsiElement psiElement) {
        String blockName = psiElement.getText();
        if(StringUtils.isBlank(blockName)) {
            return new PsiElement[0];
        }

        Collection<PsiElement> psiElements = new HashSet<>();
        Pair<PsiFile[], Boolean> scopedFile = TwigHelper.findScopedFile(psiElement);
        for (PsiFile psiFile : scopedFile.getFirst()) {
            ContainerUtil.addAll(psiElements, getBlockNameGoTo(psiFile, blockName, scopedFile.getSecond()));
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    @NotNull
    public static PsiElement[] getBlockNameGoTo(@NotNull PsiFile psiFile, @NotNull String blockName) {
        return getBlockNameGoTo(psiFile, blockName, false);
    }

    @NotNull
    public static PsiElement[] getBlockNameGoTo(PsiFile psiFile, String blockName, boolean withSelfBlocks) {
        Map<String, VirtualFile> twigFilesByName = TwigHelper.getTwigFilesByName(psiFile.getProject());
        List<TwigBlock> blocks = new TwigBlockParser(twigFilesByName).withSelfBlocks(withSelfBlocks).walk(psiFile);
        List<PsiElement> psiElements = new ArrayList<>();
        for (TwigBlock block : blocks) {
            if(block.getName().equals(blockName)) {
                Collections.addAll(psiElements, block.getBlock());
            }
        }

        return psiElements.toArray(new PsiElement[psiElements.size()]);
    }

    private PsiElement[] getRouteGoTo(PsiElement psiElement) {

        String text = PsiElementUtils.getText(psiElement);

        if(StringUtils.isBlank(text)) {
            return new PsiElement[0];
        }

        PsiElement[] methods = RouteHelper.getMethods(psiElement.getProject(), text);
        if(methods.length > 0) {
            return methods;
        }

        List<PsiElement> psiElementList = RouteHelper.getRouteDefinitionTargets(psiElement.getProject(), text);
        return psiElementList.toArray(new PsiElement[psiElementList.size()]);
    }

    private PsiElement[] getTranslationKeyGoTo(PsiElement psiElement) {
        String translationKey = psiElement.getText();
        return TranslationUtil.getTranslationPsiElements(psiElement.getProject(), translationKey, TwigUtil.getPsiElementTranslationDomain(psiElement));
    }

    private PsiElement[] getTranslationDomainGoto(PsiElement psiElement) {
        String text = PsiElementUtils.trimQuote(psiElement.getText());
        
        if(StringUtils.isNotBlank(text)) {
            List<PsiFile> domainPsiFiles = TranslationUtil.getDomainPsiFiles(psiElement.getProject(), text);
            return domainPsiFiles.toArray(new PsiElement[domainPsiFiles.size()]);
        }

        return new PsiElement[0];
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
