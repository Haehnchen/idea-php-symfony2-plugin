package fr.adrienbrault.idea.symfony2plugin.navigation;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Settings;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern;
import fr.adrienbrault.idea.symfony2plugin.routing.Route;
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigFoldingBuilder extends FoldingBuilderEx {

    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement psiElement, @NotNull Document document, boolean b) {
        if (!(psiElement instanceof TwigFile)) {
            return new FoldingDescriptor[0];
        }

        Project project = psiElement.getProject();
        if (!Symfony2ProjectComponent.isEnabled(project)) {
            return new FoldingDescriptor[0];
        }

        List<FoldingDescriptor> descriptors = new ArrayList<>();

        if (Settings.getInstance(project).codeFoldingTwigRoute) {
            attachPathFoldingDescriptors(project, psiElement, descriptors);
        }

        if (Settings.getInstance(project).codeFoldingTwigTemplate) {
            attachTemplateFoldingDescriptors(psiElement, descriptors);
        }

        if (Settings.getInstance(project).codeFoldingTwigConstant) {
            attachConstantFoldingDescriptors(psiElement, descriptors);
        }

        return descriptors.toArray(new FoldingDescriptor[0]);
    }

    private void attachTemplateFoldingDescriptors(PsiElement psiElement, List<FoldingDescriptor> descriptors) {
        // find path calls in file
        PsiElement[] fileReferences = PsiTreeUtil.collectElements(psiElement, psiElement1 ->
            TwigPattern.getTemplateFileReferenceTagPattern().accepts(psiElement1) || TwigPattern.getFormThemeFileTagPattern().accepts(psiElement1)
        );

        for(PsiElement fileReference: fileReferences) {
            final String templateShortcutName = TwigUtil.getFoldingTemplateName(fileReference.getText());
            if(templateShortcutName != null) {
                descriptors.add(new FoldingDescriptor(fileReference.getNode(),
                    new TextRange(fileReference.getTextRange().getStartOffset(), fileReference.getTextRange().getEndOffset())) {
                    @Override
                    public String getPlaceholderText() {
                        return templateShortcutName;
                    }
                });
            }
        }

    }

    private void attachPathFoldingDescriptors(@NotNull Project project, @NotNull PsiElement psiElement, @NotNull List<FoldingDescriptor> descriptors) {

        // find path calls in file
        PsiElement[] psiElements = PsiTreeUtil.collectElements(psiElement, psiElement12 ->
            TwigPattern.getAutocompletableRoutePattern().accepts(psiElement12)
        );

        if(psiElements.length == 0) {
            return;
        }

        Map<String,Route> routes = null;
        for(PsiElement psiElement1: psiElements) {

            // cache routes if we need them
            if(routes == null) {
                routes = RouteHelper.getAllRoutes(project);
            }

            String contents = PsiElementUtils.trimQuote(psiElement1.getText());
            if(contents.length() > 0 && routes.containsKey(contents)) {
                final Route route = routes.get(contents);

                final String url = RouteHelper.getRouteUrl(route);
                if(url != null) {
                    descriptors.add(new FoldingDescriptor(psiElement1.getNode(),
                        new TextRange(psiElement1.getTextRange().getStartOffset(), psiElement1.getTextRange().getEndOffset())) {
                        @Nullable
                        @Override
                        public String getPlaceholderText() {
                            return url;
                        }
                    });
                }
            }

        }

    }

    private void attachConstantFoldingDescriptors(PsiElement psiElement, List<FoldingDescriptor> descriptors) {
        // find path calls in file
        PsiElement[] constantReferences = PsiTreeUtil.collectElements(psiElement, psiElement1 ->
            TwigPattern.getPrintBlockOrTagFunctionPattern("constant").accepts(psiElement1)
        );

        if(constantReferences.length == 0) {
            return;
        }

        for(PsiElement fileReference: constantReferences) {
            String contents = fileReference.getText();
            if(StringUtils.isNotBlank(contents) && contents.contains(":")) {
                final String[] parts = contents.split("::");
                if(parts.length == 2) {
                    descriptors.add(new FoldingDescriptor(fileReference.getNode(),
                        new TextRange(fileReference.getTextRange().getStartOffset(), fileReference.getTextRange().getEndOffset())) {
                        @Nullable
                        @Override
                        public String getPlaceholderText() {
                            return parts[1];
                        }
                    });
                }

            }

        }

    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode astNode) {
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
        return true;
    }
}
