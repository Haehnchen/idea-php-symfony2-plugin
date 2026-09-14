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
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigExtendsGenerator : CodeInsightAction() {
    override fun getHandler(): CodeInsightActionHandler = MyCodeInsightActionHandler()

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile) =
        Symfony2ProjectComponent.isEnabled(project) &&
            (file is TwigFile ||
                file is HtmlFileImpl && file.name.endsWith(".twig", ignoreCase = true) ||
                TwigUtil.getInjectedTwigElement(file, editor) != null)

    private class MyCodeInsightActionHandler : CodeInsightActionHandler {
        override fun invoke(project: Project, editor: Editor, file: PsiFile) {
            if (TwigUtil.getInjectedTwigElement(file, editor) == null) {
                return
            }
            val prioritizedKeys = TwigUtil.getExtendsTemplateUsageAsOrderedList(project)

            if (prioritizedKeys.isEmpty()) {
                IdeHelper.showErrorHintIfAvailable(editor, "No extends found")
                return
            }

            JBPopupFactory.getInstance().createPopupChooserBuilder(prioritizedKeys)
                .setTitle("Symfony: Twig Extends")
                .setItemsChosenCallback { strings ->
                    try {
                        WriteCommandAction.writeCommandAction(editor.project)
                            .withName("Twig Extends")
                            .run(ThrowableRunnable {
                                val content = strings.joinToString("\n") { string ->
                                    "{% extends '$string' %}"
                                }

                                PhpInsertHandlerUtil.insertStringAtCaret(editor, content)
                            })
                    } catch (_: Throwable) {
                    }
                }
                .createPopup()
                .showInBestPositionFor(editor)
        }
    }
}
