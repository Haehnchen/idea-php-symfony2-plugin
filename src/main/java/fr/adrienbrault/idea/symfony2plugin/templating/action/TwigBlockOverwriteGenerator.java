package fr.adrienbrault.idea.symfony2plugin.templating.action;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.html.HtmlFileImpl;
import com.intellij.util.ThrowableRunnable;
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil;
import com.jetbrains.twig.TwigFile;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil;
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil;
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
public class TwigBlockOverwriteGenerator extends CodeInsightAction {
    @Override
    protected @NotNull
    CodeInsightActionHandler getHandler() {
        return new MyCodeInsightActionHandler();
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

            // collect blocks in all related files
            Pair<Collection<PsiFile>, Boolean> scopedContext = TwigUtil.findScopedFile(psiElement);

            Collection<LookupElement> blockLookupElements = TwigUtil.getBlockLookupElements(
                project,
                TwigFileUtil.collectParentFiles(scopedContext.getSecond(), scopedContext.getFirst())
            );

            List<String> items = blockLookupElements.stream()
                .map(LookupElement::getLookupString)
                .distinct()
                .collect(Collectors.toList());

            if (items.size() == 0) {
                IdeHelper.showErrorHintIfAvailable(editor, "No block found");
                return;
            }

            JBPopupFactory.getInstance().createPopupChooserBuilder(items)
                .setTitle("Symfony: Twig Blocks")
                .setItemsChosenCallback(strings -> {
                    try {
                        String titleBlocks = StringUtils.abbreviate(strings.stream()
                            .map((Function<String, String>) s -> s)
                            .collect(Collectors.joining(", ")), 10);

                        WriteCommandAction.writeCommandAction(editor.getProject())
                            .withName("Block Overwrite: " + titleBlocks)
                            .run((ThrowableRunnable<Throwable>) () -> {
                                String endBlock = "{% endblock %}";

                                String content = strings.stream()
                                    .map((Function<String, String>) s -> "{% block " + s + " %}" + endBlock)
                                    .collect(Collectors.joining("\n"));

                                PhpInsertHandlerUtil.insertStringAtCaret(editor, content);

                                // move caret inside block
                                // {% block %}<caret>{% endblock %}
                                editor.getCaretModel().moveCaretRelatively(-endBlock.length(), 0, false, false, true);
                            });
                    } catch (Throwable ignored) {
                    }
                })
                .createPopup()
                .showInBestPositionFor(editor);
        }
    }
}
