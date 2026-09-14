package fr.adrienbrault.idea.symfony2plugin.templating.action

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.html.HtmlFileImpl
import com.intellij.util.ThrowableRunnable
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil
import com.jetbrains.twig.TwigFile
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.twig.utils.TwigFileUtil
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigBlockOverwriteGenerator : CodeInsightAction() {
    override fun getHandler(): CodeInsightActionHandler = MyCodeInsightActionHandler()

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile) =
        Symfony2ProjectComponent.isEnabled(project) &&
            (file is TwigFile ||
                file is HtmlFileImpl && file.name.endsWith(".twig", ignoreCase = true) ||
                TwigUtil.getInjectedTwigElement(file, editor) != null)

    private class MyCodeInsightActionHandler : CodeInsightActionHandler {
        override fun invoke(project: Project, editor: Editor, file: PsiFile) {
            val psiElement = TwigUtil.getInjectedTwigElement(file, editor) ?: return

            // collect blocks in all related files
            val scopedContext = TwigUtil.findScopedFile(psiElement)

            val blockLookupElements = TwigUtil.getBlockLookupElements(
                project,
                TwigFileUtil.collectParentFiles(scopedContext.second, scopedContext.first)
            )

            val items = blockLookupElements
                .map { it.lookupString }
                .distinct()

            if (items.isEmpty()) {
                IdeHelper.showErrorHintIfAvailable(editor, "No block found")
                return
            }

            JBPopupFactory.getInstance().createPopupChooserBuilder(items)
                .setTitle("Symfony: Twig Blocks")
                .setItemsChosenCallback { strings ->
                    try {
                        val titleBlocks = StringUtils.abbreviate(strings.joinToString(", "), 10)

                        WriteCommandAction.writeCommandAction(editor.project)
                            .withName("Block Overwrite: $titleBlocks")
                            .run(ThrowableRunnable {
                                val endBlock = "{% endblock %}"

                                val content = strings.joinToString("\n") { string ->
                                    "{% block $string %}$endBlock"
                                }

                                PhpInsertHandlerUtil.insertStringAtCaret(editor, content)

                                // move caret inside block
                                // {% block %}<caret>{% endblock %}
                                editor.caretModel.moveCaretRelatively(-endBlock.length, 0, false, false, true)
                            })
                    } catch (_: Throwable) {
                    }
                }
                .createPopup()
                .showInBestPositionFor(editor)
        }
    }
}
