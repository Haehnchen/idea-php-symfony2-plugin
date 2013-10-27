package fr.adrienbrault.idea.symfony2plugin.templating;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CompletionPhase;
import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.impl.CompletionServiceImpl;
import com.intellij.codeInsight.editorActions.CompletionAutoPopupHandler;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.ide.PowerSaveMode;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.twig.TwigFile;
import com.jetbrains.twig.TwigTokenTypes;
import fr.adrienbrault.idea.symfony2plugin.TwigHelper;
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper;
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorkaroundTwigTypedHandler extends TypedHandlerDelegate {

    @Override
    public Result charTyped(char c, Project project, Editor editor, @NotNull PsiFile file) {

        if (!(file instanceof TwigFile)) {
            return TypedHandlerDelegate.Result.CONTINUE;
        }

        /*
        if(c == ' ' && PluginManager.getPlugin(PluginId.getId("com.jetbrains.twig")) != null) {
            PsiElement psiElement = file.findElementAt(editor.getCaretModel().getOffset());
            if(psiElement == null || TwigHelper.getBlockTagPattern().accepts(psiElement)) {
                return null;
            }

            scheduleAutoPopup(project, editor, null);
        }
        */

        if ((c != '|')) {
            return TypedHandlerDelegate.Result.CONTINUE;
        }

        PsiElement psiElement = file.findElementAt(editor.getCaretModel().getOffset());
        if(psiElement == null || PlatformPatterns.psiElement(TwigTokenTypes.STRING_TEXT).accepts(psiElement)) {
            return null;
        }

        scheduleAutoPopup(project, editor, null);

        return TypedHandlerDelegate.Result.CONTINUE;
    }

    /**
     * PhpTypedHandler.scheduleAutoPopup but use SMART since BASIC is blocked
     */
    public void scheduleAutoPopup(final Project project, final Editor editor, @Nullable final Condition<PsiFile> condition) {
        if (ApplicationManager.getApplication().isUnitTestMode() && !CompletionAutoPopupHandler.ourTestingAutopopup) {
            return;
        }

        if (!CodeInsightSettings.getInstance().AUTO_POPUP_COMPLETION_LOOKUP) {
            return;
        }
        if (PowerSaveMode.isEnabled()) {
            return;
        }

        if (!CompletionServiceImpl.isPhase(CompletionPhase.CommittingDocuments.class, CompletionPhase.NoCompletion.getClass())) {
            return;
        }

        final CompletionProgressIndicator currentCompletion = CompletionServiceImpl.getCompletionService().getCurrentCompletion();
        if (currentCompletion != null) {
            currentCompletion.closeAndFinish(true);
        }

        final CompletionPhase.CommittingDocuments phase = new CompletionPhase.CommittingDocuments(null, editor);
        CompletionServiceImpl.setCompletionPhase(phase);

        CompletionAutoPopupHandler.runLaterWithCommitted(project, editor.getDocument(), new Runnable() {
            @Override
            public void run() {
                CompletionAutoPopupHandler.invokeCompletion(CompletionType.SMART, true, project, editor, 0, false);
            }
        });
    }

}
