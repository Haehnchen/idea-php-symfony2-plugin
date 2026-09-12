package fr.adrienbrault.idea.symfony2plugin.routing.documentation

import com.intellij.model.Pointer
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.usages.UsageView
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteUsageTargetProvider
import com.intellij.openapi.project.DumbService
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.routing.documentation.RouteDocumentationTargetProvider.RouteDocumentationContext
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TwigRouteUsageStubIndex

class RouteDocumentationTarget(private val context: RouteDocumentationContext) : DocumentationTarget {
    override fun createPointer(): Pointer<RouteDocumentationTarget> {
        val pointer = SmartPointerManager.createPointer(context.anchor)

        return Pointer {
            val anchor = pointer.element ?: return@Pointer null
            val restored = RouteDocumentationTargetProvider().resolveContext(anchor.containingFile, anchor.textOffset) ?: return@Pointer null
            RouteDocumentationTarget(restored)
        }
    }

    override val navigatable: Navigatable?
        get() {
            val anchor = context.anchor
            if (!anchor.isValid || DumbService.isDumb(anchor.project)) {
                return null
            }

            return RouteHelper.getRouteNameTarget(anchor.project, context.routeName)
                .singleOrNull()?.navigationElement as? Navigatable
        }

    override fun computePresentation(): TargetPresentation = TargetPresentation.builder(context.routeName)
        .icon(Symfony2Icons.ROUTE)
        .locationText((navigatable as? PsiElement)?.containingFile?.name)
        .presentation()

    override fun computeDocumentationHint(): String? = documentation()

    override fun computeDocumentation(): DocumentationResult? = documentation()?.let { DocumentationResult.documentation(it) }

    private fun documentation(): String? {
        val anchor = context.anchor
        if (!anchor.isValid) {
            return null
        }

        val current = RouteDocumentationTargetProvider().resolveContext(anchor.containingFile, anchor.textOffset) ?: return null
        val routes = RouteHelper.getRoute(anchor.project, current.routeName)

        if (routes.isEmpty()) {
            return null
        }

        val twigTemplates = FileBasedIndex.getInstance().getContainingFiles(
            TwigRouteUsageStubIndex.KEY, current.routeName, GlobalSearchScope.projectScope(anchor.project)
        ).size

        return renderRouteDocumentation(routes, twigTemplates)
    }

    internal fun findUsagesContext(): DataContext? {
        val anchor = context.anchor
        if (!anchor.isValid) {
            return null
        }

        val current = RouteDocumentationTargetProvider().resolveContext(anchor.containingFile, anchor.textOffset) ?: return null
        val targets = RouteUsageTargetProvider().getTargets(current.anchor)
        if (targets.isEmpty()) {
            return null
        }

        return SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, anchor.project)
            .add(CommonDataKeys.PSI_ELEMENT, current.anchor)
            .add(CommonDataKeys.PSI_FILE, current.anchor.containingFile)
            .add(UsageView.USAGE_TARGETS_KEY, targets)
            .build()
    }
}

internal const val ROUTE_FIND_USAGES_LINK = "symfony-route-find-usages"
