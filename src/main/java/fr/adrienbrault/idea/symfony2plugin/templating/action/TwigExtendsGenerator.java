package fr.adrienbrault.idea.symfony2plugin.templating.action;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import com.intellij.util.ThrowableRunnable;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigExtendsGenerator extends CodeInsightAction {
    @Override
    protected @NotNull
    CodeInsightActionHandler getHandler() {
        return new TwigExtendsGenerator.MyCodeInsightActionHandler();
    }

    @Override
    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return Symfony2ProjectComponent.isEnabled(project) && (
            file instanceof TwigFile
                || (file instanceof HtmlFileImpl && file.getName().toLowerCase().endsWith(".twig"))
                || TwigUtil.getInjectedTwigElement(file, editor) != null
        );
    }

    private static class MyCodeInsightActionHandler implements CodeInsightActionHandler {
        @Override
        public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
            PsiElement psiElement = TwigUtil.getInjectedTwigElement(file, editor);
            if(psiElement == null) {
                return;
            }

            List<String> prioritizedKeys = TwigUtil.getExtendsTemplateUsageAsOrderedList(project);

            if (prioritizedKeys.size() == 0) {
                if (!ApplicationManager.getApplication().isHeadlessEnvironment()) {
                    HintManager.getInstance().showErrorHint(editor, "No extends found");
                }

                return;
            }

            JBPopupFactory.getInstance().createPopupChooserBuilder(prioritizedKeys)
                .setTitle("Symfony: Twig Extends")
                .setItemsChosenCallback(strings -> {
                    try {
                        WriteCommandAction.writeCommandAction(editor.getProject())
                            .withName("Twig Extends")
                            .run((ThrowableRunnable<Throwable>) () -> {
                                String content = strings.stream()
                                    .map((Function<String, String>) s -> "{% extends '" + s + "' %}")
                                    .collect(Collectors.joining("\n"));

                                PhpInsertHandlerUtil.insertStringAtCaret(editor, content);
                            });
                    } catch (Throwable ignored) {
                    }
                })
                .createPopup()
                .showInBestPositionFor(editor);
        }
    }
}
