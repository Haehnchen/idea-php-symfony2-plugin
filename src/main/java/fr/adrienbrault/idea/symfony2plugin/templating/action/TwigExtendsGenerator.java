package fr.adrienbrault.idea.symfony2plugin.templating.action;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigExtendsStubIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
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

            Set<String> allKeys = FileBasedIndex.getInstance().getAllKeys(TwigExtendsStubIndex.KEY, project)
                .stream()
                .filter(s -> !s.toLowerCase().contains("@webprofiler") && !s.toLowerCase().contains("/profiler/") && !s.toLowerCase().contains("@twig") && !s.equalsIgnoreCase("form_div_layout.html.twig"))
                .collect(Collectors.toSet());

            Map<String, Integer> extendsWithFileCountUsage = new HashMap<>();
            for (String allKey : allKeys) {
                Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance().getContainingFiles(TwigExtendsStubIndex.KEY, allKey, GlobalSearchScope.allScope(project));
                extendsWithFileCountUsage.put(allKey, containingFiles.size());
            }

            List<String> prioritizedKeys = extendsWithFileCountUsage.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .limit(40)
                .collect(Collectors.toList());

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
