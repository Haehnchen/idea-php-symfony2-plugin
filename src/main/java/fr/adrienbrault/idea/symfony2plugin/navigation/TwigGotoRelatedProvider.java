package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigLanguage;
import com.jetbrains.twig.elements.TwigBlockStatement;
import com.jetbrains.twig.elements.TwigBlockTag;
import com.jetbrains.twig.elements.TwigElementTypes;
import com.jetbrains.twig.elements.TwigTagWithFileReference;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.dic.RelatedPopupGotoLineMarker;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import icons.TwigIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigGotoRelatedProvider extends GotoRelatedProvider {
    @NotNull
    @Override
    public List<? extends GotoRelatedItem> getItems(@NotNull PsiElement psiElement2) {
        if (!Symfony2ProjectComponent.isEnabled(psiElement2)) {
            return Collections.emptyList();
        }

        Language language = psiElement2.getLanguage();
        if (language != TwigLanguage.INSTANCE && language != HTMLLanguage.INSTANCE) {
            return Collections.emptyList();
        }

        List<GotoRelatedItem> gotoRelatedItems = new ArrayList<>();

        PsiElement psiElement = TwigUtil.getElementOnTwigViewProvider(psiElement2);
        if (psiElement == null) {
            return Collections.emptyList();
        }

        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile instanceof TwigFile) {
            // extends
            Set<String> templates = new HashSet<>();
            TwigUtil.visitTemplateExtends((TwigFile) psiFile, pair -> templates.add(pair.getFirst()));
            Set<VirtualFile> virtualFiles = new HashSet<>();
            for (String template : templates) {
                for (PsiFile templatePsiElement : TwigUtil.getTemplatePsiElements(psiElement.getProject(), template)) {
                    VirtualFile virtualFile = templatePsiElement.getVirtualFile();
                    if (!virtualFiles.contains(virtualFile)) {
                        virtualFiles.add(virtualFile);
                        gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templatePsiElement, "extends").withIcon(TwigIcons.TwigFileIcon, Symfony2Icons.TWIG_LINE_MARKER));
                    }
                }
            }

            // twig blocks up
            @Nullable TwigBlockStatement parentOfType = PsiTreeUtil.getParentOfType(psiElement, TwigBlockStatement.class);
            if (parentOfType != null) {
                TwigBlockTag twigBlockTag = PsiTreeUtil.findChildOfType(parentOfType, TwigBlockTag.class);
                if (twigBlockTag != null) {
                    PsiElement childrenOfType = PsiElementUtils.getChildrenOfType(twigBlockTag, TwigPattern.getBlockTagPattern());
                    if (childrenOfType != null) {
                        String blockName = twigBlockTag.getName();

                        gotoRelatedItems.addAll(TwigBlockUtil.getBlockOverwriteTargets(childrenOfType).stream().map((Function<PsiElement, GotoRelatedItem>) psiElement1 ->
                            new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(psiElement1, blockName).withIcon(Symfony2Icons.TWIG_BLOCK_OVERWRITE, Symfony2Icons.TWIG_BLOCK_OVERWRITE)
                        ).collect(Collectors.toList()));

                        gotoRelatedItems.addAll(TwigBlockUtil.getBlockImplementationTargets(childrenOfType).stream().map((Function<PsiElement, GotoRelatedItem>) psiElement1 ->
                            new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(psiElement1, blockName).withIcon(Symfony2Icons.TWIG_BLOCK_OVERWRITE, Symfony2Icons.TWIG_BLOCK_OVERWRITE)
                        ).collect(Collectors.toList()));
                    }
                }
            }

            // "include" and other file tags
            TwigTagWithFileReference twigTagWithFileReference = PsiTreeUtil.getParentOfType(psiElement, TwigTagWithFileReference.class);
            if (twigTagWithFileReference != null) {
                visitFileReferenceElement(psiElement.getProject(), gotoRelatedItems, twigTagWithFileReference);
            }

            // "embed" tag
            PsiElement parentOfType1 = PsiElementUtils.getParentOfType(psiElement, TwigElementTypes.EMBED_STATEMENT);
            if (parentOfType1 != null) {
                PsiElement embedTag = PsiElementUtils.getChildrenOfType(parentOfType1, PlatformPatterns.psiElement().withElementType(TwigElementTypes.EMBED_TAG));
                if (embedTag != null) {
                    visitFileReferenceElement(psiElement.getProject(), gotoRelatedItems, embedTag);
                }
            }
        }

        return gotoRelatedItems;
    }

    private void visitFileReferenceElement(@NotNull Project project, @NotNull List<GotoRelatedItem> gotoRelatedItems, @NotNull PsiElement psiElement) {
        TwigUtil.visitTemplateIncludes(psiElement, templateInclude -> {
            for (PsiFile templatePsiElement : TwigUtil.getTemplatePsiElements(project, templateInclude.getTemplateName())) {
                gotoRelatedItems.add(new RelatedPopupGotoLineMarker.PopupGotoRelatedItem(templatePsiElement, templateInclude.getType().toString().toLowerCase()).withIcon(Symfony2Icons.TWIG_BLOCK_OVERWRITE, Symfony2Icons.TWIG_BLOCK_OVERWRITE));
            }
        });
    }
}
