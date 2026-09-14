package fr.adrienbrault.idea.symfony2plugin.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTokenType
import com.jetbrains.twig.TwigFile
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.dict.TranslationFileModel
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import fr.adrienbrault.idea.symfony2plugin.translation.form.TranslatorKeyExtractorDialog
import fr.adrienbrault.idea.symfony2plugin.translation.util.TranslationInsertUtil
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import java.awt.Dimension

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigExtractLanguageAction : DumbAwareAction(
    "Extract Translation",
    "Extract Translation Key",
    Symfony2Icons.SYMFONY
) {
    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        setStatus(event, false)

        if (!Symfony2ProjectComponent.isEnabled(event.project)) {
            return
        }

        val psiFile = event.getData(PlatformDataKeys.PSI_FILE)
        if (psiFile !is TwigFile) {
            return
        }

        val editor = event.getData(PlatformDataKeys.EDITOR) ?: return

        // find valid PsiElement context, because only html text is a valid extractor action
        val psiElement = if (editor.selectionModel.hasSelection()) {
            psiFile.findElementAt(editor.selectionModel.selectionStart)
        } else {
            psiFile.findElementAt(editor.caretModel.offset)
        }

        if (psiElement == null) return

        // <a title="TEXT">TEXT</a>
        val elementType = psiElement.node.elementType
        setStatus(
            event,
            elementType === XmlTokenType.XML_DATA_CHARACTERS ||
                    elementType === XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN
        )
    }

    private fun setStatus(event: AnActionEvent, status: Boolean) {
        event.presentation.isVisible = status
        event.presentation.isEnabled = status
    }

    override fun actionPerformed(event: AnActionEvent) {
        val editor = event.getData(PlatformDataKeys.EDITOR) ?: return

        val psiFile = event.getData(PlatformDataKeys.PSI_FILE)
        if (psiFile !is TwigFile) {
            return
        }

        val project = psiFile.project
        val caretOffset = editor.caretModel.offset

        val (startOffset, endOffset, translationText) = findTranslationSelection(psiFile, editor) ?: return

        val domainNames = TranslationUtil.getTranslationDomainLookupElements(project)
            .map { it.lookupString }
            .toSortedSet()

        // get default domain on twig tag
        // also pipe it to insert handler; to append it as parameter

        // scope to search translation domain
        val transDefaultScope = psiFile.findElementAt(caretOffset) ?: psiFile

        val element = TwigUtil.getElementOnTwigViewProvider(transDefaultScope)
        val twigFileDomainScope = TwigUtil.getTwigFileDomainScope(element ?: transDefaultScope)

        // we want to have mostly used domain preselected
        val defaultDomain = twigFileDomainScope.defaultDomain
        val reselectedDomain = twigFileDomainScope.domain

        val defaultKey = translationText
            .takeIf { it.length < 15 }
            ?.lowercase()
            ?.replace(" ", ".")

        val extractorDialog = TranslatorKeyExtractorDialog(
            project,
            psiFile,
            domainNames,
            defaultKey,
            reselectedDomain,
            MyOnOkCallback(project, editor, defaultDomain, startOffset, endOffset, translationText)
        )

        extractorDialog.title = "Symfony: Extract Translation Key"
        extractorDialog.minimumSize = Dimension(600, 200)
        extractorDialog.pack()
        extractorDialog.setLocationRelativeTo(editor.component)
        extractorDialog.isVisible = true
        extractorDialog.setIconImage(Symfony2Icons.getImage(Symfony2Icons.SYMFONY))
    }

    private fun findTranslationSelection(psiFile: TwigFile, editor: Editor): TranslationSelection? {
        val selectedText = editor.selectionModel.selectedText
        if (selectedText != null) {
            return TranslationSelection(
                editor.selectionModel.selectionStart,
                editor.selectionModel.selectionEnd,
                selectedText
            )
        }

        // use dont selected text, so find common PsiElement
        val psiElement = psiFile.findElementAt(editor.caretModel.offset) ?: return null
        val elementType = psiElement.node.elementType
        if (elementType !== XmlTokenType.XML_DATA_CHARACTERS && elementType !== XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN) {
            return null
        }

        return TranslationSelection(psiElement.textRange.startOffset, psiElement.textRange.endOffset, psiElement.text)
    }

    private data class TranslationSelection(val startOffset: Int, val endOffset: Int, val text: String)

    private data class MyOnOkCallback(
        val project: Project,
        val editor: Editor,
        val finalDefaultDomain: String,
        val finalStartOffset: Int,
        val finalEndOffset: Int,
        val finalTranslationText: String
    ) : TranslatorKeyExtractorDialog.OnOkCallback {
        override fun onClick(
            files: MutableList<TranslationFileModel>,
            keyName: String,
            domain: String,
            navigateTo: Boolean
        ) {
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

            // insert Twig trans key
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction {
                    val insertString = if (finalDefaultDomain == domain) {
                        "{{ '$keyName'|trans }}"
                    } else {
                        "{{ '$keyName'|trans({}, '$domain') }}"
                    }

                    editor.document.replaceString(finalStartOffset, finalEndOffset, insertString)
                    editor.caretModel.moveToOffset(finalEndOffset)
                }
            }, "Twig Translation Insert $keyName", null)

            val targets = ArrayList<PsiElement>()

            // so finally insert it; first file can be a navigation target
            for (transPsiFile in files) {
                val file = transPsiFile.psiFile

                CommandProcessor.getInstance().executeCommand(file.project, {
                    ApplicationManager.getApplication().runWriteAction {
                        val target = TranslationInsertUtil.invokeTranslation(file, keyName, finalTranslationText)
                        if (target != null) {
                            targets.add(target)
                        }
                    }
                }, "Translation Insert ${file.name}", null)
            }

            if (navigateTo && targets.isNotEmpty()) {
                PsiDocumentManager.getInstance(project).commitAndRunReadAction {
                    IdeHelper.navigateToPsiElement(targets.first())
                }
            }
        }
    }
}
