package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ConstantFunction;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.dict.TemplateFileMap;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.stream.Collectors;

public class TwigControllerLineMarkerProvider implements LineMarkerProvider {


    private TemplateFileMap templateMapCache = null;

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> results) {

        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        for(PsiElement psiElement: psiElements) {

            // blocks
            if (TwigHelper.getBlockTagPattern().accepts(psiElement)) {

                LineMarkerInfo lineImpl = this.attachBlockImplements(psiElement);
                if(lineImpl != null) {
                    results.add(lineImpl);
                }

                LineMarkerInfo lineOverwrites = this.attachBlockOverwrites(psiElement);
                if(lineOverwrites != null) {
                    results.add(lineOverwrites);
                }

            }

            // controller
            if(psiElement instanceof TwigFile) {
                attachController((TwigFile) psiElement, results);

                // attach parent includes goto
                LineMarkerInfo lineIncludes = attachIncludes((TwigFile) psiElement);
                if(lineIncludes != null) {
                    results.add(lineIncludes);
                }

                // attach parent includes goto
                LineMarkerInfo lineFromInclude = attachFromIncludes((TwigFile) psiElement);
                if(lineFromInclude != null) {
                    results.add(lineFromInclude);
                }

                // attach parent includes goto
                LineMarkerInfo overwrites = attachOverwrites((TwigFile) psiElement);
                if(overwrites != null) {
                    results.add(overwrites);
                }
            }

        }

        // reset cache
        templateMapCache = null;

    }

    private void attachController(@NotNull TwigFile twigFile, @NotNull Collection<? super RelatedItemLineMarkerInfo> result) {

        Set<Function> methods = new HashSet<>();
        Method method = TwigUtil.findTwigFileController(twigFile);
        if(method != null) {
            methods.add(method);
        }

        methods.addAll(TwigUtil.getTwigFileMethodUsageOnIndex(twigFile));

        if(methods.size() == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
            setTargets(methods).
            setTooltipText("Navigate to controller");

        result.add(builder.createLineMarkerInfo(twigFile));
    }

    private LineMarkerInfo attachIncludes(TwigFile twigFile) {

        TemplateFileMap files = getTemplateFilesByName(twigFile.getProject());

        final Collection<PsiFile> targets = new ArrayList<>();
        for(String templateName: TwigUtil.getTemplateName(twigFile.getVirtualFile(), files)) {

            final Project project = twigFile.getProject();
            FileBasedIndexImpl.getInstance().getFilesWithKey(TwigIncludeStubIndex.KEY, new HashSet<>(Arrays.asList(templateName)), virtualFile -> {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

                if(psiFile != null) {
                    targets.add(psiFile);
                }

                return true;
            }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE));

        }

        if(targets.size() == 0) {
            return null;
        }


        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<>();
        for(PsiElement blockTag: targets) {
            gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(blockTag, TwigUtil.getPresentableTemplateName(files.getTemplates(), blockTag, true)).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
        }

        return getRelatedPopover("Implementations", "Impl: " ,twigFile, gotoRelatedItems);

    }

    @Nullable
    private LineMarkerInfo attachOverwrites(@NotNull TwigFile twigFile) {

        Collection<PsiFile> targets = new ArrayList<>();

        TemplateFileMap files = getTemplateFilesByName(twigFile.getProject());

        for (String templateName: TwigUtil.getTemplateName(twigFile.getVirtualFile(), files)) {
            for (PsiFile psiFile : TwigHelper.getTemplatePsiElements(twigFile.getProject(), templateName)) {
                if(!psiFile.getVirtualFile().equals(twigFile.getVirtualFile()) && !targets.contains(psiFile)) {
                    targets.add(psiFile);
                }
            }
        }

        if(targets.size() == 0) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<>();
        for(PsiElement blockTag: targets) {
            gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(
                blockTag,
                TwigUtil.getPresentableTemplateName(files.getTemplates(), blockTag, true)
            ).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_OVERWRITE));
        }

        return getRelatedPopover("Overwrites", "Overwrite", twigFile, gotoRelatedItems, Symfony2Icons.TWIG_LINE_OVERWRITE);
    }

    private TemplateFileMap getTemplateFilesByName(Project project) {
        return this.templateMapCache == null ? this.templateMapCache = TwigHelper.getTemplateMap(project, true, false) : this.templateMapCache;
    }

    @Nullable
    private LineMarkerInfo attachFromIncludes(TwigFile twigFile) {
        TemplateFileMap files = getTemplateFilesByName(twigFile.getProject());

        final Collection<PsiFile> targets = TwigUtil.getImplementationsForExtendsTag(twigFile, files);
        if(targets.size() == 0) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = targets.stream().map(blockTag ->
            new RelatedPopupGotoLineMarker
                .PopupGotoRelatedItem(blockTag, TwigUtil.getPresentableTemplateName(files.getTemplates(), blockTag, true))
                .withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER))
            .collect(Collectors.toList()
        );

        return getRelatedPopover("Implementations", "Impl: ", twigFile, gotoRelatedItems);
    }

    private LineMarkerInfo getRelatedPopover(String singleItemTitle, String singleItemTooltipPrefix, PsiElement lineMarkerTarget, List<GotoRelatedItem> gotoRelatedItems) {
        return getRelatedPopover(singleItemTitle, singleItemTooltipPrefix, lineMarkerTarget, gotoRelatedItems, PhpIcons.IMPLEMENTED);
    }

    private LineMarkerInfo getRelatedPopover(String singleItemTitle, String singleItemTooltipPrefix, PsiElement lineMarkerTarget, List<GotoRelatedItem> gotoRelatedItems, Icon icon) {

        // single item has no popup
        String title = singleItemTitle;
        if(gotoRelatedItems.size() == 1) {
            String customName = gotoRelatedItems.get(0).getCustomName();
            if(customName != null) {
                title = String.format(singleItemTooltipPrefix, customName);
            }
        }

        return new LineMarkerInfo<>(lineMarkerTarget, lineMarkerTarget.getTextOffset(), icon, 6, new ConstantFunction<>(title), new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems));
    }

    @Nullable
    private LineMarkerInfo attachBlockImplements(final PsiElement psiElement) {
        PsiFile psiFile = psiElement.getContainingFile();
        if(psiFile == null) {
            return null;
        }

        TemplateFileMap files = getTemplateFilesByName(psiElement.getProject());

        Collection<PsiFile> twigChild = TwigUtil.getTemplateFileReferences(psiFile, files);
        if(twigChild.size() == 0) {
            return null;
        }

        final String blockName = psiElement.getText();

        List<PsiElement> blockTargets = new ArrayList<>();
        for(PsiFile psiFile1: twigChild) {

            blockTargets.addAll(Arrays.asList(PsiTreeUtil.collectElements(psiFile1, psiElement1 ->
                TwigHelper.getBlockTagPattern().accepts(psiElement1) && blockName.equals(psiElement1.getText())))
            );

        }

        if(blockTargets.size() == 0) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<>();
        for(PsiElement blockTag: blockTargets) {
            gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(blockTag, TwigUtil.getPresentableTemplateName(files.getTemplates(), blockTag, true)).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
        }

        return getRelatedPopover("Implementations", "Impl: ", psiElement, gotoRelatedItems);

    }

    @Nullable
    private LineMarkerInfo attachBlockOverwrites(PsiElement psiElement) {

        PsiElement[] blocks = TwigTemplateGoToDeclarationHandler.getBlockGoTo(psiElement);
        if(blocks.length == 0) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<>();
        for(PsiElement blockTag: blocks) {
            gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(blockTag, TwigUtil.getPresentableTemplateName(getTemplateFilesByName(psiElement.getProject()).getTemplates(), blockTag, true)).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
        }

        // single item has no popup
        String title = "Overwrites";
        if(gotoRelatedItems.size() == 1) {
            String customName = gotoRelatedItems.get(0).getCustomName();
            if(customName != null) {
                title = title.concat(": ").concat(customName);
            }
        }

        return new LineMarkerInfo<>(psiElement, psiElement.getTextOffset(), PhpIcons.OVERRIDES, 6, new ConstantFunction<>(title), new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems));
    }

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }
}
