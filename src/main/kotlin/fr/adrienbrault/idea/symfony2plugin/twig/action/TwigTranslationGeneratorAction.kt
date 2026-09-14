package fr.adrienbrault.idea.symfony2plugin.twig.action

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.html.HtmlFileImpl
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil
import com.jetbrains.twig.TwigFile
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTranslationGeneratorAction : CodeInsightAction() {
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile) =
        Symfony2ProjectComponent.isEnabled(project) &&
            (file is TwigFile ||
                file is HtmlFileImpl && file.name.endsWith(".twig", ignoreCase = true) ||
                TwigUtil.getInjectedTwigElement(file, editor) != null)

    override fun getHandler(): CodeInsightActionHandler = MyCodeInsightActionHandler()

    private class MyCodeInsightActionHandler : CodeInsightActionHandler {
        override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
            val psiElement = TwigUtil.getInjectedTwigElement(psiFile, editor) ?: return

            val containingFile = psiElement.containingFile
            if (containingFile !is TwigFile) {
                return
            }

            val element = TwigUtil.getElementOnTwigViewProvider(psiElement)
            val twigFileDomainScope = TwigUtil.getTwigFileDomainScope(element ?: psiElement)

            val defaultDomain = twigFileDomainScope.defaultDomain
            val domain = twigFileDomainScope.domain

            val list = TranslationUtil.getTranslationLookupElementsOnDomain(project, domain)
                .map { it.lookupString }
                .sorted()

            JBPopupFactory.getInstance().createPopupChooserBuilder(list)
                .setTitle("Symfony: Translations \"${StringUtils.abbreviate(domain, 20)}\"")
                .setItemChosenCallback { selectedValue ->
                    WriteCommandAction.runWriteCommandAction(
                        editor.project,
                        "Symfony: Add Translation \"${StringUtils.abbreviate(selectedValue, 20)}\"",
                        null,
                        {
                            PhpInsertHandlerUtil.insertStringAtCaret(
                                editor,
                                createTranslationSnippet(selectedValue, defaultDomain, domain)
                            )
                        }
                    )
                }
                .createPopup()
                .showInBestPositionFor(editor)
        }
    }
}

fun createTranslationSnippet(translationKey: String, defaultDomain: String, domain: String) =
    if (domain != defaultDomain) {
        "{{ '$translationKey'|trans({}, '$domain') }}"
    } else {
        "{{ '$translationKey'|trans }}"
    }
