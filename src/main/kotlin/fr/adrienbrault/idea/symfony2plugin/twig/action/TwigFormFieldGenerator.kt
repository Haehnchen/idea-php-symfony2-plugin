package fr.adrienbrault.idea.symfony2plugin.twig.action

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.html.HtmlFileImpl
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.completion.insert.PhpInsertHandlerUtil
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.resolve.types.PhpType
import com.jetbrains.twig.TwigFile
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigTypeResolveUtil
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable
import fr.adrienbrault.idea.symfony2plugin.templating.variable.resolver.FormFieldResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import org.apache.commons.lang3.StringUtils
import java.util.HashSet

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigFormFieldGenerator : CodeInsightAction() {
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        val isTwigContext = Symfony2ProjectComponent.isEnabled(project) && (
                file is TwigFile ||
                        file is HtmlFileImpl && file.name.endsWith(".twig", ignoreCase = true) ||
                        TwigUtil.getInjectedTwigElement(file, editor) != null
                )

        if (!isTwigContext) {
            return false
        }

        val psiElement = TwigUtil.getInjectedTwigElement(file, editor) ?: return false

        return TwigTypeResolveUtil.collectScopeVariables(psiElement).values.any { it.isFormView(project) }
    }

    override fun getHandler(): CodeInsightActionHandler = MyCodeInsightActionHandler()

    private class MyCodeInsightActionHandler : CodeInsightActionHandler {
        override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
            val psiElement = TwigUtil.getInjectedTwigElement(psiFile, editor) ?: return

            val phpClasses = buildList {
                for ((key, variable) in TwigTypeResolveUtil.collectScopeVariables(psiElement)) {
                    if (!variable.isFormView(project)) {
                        continue
                    }

                    for (formTypeFqn in variable.formTypeFqns) {
                        PhpElementsUtil.getClassInterface(project, formTypeFqn)?.let { phpClass ->
                            add(JBFormFieldItem(key, phpClass))
                        }
                    }
                }
            }

            if (phpClasses.size == 1) {
                extracted(project, editor, phpClasses.single())
                return
            }

            JBPopupFactory.getInstance().createPopupChooserBuilder(ArrayList(phpClasses))
                .setTitle("Symfony: Select FormType")
                .setItemChosenCallback { item ->
                    WriteCommandAction.runWriteCommandAction(project, "", null, {
                        extracted(project, editor, item)
                    })
                }
                .createPopup()
                .showInBestPositionFor(editor)
        }

        private fun extracted(project: Project, editor: Editor, next: JBFormFieldItem) {
            val fields = HashSet<String?>().apply {
                FormFieldResolver.visitFormReferencesFields(next.phpClass) { twigTypeContainer ->
                    add(twigTypeContainer.stringElement)
                }
            }

            JBPopupFactory.getInstance().createPopupChooserBuilder(ArrayList(fields))
                .setTitle("Symfony: Select Form Fields \"${StringUtils.abbreviate(next.phpClass.name, 20)}\"")
                .setItemsChosenCallback { strings ->
                    WriteCommandAction.runWriteCommandAction(project, "", null, {
                        val content = strings.joinToString(separator = "") { field ->
                            "{{ form_row(${next.key}.$field) }}\n"
                        }

                        PhpInsertHandlerUtil.insertStringAtCaret(editor, content)
                    })
                }
                .createPopup()
                .showInBestPositionFor(editor)
        }
    }

    data class JBFormFieldItem(val key: String, val phpClass: PhpClass) {
        override fun toString() = "$key => ${phpClass.name}"
    }
}

private fun PsiVariable.isFormView(project: Project): Boolean {
    if (formTypeFqns.isEmpty()) {
        return false
    }

    val phpType = PhpIndex.getInstance(project).completeType(
        project,
        PhpType.from(*types.toTypedArray<String>()),
        HashSet()
    )
    return FormFieldResolver.isFormView(phpType)
}
