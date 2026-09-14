package fr.adrienbrault.idea.symfony2plugin.form.action.generator

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.form.FormUnderscoreMethodReference
import fr.adrienbrault.idea.symfony2plugin.form.util.FormOptionsUtil
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
import fr.adrienbrault.idea.symfony2plugin.util.IdeHelper
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class FormBuilderFieldGeneratorAction : CodeInsightAction() {
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile) =
        file is PhpFile &&
            Symfony2ProjectComponent.isEnabled(project) &&
            PsiTreeUtil.findElementOfClassAtOffset(
                file,
                editor.caretModel.offset,
                Method::class.java,
                false
            )?.let { method ->
                method.name == "buildForm" &&
                    PhpElementsUtil.isMethodInstanceOf(
                        method,
                        "\\Symfony\\Component\\Form\\FormTypeInterface",
                        "buildForm"
                    )
            } == true

    override fun getHandler(): CodeInsightActionHandler = MyCodeInsightActionHandler()

    private class MyCodeInsightActionHandler : CodeInsightActionHandler {
        override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
            val method = PsiTreeUtil.findElementOfClassAtOffset(
                psiFile,
                editor.caretModel.offset,
                Method::class.java,
                false
            ) ?: return

            val phpClass = FormOptionsUtil.getFormPhpClassFromContext(method)
            if (phpClass == null) {
                IdeHelper.showErrorHintIfAvailable(editor, "No data_class option context found")
                return
            }

            val jbFormFieldItems = buildList {
                FormUnderscoreMethodReference.visitPropertyPath(phpClass) { (key, phpNamedElement) ->
                    add(JBFormFieldItem(key, phpNamedElement))
                }
            }

            JBPopupFactory.getInstance().createPopupChooserBuilder(jbFormFieldItems)
                .setTitle("Symfony: Select Fields")
                .setItemsChosenCallback { strings ->
                    WriteCommandAction.runWriteCommandAction(project, "", null, {
                        insertSelectedNamedElements(project, method, editor, strings)
                    })
                }
                .createPopup()
                .showInBestPositionFor(editor)
        }

        private data class JBFormFieldItem(val key: String, val phpNamedElement: PhpNamedElement) {
            override fun toString() = key
        }

        private fun insertSelectedNamedElements(
            project: Project,
            method: Method,
            editor: Editor,
            strings: Collection<JBFormFieldItem>
        ) {
            val formBuilderVariable = findFormBuilderVariable(project, method) ?: return
            val instance = PhpIndex.getInstance(project)

            val content = buildString {
                for (field in strings) {
                    val (guessedTypeClass, options) = FormUtil.getGuessedFormFieldParameters(
                        instance,
                        project,
                        field.key,
                        field.phpNamedElement
                    )

                    append("\$$formBuilderVariable->add('${field.key}'")

                    guessedTypeClass?.let { typeClass ->
                        append(", ")
                        append(PhpElementsUtil.insertUseIfNecessary(method, typeClass))
                        append("::class")
                    }

                    if (options.isNotEmpty()) {
                        append(", [")
                        append(options.entries.joinToString(", ") { (key, value) ->
                            if (key == "class") {
                                val classUse = PhpElementsUtil.insertUseIfNecessary(method, value)
                                "'$key' => $classUse::class"
                            } else {
                                "'$key' => $value"
                            }
                        })
                        append("]")
                    }

                    append(");\n")
                }
            }

            val caretModel = editor.caretModel.offset

            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

            PhpInsertHandlerUtil.insertStringAtCaret(editor, content)

            CodeStyleManager.getInstance(project)
                .reformatText(method.containingFile, caretModel, caretModel + content.length)
            PsiDocumentManager.getInstance(project).commitDocument(editor.document)
        }

        private fun findFormBuilderVariable(project: Project, method: Method) =
            method.parameters
                .firstOrNull { parameter ->
                    parameter.type.isConvertibleFrom(
                        project,
                        PhpType().add("\\Symfony\\Component\\Form\\FormBuilderInterface")
                    )
                }
                ?.name
    }
}
