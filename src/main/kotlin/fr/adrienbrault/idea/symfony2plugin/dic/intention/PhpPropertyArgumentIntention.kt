package fr.adrienbrault.idea.symfony2plugin.dic.intention

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.IntentionAndQuickFixAction
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ThrowableRunnable
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.findUsages.PhpGotoTargetRendererProvider
import com.jetbrains.php.lang.psi.elements.FieldReference
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.completion.ServicePropertyInsertUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import icons.SymfonyIcons
import org.apache.commons.lang3.StringUtils
import java.awt.Component
import javax.swing.Icon
import javax.swing.JList
import javax.swing.ListCellRenderer

private class DelegatedNamedElementCellRenderer(
    private val project: Project,
) : ListCellRenderer<String> {
    private val delegatedRenderer = PhpGotoTargetRendererProvider.PhpNamedElementPsiElementListCellRenderer(false)

    override fun getListCellRendererComponent(
        list: JList<out String>,
        value: String,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean,
    ): Component {
        return delegatedRenderer.getListCellRendererComponent(
            list,
            PhpElementsUtil.getClassInterface(project, java.lang.String.valueOf(value)),
            index,
            isSelected,
            cellHasFocus,
        )
    }
}

private fun buildProperty(project: Project, fieldReference: FieldReference, classFqn: String) {
    try {
        val phpClassScope = PsiTreeUtil.getParentOfType(fieldReference, PhpClass::class.java)
        if (phpClassScope == null || !ServiceUtil.isPhpClassAService(phpClassScope)) {
            return
        }

        WriteCommandAction.writeCommandAction(project)
            .withName("Symfony: Add Property Service")
            .run(
                ThrowableRunnable {
                    ServicePropertyInsertUtil.appendPropertyInjection(phpClassScope, fieldReference.name!!, classFqn)
                },
            )
    } catch (_: Throwable) {
    }
}

private fun getElement(editor: Editor, file: PsiFile): FieldReference? {
    val caretModel = editor.caretModel

    val position = caretModel.offset
    val elementAt = file.findElementAt(position)
        ?: return null

    val parent = elementAt.parent
    return parent as? FieldReference
}

open class PhpPropertyArgumentIntention : IntentionAndQuickFixAction(), Iconable, HighPriorityAction {
    @IntentionName
    override fun getName(): String {
        return "Symfony: Add Property Service"
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    @IntentionFamilyName
    override fun getFamilyName(): String {
        return name
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (editor == null) {
            return false
        }

        val fieldReference = getElement(editor, file)
        if (fieldReference != null) {
            val name = fieldReference.name
            if (name != null && name.length > 2) {
                val classReference1 = fieldReference.classReference
                if (classReference1 != null && "this" == classReference1.name) {
                    val phpClassScope = PsiTreeUtil.getParentOfType(fieldReference, PhpClass::class.java)
                    if (phpClassScope != null) {
                        if (phpClassScope.findFieldByName(name, false) == null) {
                            return ServiceUtil.isPhpClassAService(phpClassScope)
                        }
                    }
                }
            }
        }

        return false
    }

    override fun applyFix(project: Project, file: PsiFile, editor: Editor?) {
        if (editor == null) {
            return
        }

        val fieldReference = getElement(editor, file)
            ?: return

        val name = fieldReference.name
            ?: return

        var methodName: String? = null
        val parent = fieldReference.parent
        if (parent is MethodReference) {
            val name1 = parent.name
            if (StringUtils.isNotBlank(name1)) {
                methodName = name1
            }
        }

        val injectionService = ServicePropertyInsertUtil.getInjectionService(project, name, methodName)
            .map { StringUtils.stripStart(it, "\\") }

        if (injectionService.size == 1) {
            buildProperty(project, fieldReference, injectionService[0])
            return
        }

        val phpClasses = injectionService
            .map { PhpIndex.getInstance(project).getAnyByFQN(it).iterator().next() }
            .distinct()
            .map { it.fqn }

        JBPopupFactory.getInstance().createPopupChooserBuilder(phpClasses)
            .setTitle("Symfony: Property Service Suggestions")
            .setItemChosenCallback { buildProperty(project, fieldReference, it) }
            .setRenderer(DelegatedNamedElementCellRenderer(project))
            .createPopup()
            .showInBestPositionFor(editor)
    }

    override fun getIcon(flags: Int): Icon {
        return SymfonyIcons.Symfony
    }
}
