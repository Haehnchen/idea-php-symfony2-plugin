package fr.adrienbrault.idea.symfony2plugin.documentation

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.platform.backend.documentation.DocumentationLinkHandler
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.LinkResolveResult
import com.intellij.util.concurrency.AppExecutorUtil
import fr.adrienbrault.idea.symfony2plugin.routing.documentation.ROUTE_FIND_USAGES_LINK
import fr.adrienbrault.idea.symfony2plugin.routing.documentation.RouteDocumentationTarget
import fr.adrienbrault.idea.symfony2plugin.templating.documentation.TEMPLATE_FIND_USAGES_LINK
import fr.adrienbrault.idea.symfony2plugin.templating.documentation.TwigTemplateDocumentationTarget

/**
 * Opens Find Usages from documentation links.
 */
class DocumentationFindUsagesLinkHandler : DocumentationLinkHandler {
    override fun resolveLink(target: DocumentationTarget, url: String): LinkResolveResult? {
        val contextProvider = when (url) {
            ROUTE_FIND_USAGES_LINK if target is RouteDocumentationTarget -> routeContextProvider(target)
            TEMPLATE_FIND_USAGES_LINK if target is TwigTemplateDocumentationTarget -> twigTemplateContextProvider(target)
            else -> return null
        }

        invokeFindUsages(contextProvider)
        return LinkResolveResult.resolvedTarget(target)
    }

    private fun routeContextProvider(target: RouteDocumentationTarget): () -> DataContext? {
        val pointer = target.createPointer()
        return { pointer.dereference()?.findUsagesContext() }
    }

    private fun twigTemplateContextProvider(target: TwigTemplateDocumentationTarget): () -> DataContext? {
        val pointer = target.createPointer()
        return { pointer.dereference()?.findUsagesContext() }
    }

    private fun invokeFindUsages(contextProvider: () -> DataContext?) {
        ReadAction.nonBlocking<DataContext?> { contextProvider() }
            .finishOnUiThread(ModalityState.nonModal()) { context ->
                val project = context?.getData(CommonDataKeys.PROJECT) ?: return@finishOnUiThread
                if (project.isDisposed) {
                    return@finishOnUiThread
                }

                val actionContext = SimpleDataContext.builder()
                    .setParent(context)
                    .add(CommonDataKeys.EDITOR, FileEditorManager.getInstance(project).selectedTextEditor)
                    .add(PlatformDataKeys.CONTEXT_COMPONENT, WindowManager.getInstance().getIdeFrame(project)?.component)
                    .build()
                val action = ActionManager.getInstance().getAction(IdeActions.ACTION_FIND_USAGES)
                val event = AnActionEvent.createEvent(action, actionContext, null, "SymfonyDocumentation", ActionUiKind.POPUP, null)
                ActionUtil.performAction(action, event)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }
}
