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
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import com.jetbrains.twig.elements.TwigElementTypes;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.TwigTypeContainer;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.holder.FormDataHolder;
import fr.adrienbrault.idea.symfony2plugin.twig.loader.FileImplementsLazyLoader;
import fr.adrienbrault.idea.symfony2plugin.twig.loader.FileOverwritesLazyLoader;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import icons.TwigIcons;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;
import java.util.function.Supplier;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigLineMarkerProvider implements LineMarkerProvider {
    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> psiElements, @NotNull Collection<? super LineMarkerInfo<?>> results) {
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
                LineMarkerInfo<?> lineIncludes = attachIncludes((TwigFile) psiElement);
                if(lineIncludes != null) {
                    results.add(lineIncludes);
                }

                LineMarkerInfo<?> extending = attachExtends((TwigFile) psiElement);
                if(extending != null) {
                    results.add(extending);
                }

                // eg bundle overwrites
                LineMarkerInfo<?> overwrites = attachOverwrites((TwigFile) psiElement);
                if(overwrites != null) {
                    results.add(overwrites);
                }
            } else if (TwigPattern.getBlockTagPattern().accepts(psiElement) || TwigPattern.getPrintBlockOrTagFunctionPattern("block").accepts(psiElement)) {
                // blocks: {% block 'foobar' %}, {{ block('foobar') }}

                VirtualFile virtualFile = psiElement.getContainingFile().getVirtualFile();
                if(!implementsMap.containsKey(virtualFile)) {
                    implementsMap.put(virtualFile, new FileImplementsLazyLoader(psiElement.getProject(), virtualFile));
                }

                LineMarkerInfo<?> lineImpl = attachBlockImplements(psiElement, implementsMap.get(virtualFile));
                if(lineImpl != null) {
                    results.add(lineImpl);
                }

                if(fileOverwritesLazyLoader == null) {
                    fileOverwritesLazyLoader = new FileOverwritesLazyLoader(psiElements.get(0).getProject());
                }

                LineMarkerInfo<?> lineOverwrites = attachBlockOverwrites(psiElement, fileOverwritesLazyLoader);
                if(lineOverwrites != null) {
                    results.add(lineOverwrites);
                }
            } else if(TwigPattern.getFunctionPattern("form_start", "form", "form_end", "form_rest").accepts(psiElement)) {
                LineMarkerInfo<?> lineOverwrites = attachFormType(psiElement);
                if(lineOverwrites != null) {
                    results.add(lineOverwrites);
                }
            }
        }
    }

    private void attachController(@NotNull TwigFile twigFile, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {

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

    @Nullable
    private LineMarkerInfo<?> attachIncludes(@NotNull TwigFile twigFile) {
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
            .setTargets(NotNullLazyValue.lazy(new MyTemplateIncludeLazyValue(twigFile, templateNames)))
            .setTooltipText("Navigate to includes")
            .setCellRenderer(new MyFileReferencePsiElementListCellRenderer());

        return builder.createLineMarkerInfo(twigFile);
    }

    @Nullable
    private LineMarkerInfo<?> attachExtends(@NotNull TwigFile twigFile) {
        Collection<String> templateNames = TwigUtil.getTemplateNamesForFile(twigFile);

        boolean found = false;
        for(String templateName: templateNames) {
            Project project = twigFile.getProject();

            Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(
                TwigExtendsStubIndex.KEY, templateName, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE)
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
            .setTargets(NotNullLazyValue.lazy(new TemplateExtendsLazyTargets(twigFile.getProject(), twigFile.getVirtualFile())))
            .setTooltipText("Navigate to extends")
            .setCellRenderer(new MyFileReferencePsiElementListCellRenderer());

        return builder.createLineMarkerInfo(twigFile);
    }

    @Nullable
    private LineMarkerInfo<?> attachOverwrites(@NotNull TwigFile twigFile) {
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

        return getRelatedPopover("Overwrites", "Overwrite", twigFile, gotoRelatedItems, Symfony2Icons.TWIG_LINE_OVERWRITE, "Go to the overwritten template");
    }

    private LineMarkerInfo<?> getRelatedPopover(String singleItemTitle, String singleItemTooltipPrefix, PsiElement lineMarkerTarget, List<GotoRelatedItem> gotoRelatedItems, Icon icon, String accessibleName) {

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
            new ConstantFunction<>(title),
            new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems),
            GutterIconRenderer.Alignment.RIGHT,
            () -> accessibleName
        );
    }

    @Nullable
    private LineMarkerInfo<?> attachBlockImplements(@NotNull PsiElement psiElement, @NotNull FileImplementsLazyLoader implementsLazyLoader) {
        if(!TwigBlockUtil.hasBlockImplementations(psiElement, implementsLazyLoader)) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTED)
            .setTargets(NotNullLazyValue.lazy(new BlockImplementationLazyValue(psiElement)))
            .setTooltipText("Implementations")
            .setCellRenderer(new MyBlockListCellRenderer());

        return builder.createLineMarkerInfo(psiElement);
    }

    private static class BlockImplementationLazyValue implements Supplier<Collection<? extends PsiElement>> {
        @NotNull
        private final PsiElement psiElement;

        BlockImplementationLazyValue(@NotNull PsiElement psiElement) {
            this.psiElement = psiElement;
        }

        @Override
        public Collection<? extends PsiElement> get() {
            return TwigBlockUtil.getBlockImplementationTargets(psiElement);
        }
    }

    /**
     * Provides lazy targets for given template scope
     */
    private static class BlockOverwriteLazyValue implements Supplier<Collection<? extends PsiElement>> {
        @NotNull
        private final PsiElement psiElement;

        BlockOverwriteLazyValue(@NotNull PsiElement psiElement) {
            this.psiElement = psiElement;
        }

        @Override
        public Collection<? extends PsiElement> get() {
            return TwigBlockUtil.getBlockOverwriteTargets(psiElement);
        }
    }

    @Nullable
    private LineMarkerInfo<?> attachBlockOverwrites(@NotNull PsiElement psiElement, @NotNull FileOverwritesLazyLoader loader) {
        if(!TwigBlockUtil.hasBlockOverwrites(psiElement, loader)) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.OVERRIDES)
            .setTargets(NotNullLazyValue.lazy(new BlockOverwriteLazyValue(psiElement)))
            .setTooltipText("Overwrites")
            .setCellRenderer(new MyBlockListCellRenderer());

        return builder.createLineMarkerInfo(psiElement);
    }

    @Nullable
    private LineMarkerInfo<?> attachFormType(@NotNull PsiElement psiElement) {
        // form(theform);
        PsiElement twigFunctionParameterIdentifierPsi = TwigUtil.getTwigFunctionParameterIdentifierPsi(psiElement);
        if (twigFunctionParameterIdentifierPsi == null) {
            return null;
        }

        Collection<TwigTypeContainer> twigTypeContainers = TwigTypeResolveUtil.resolveTwigMethodName(twigFunctionParameterIdentifierPsi, TwigTypeResolveUtil.formatPsiTypeNameWithCurrent(twigFunctionParameterIdentifierPsi));

        Collection<PhpClass> phpClasses = new HashSet<>();

        for (TwigTypeContainer twigTypeContainer : twigTypeContainers) {
            Object dataHolder = twigTypeContainer.getDataHolder();
            if (dataHolder instanceof FormDataHolder && PhpElementsUtil.isInstanceOf(((FormDataHolder) dataHolder).getFormType(), "\\Symfony\\Component\\Form\\FormTypeInterface")) {
                phpClasses.add(((FormDataHolder) dataHolder).getFormType());
            }
        }

        if (phpClasses.isEmpty()) {
            return null;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.FORM_TYPE_LINE_MARKER)
            .setTargets(phpClasses)
            .setTooltipText("Navigate to Form")
            .setCellRenderer(new MyBlockListCellRenderer());

        return builder.createLineMarkerInfo(psiElement.getFirstChild());
    }

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
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
            String relativePath = VfsUtil.getRelativePath(containingFile.getVirtualFile(), ProjectUtil.getProjectDir(psiElement), '/');
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

    private static class MyTemplateIncludeLazyValue implements Supplier<Collection<? extends PsiElement>> {
        @NotNull
        private final TwigFile twigFile;

        @NotNull
        private final Collection<String> templateNames;

        MyTemplateIncludeLazyValue(@NotNull TwigFile twigFile, @NotNull Collection<String> templateNames) {
            this.twigFile = twigFile;
            this.templateNames = templateNames;
        }

        @Override
        public Collection<? extends PsiElement> get() {
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

    /**
     * Find "extends" which are targeting the given template file
     */
    private static class TemplateExtendsLazyTargets implements Supplier<Collection<? extends PsiElement>> {
        @NotNull
        private final Project project;
        @NotNull
        private final VirtualFile virtualFile;

        TemplateExtendsLazyTargets(@NotNull Project project, @NotNull VirtualFile virtualFile) {
            this.project = project;
            this.virtualFile = virtualFile;
        }

        @Override
        public Collection<? extends PsiElement> get() {
            return PsiElementUtils.convertVirtualFilesToPsiFiles(project, TwigUtil.getTemplatesExtendingFile(project, virtualFile));
        }
    }
}
