package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigBlockUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.ProjectUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import icons.TwigIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Provides completion and navigation for block names in AbstractController::renderBlock() and renderBlockView() calls.
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TemplateRenderBlockCompletionContributor {

    /**
     * $this->renderBlock('foo.html.twig', 'the_block', $context);
     */
    public static final MethodMatcher.CallToSignature[] CALL_TO_SIGNATURES = {
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "renderBlock"),
        new MethodMatcher.CallToSignature("\\Symfony\\Bundle\\FrameworkBundle\\Controller\\AbstractController", "renderBlockView")
    };

    public static class Completion extends CompletionContributor {

        public Completion() {
            extend(CompletionType.BASIC, PhpElementsUtil.getParameterInsideMethodReferencePattern(), new CompletionProvider<>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                    PsiElement psiElement = parameters.getOriginalPosition();
                    if (!(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression)) {
                        return;
                    }

                    int currentIndex = PsiElementUtils.getParameterIndexValue(stringLiteralExpression);
                    if (currentIndex != 1) {
                        return;
                    }

                    MethodReference parentOfType = PsiTreeUtil.getParentOfType(stringLiteralExpression, MethodReference.class);
                    if (!PhpElementsUtil.isMethodReferenceInstanceOf(parentOfType, CALL_TO_SIGNATURES)) {
                        return;
                    }

                    String templateName = PhpElementsUtil.getMethodReferenceStringValueParameter(parentOfType, 0);
                    if (StringUtils.isBlank(templateName)) {
                        return;
                    }

                    Project project = parentOfType.getProject();

                    Set<VirtualFile> parentFiles = new HashSet<>();
                    for (VirtualFile templateFile : TwigUtil.getTemplateFiles(project, templateName)) {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(templateFile);
                        if (psiFile != null) {
                            parentFiles.addAll(TwigFileUtil.collectParentFiles(true, psiFile));
                        }
                    }

                    Set<String> blockNames = new HashSet<>();
                    TwigUtil.getBlockNamesForFiles(project, parentFiles).values()
                        .forEach(blockNames::addAll);

                    for (String blockName : blockNames) {
                        result.addElement(LookupElementBuilder.create(blockName).withIcon(TwigIcons.TwigFileIcon));
                    }
                }
            });
        }
    }

    public static class GotoDeclaration implements GotoDeclarationHandler {
        @Override
        public PsiElement @Nullable [] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int offset, Editor editor) {
            if (psiElement == null || !(psiElement.getParent() instanceof StringLiteralExpression stringLiteralExpression) || !PhpElementsUtil.getParameterInsideMethodReferencePattern().accepts(psiElement)) {
                return null;
            }

            String contents = stringLiteralExpression.getContents();
            if (StringUtils.isBlank(contents)) {
                return null;
            }

            int currentIndex = PsiElementUtils.getParameterIndexValue(stringLiteralExpression);
            if (currentIndex != 1) {
                return null;
            }

            MethodReference parentOfType = PsiTreeUtil.getParentOfType(stringLiteralExpression, MethodReference.class);
            if (!PhpElementsUtil.isMethodReferenceInstanceOf(parentOfType, CALL_TO_SIGNATURES)) {
                return null;
            }

            String templateName = PhpElementsUtil.getMethodReferenceStringValueParameter(parentOfType, 0);
            if (StringUtils.isBlank(templateName)) {
                return null;
            }

            Set<PsiElement> targets = new HashSet<>();

            Project project = psiElement.getProject();

            for (VirtualFile templateFile : TwigUtil.getTemplateFiles(project, templateName)) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(templateFile);
                if (psiFile == null) {
                    continue;
                }

                targets.addAll(TwigBlockUtil.getBlockOverwriteTargets(psiFile, contents, true));
            }

            if (targets.size() <= 1) {
                return targets.toArray(new PsiElement[0]);
            }

            VirtualFile projectDir = ProjectUtil.getProjectDir(psiElement);
            return targets.stream()
                .map(element -> new TwigBlockFakePsiNavigationItem(projectDir, element))
                .toArray(PsiElement[]::new);
        }
    }
}
