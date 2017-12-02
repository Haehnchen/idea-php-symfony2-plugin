package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ConstantFunction;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.loader.FileImplementsLazyLoader;
import fr.adrienbrault.idea.symfony2plugin.twig.loader.FileOverwritesLazyLoader;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import icons.TwigIcons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigLineMarkerProvider implements LineMarkerProvider {
    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> results) {
        if(psiElements.size() == 0 || !Symfony2ProjectComponent.isEnabled(psiElements.get(0))) {
            return;
        }

        Map<VirtualFile, FileImplementsLazyLoader> implementsMap = new HashMap<>();
        FileOverwritesLazyLoader fileOverwritesLazyLoader = null;

        for(PsiElement psiElement: psiElements) {
            // controller
            if(psiElement instanceof TwigFile) {
                attachController((TwigFile) psiElement, results);

                // find foreign file references tags like:
                // include, embed, source, from, import, ...
                LineMarkerInfo lineIncludes = attachIncludes((TwigFile) psiElement);
                if(lineIncludes != null) {
                    results.add(lineIncludes);
                }

                // eg bundle overwrites
                LineMarkerInfo overwrites = attachOverwrites((TwigFile) psiElement);
                if(overwrites != null) {
                    results.add(overwrites);
                }
            } else if (TwigPattern.getBlockTagPattern().accepts(psiElement) || TwigPattern.getPrintBlockFunctionPattern("block").accepts(psiElement)) {
                // blocks: {% block 'foobar' %}, {{ block('foobar') }}

                VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
                if(!implementsMap.containsKey(virtualFile)) {
                    implementsMap.put(virtualFile, new FileImplementsLazyLoader(psiElement.getProject(), virtualFile));
                }

                LineMarkerInfo lineImpl = attachBlockImplements(psiElement, implementsMap.get(virtualFile));
                if(lineImpl != null) {
                    results.add(lineImpl);
                }

                if(fileOverwritesLazyLoader == null) {
                    fileOverwritesLazyLoader = new FileOverwritesLazyLoader(psiElements.get(0).getProject());
                }

                LineMarkerInfo lineOverwrites = attachBlockOverwrites(psiElement, fileOverwritesLazyLoader);
                if(lineOverwrites != null) {
                    results.add(lineOverwrites);
                }
            }
        }
    }

    private void attachController(@NotNull TwigFile twigFile, @NotNull Collection<? super RelatedItemLineMarkerInfo> result) {

        Set<Function> methods = new HashSet<>();


        methods.addAll(TwigUtil.findTwigFileController(twigFile));
        methods.addAll(TwigUtil.getTwigFileMethodUsageOnIndex(twigFile));

        if(methods.size() == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
            setTargets(methods).
            setTooltipText("Navigate to controller");

        result.add(builder.createLineMarkerInfo(twigFile));
    }

    private LineMarkerInfo attachIncludes(@NotNull TwigFile twigFile) {
        Collection<String> templateNames = TwigUtil.getTemplateNamesForFile(twigFile);

        boolean found = false;
        for(String templateName: templateNames) {
            Project project = twigFile.getProject();

            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(
                TwigIncludeStubIndex.KEY, templateName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE)
            );

            // stop on first target, we load them lazily afterwards
            if(containingFiles.size() > 0) {
                found = true;
                break;
            }
        }

        if(!found) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTED)
            .setTargets(new MyTemplateIncludeLazyValue(twigFile, templateNames))
            .setTooltipText("Navigate to includes")
            .setCellRenderer(new MyFileReferencePsiElementListCellRenderer());

        return builder.createLineMarkerInfo(twigFile);
    }

    @Nullable
    private LineMarkerInfo attachOverwrites(@NotNull TwigFile twigFile) {
        Collection<PsiFile> targets = new ArrayList<>();

        for (String templateName: TwigUtil.getTemplateNamesForFile(twigFile)) {
            for (PsiFile psiFile : TwigUtil.getTemplatePsiElements(twigFile.getProject(), templateName)) {
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
                TwigUtil.getPresentableTemplateName(blockTag, true)
            ).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_OVERWRITE));
        }

        return getRelatedPopover("Overwrites", "Overwrite", twigFile, gotoRelatedItems, Symfony2Icons.TWIG_LINE_OVERWRITE);
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

        return new LineMarkerInfo<>(
            lineMarkerTarget,
            lineMarkerTarget.getTextRange(),
            icon,
            6,
            new ConstantFunction<>(title),
            new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems),
            GutterIconRenderer.Alignment.RIGHT
        );
    }

    @Nullable
    private LineMarkerInfo attachBlockImplements(@NotNull PsiElement psiElement, @NotNull FileImplementsLazyLoader implementsLazyLoader) {
        if(!TwigBlockUtil.hasBlockImplementations(psiElement, implementsLazyLoader)) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTED)
            .setTargets(new BlockImplementationLazyValue(psiElement))
            .setTooltipText("Implementations")
            .setCellRenderer(new MyBlockListCellRenderer());

        return builder.createLineMarkerInfo(psiElement);
    }

    private static class BlockImplementationLazyValue extends NotNullLazyValue<Collection<? extends PsiElement>> {
        @NotNull
        private final PsiElement psiElement;

        BlockImplementationLazyValue(@NotNull PsiElement psiElement) {
            this.psiElement = psiElement;
        }

        @NotNull
        @Override
        protected Collection<? extends PsiElement> compute() {
            return TwigBlockUtil.getBlockImplementationTargets(psiElement);
        }
    }

    /**
     * Provides lazy targets for given template scope
     */
    private static class BlockOverwriteLazyValue extends NotNullLazyValue<Collection<? extends PsiElement>> {
        @NotNull
        private final PsiElement psiElement;

        BlockOverwriteLazyValue(@NotNull PsiElement psiElement) {
            this.psiElement = psiElement;
        }

        @NotNull
        @Override
        protected Collection<? extends PsiElement> compute() {
            return TwigBlockUtil.getBlockOverwriteTargets(psiElement);
        }
    }

    @Nullable
    private LineMarkerInfo attachBlockOverwrites(PsiElement psiElement, @NotNull FileOverwritesLazyLoader loader) {
        if(!TwigBlockUtil.hasBlockOverwrites(psiElement, loader)) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.OVERRIDES)
            .setTargets(new BlockOverwriteLazyValue(psiElement))
            .setTooltipText("Overwrites")
            .setCellRenderer(new MyBlockListCellRenderer());

        return builder.createLineMarkerInfo(psiElement);
    }

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    private static class MyFileReferencePsiElementListCellRenderer extends PsiElementListCellRenderer {
        @Override
        public String getElementText(PsiElement psiElement) {
            String symbolPresentableText = SymbolPresentationUtil.getSymbolPresentableText(psiElement);
            return StringUtils.abbreviate(symbolPresentableText, 50);
        }

        @Nullable
        @Override
        protected String getContainerText(PsiElement psiElement, String s) {
            // relative path else fallback to default name extraction
            PsiFile containingFile = psiElement.getContainingFile();
            String relativePath = VfsUtil.getRelativePath(containingFile.getVirtualFile(), psiElement.getProject().getBaseDir(), '/');
            return relativePath != null ? relativePath : SymbolPresentationUtil.getSymbolContainerText(psiElement);
        }

        @Override
        protected int getIconFlags() {
            return 1;
        }

        @Override
        protected Icon getIcon(PsiElement psiElement) {
            if(psiElement.getNode().getElementType() == TwigElementTypes.INCLUDE_TAG) {
                return PhpIcons.IMPLEMENTED;
            } else if(psiElement.getNode().getElementType() == TwigElementTypes.EMBED_TAG) {
                return PhpIcons.OVERRIDEN;
            }

            return TwigIcons.TwigFileIcon;
        }
    }

    private static class MyTemplateIncludeLazyValue extends NotNullLazyValue<Collection<? extends PsiElement>> {
        @NotNull
        private final TwigFile twigFile;

        @NotNull
        private final Collection<String> templateNames;

        MyTemplateIncludeLazyValue(@NotNull TwigFile twigFile, @NotNull Collection<String> templateNames) {
            this.twigFile = twigFile;
            this.templateNames = templateNames;
        }

        @NotNull
        @Override
        protected Collection<? extends PsiElement> compute() {
            Collection<VirtualFile> twigFiles = new ArrayList<>();

            Project project = twigFile.getProject();

            for(String templateName: this.templateNames) {
                // collect files which contains given template name for inclusion
                twigFiles.addAll(FileBasedIndex.getInstance().getContainingFiles(
                    TwigIncludeStubIndex.KEY,
                    templateName,
                    GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE))
                );
            }

            Collection<PsiElement> targets = new ArrayList<>();

            for (VirtualFile virtualFile : twigFiles) {
                // resolve virtual file
                PsiFile myTwigFile = PsiManager.getInstance(project).findFile(virtualFile);
                if(!(myTwigFile instanceof TwigFile)) {
                    continue;
                }

                Collection<PsiElement> fileTargets = new ArrayList<>();

                TwigUtil.visitTemplateIncludes((TwigFile) myTwigFile, templateInclude -> {
                        if(this.templateNames.contains(templateInclude.getTemplateName()) || this.templateNames.contains(TwigUtil.normalizeTemplateName(templateInclude.getTemplateName()))) {
                            fileTargets.add(templateInclude.getPsiElement());
                        }
                    }
                );

                // navigate to include pattern; else fallback to file scope
                if(fileTargets.size() > 0) {
                    targets.addAll(fileTargets);
                } else {
                    targets.add(myTwigFile);
                }
            }

            return targets;
        }
    }

    private static class MyBlockListCellRenderer extends PsiElementListCellRenderer {
        @Override
        public String getElementText(PsiElement psiElement) {
            return StringUtils.abbreviate(
                SymbolPresentationUtil.getSymbolPresentableText(psiElement),
                50
            );
        }

        @Override
        protected String getContainerText(PsiElement psiElement, String s) {
            return TwigUtil.getPresentableTemplateName(psiElement, true);
        }

        @Override
        protected int getIconFlags() {
            return 1;
        }

        @Override
        protected Icon getIcon(PsiElement psiElement) {
            return TwigIcons.TwigFileIcon;
        }
    }
}
