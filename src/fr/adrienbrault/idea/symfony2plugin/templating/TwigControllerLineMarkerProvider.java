package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ConstantFunction;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigFileType;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigIncludeStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TwigControllerLineMarkerProvider implements LineMarkerProvider {

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> psiElements, @NotNull Collection<LineMarkerInfo> results) {

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
            }

            // attach parent includes goto
            if(psiElement instanceof TwigFile) {
                attachIncludes((TwigFile) psiElement, results);
            }

        }

    }

    private void attachController(TwigFile psiElement, Collection<? super RelatedItemLineMarkerInfo> result) {

        Method method = TwigUtil.findTwigFileController(psiElement);
        if(method == null) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(Symfony2Icons.TWIG_CONTROLLER_LINE_MARKER).
            setTargets(method).
            setTooltipText("Navigate to controller");

        result.add(builder.createLineMarkerInfo(psiElement));
    }

    private void attachIncludes(TwigFile twigFile, Collection<? super RelatedItemLineMarkerInfo> result) {


        final Collection<PsiFile> targets = new ArrayList<PsiFile>();
        for(Map.Entry<String, PsiFile> entry: TwigUtil.getTemplateName(twigFile).entrySet()) {

            final Project project = twigFile.getProject();
            FileBasedIndexImpl.getInstance().getFilesWithKey(TwigIncludeStubIndex.KEY, new HashSet<String>(Arrays.asList(entry.getKey())), new Processor<VirtualFile>() {
                @Override
                public boolean process(VirtualFile virtualFile) {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

                    if(psiFile != null) {
                        targets.add(psiFile);
                    }

                    return true;
                }
            }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), TwigFileType.INSTANCE));

        }

        if(targets.size() == 0) {
            return;
        }

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder.create(PhpIcons.IMPLEMENTS).
            setTargets(targets).
            setTooltipText("Navigate to parent");

        result.add(builder.createLineMarkerInfo(twigFile));

    }

    @Nullable
    private LineMarkerInfo attachBlockImplements(final PsiElement psiElement) {

        Map<String, PsiFile> files = TwigHelper.getTemplateFilesByName(psiElement.getProject(), true, false);
        PsiFile psiFile = psiElement.getContainingFile();
        if(psiFile == null) {
            return null;
        }

        List<PsiFile> twigChild = new ArrayList<PsiFile>();
        getTwigChildList(files, psiFile, twigChild, 8);

        if(twigChild.size() == 0) {
            return null;
        }

        final String blockName = psiElement.getText();

        List<PsiElement> blockTargets = new ArrayList<PsiElement>();
        for(PsiFile psiFile1: twigChild) {

            blockTargets.addAll(Arrays.asList(PsiTreeUtil.collectElements(psiFile1, new PsiElementFilter() {
                @Override
                public boolean isAccepted(PsiElement psiElement) {
                    return TwigHelper.getBlockTagPattern().accepts(psiElement) && blockName.equals(psiElement.getText());
                }
            })));

        }

        if(blockTargets.size() == 0) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<GotoRelatedItem>();
        for(PsiElement blockTag: blockTargets) {
            gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(blockTag, TwigUtil.getPresentableTemplateName(files, blockTag, true)).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
        }

        // single item has no popup
        String title = "Implementations";
        if(gotoRelatedItems.size() == 1) {
            String customName = gotoRelatedItems.get(0).getCustomName();
            if(customName != null) {
                title = String.format("Impl: %s", customName);
            }
        }

        return new LineMarkerInfo<PsiElement>(psiElement, psiElement.getTextOffset(), PhpIcons.IMPLEMENTED, 6, new ConstantFunction(title), new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems));

    }

    @Nullable
    private LineMarkerInfo attachBlockOverwrites(PsiElement psiElement) {

        Map<String, PsiFile> files = TwigHelper.getTemplateFilesByName(psiElement.getProject(), true, false);

        PsiElement[] blocks = TwigTemplateGoToDeclarationHandler.getBlockGoTo(psiElement);
        if(blocks.length == 0) {
            return null;
        }

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<GotoRelatedItem>();
        for(PsiElement blockTag: blocks) {
            gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(blockTag, TwigUtil.getPresentableTemplateName(files, blockTag, true)).withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
        }

        // single item has no popup
        String title = "Overwrites";
        if(gotoRelatedItems.size() == 1) {
            String customName = gotoRelatedItems.get(0).getCustomName();
            if(customName != null) {
                title = title.concat(": ").concat(customName);
            }
        }

        return new LineMarkerInfo<PsiElement>(psiElement, psiElement.getTextOffset(), PhpIcons.OVERRIDES, 6, new ConstantFunction(title), new RelatedPopupGotoLineMarker.NavigationHandler(gotoRelatedItems));
    }

    @Nullable
    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement psiElement) {
        return null;
    }

    private static void getTwigChildList(Map<String, PsiFile> files, final PsiFile psiFile, final List<PsiFile> twigChild, int depth) {

        if(depth <= 0) {
            return;
        }

        // use set here, we have multiple shortcut on one file, but only one is required
        final HashSet<VirtualFile> virtualFiles = new LinkedHashSet<VirtualFile>();

        for(Map.Entry<String, PsiFile> entry: files.entrySet()) {

            // getFilesWithKey dont support keyset with > 1 items (bug?), so we cant merge calls
            if(entry.getValue().equals(psiFile)) {
                String key = entry.getKey();
                FileBasedIndexImpl.getInstance().getFilesWithKey(TwigExtendsStubIndex.KEY, new HashSet<String>(Arrays.asList(key)), new Processor<VirtualFile>() {
                    @Override
                    public boolean process(VirtualFile virtualFile) {
                        virtualFiles.add(virtualFile);
                        return true;
                    }
                }, GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(psiFile.getProject()), TwigFileType.INSTANCE));

            }

        }

        // finally resolve virtual file to twig files
        for(VirtualFile virtualFile: virtualFiles) {

            PsiFile resolvedPsiFile = PsiManager.getInstance(psiFile.getProject()).findFile(virtualFile);
            if(resolvedPsiFile != null) {
                twigChild.add(resolvedPsiFile);
                getTwigChildList(files, resolvedPsiFile, twigChild, depth--);
            }

        }

    }

}
