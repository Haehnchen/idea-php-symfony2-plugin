package fr.adrienbrault.idea.symfony2plugin.routing.documentation

import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.twig.TwigLanguage
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.routing.PhpRouteReferenceContributor
import fr.adrienbrault.idea.symfony2plugin.routing.Route
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.routing.usages.RouteUsageUtil
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher
import java.util.Locale

class RouteDocumentationTargetProvider : DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        val context = resolveContext(file, offset) ?: return emptyList()
        if (RouteHelper.getRoute(file.project, context.routeName).isEmpty()) {
            return emptyList()
        }

        return listOf(RouteDocumentationTarget(context))
    }

    internal fun resolveContext(file: PsiFile, offset: Int): RouteDocumentationContext? {
        if (!file.isValid || !Symfony2ProjectComponent.isEnabled(file.project) || DumbService.isDumb(file.project)) {
            return null
        }

        if (file is PhpFile) {
            return file.findElementAt(offset)?.let(::php)
        }

        val twigFile = file.viewProvider.getPsi(TwigLanguage.INSTANCE) ?: return null

        return twigFile.findElementAt(offset)?.let(::twig)
    }

    private fun php(leaf: PsiElement): RouteDocumentationContext? {
        val literal = PsiTreeUtil.getParentOfType(leaf, StringLiteralExpression::class.java, false) ?: return null

        // generateUrl('app_<caret>blog') / redirectToRoute(route: 'app_<caret>blog')
        MethodMatcher.getMatchedSignatureWithDepth(literal, PhpRouteReferenceContributor.GENERATOR_SIGNATURES)
            ?: return null

        return RouteDocumentationContext(literal.contents, literal)
    }

    private fun twig(leaf: PsiElement): RouteDocumentationContext? {
        // {{ path(<caret>'app_blog') }} / {{ url('app_blog<caret>') }}
        val element = RouteUsageUtil.getCandidateElements(leaf).firstOrNull {
            TwigPattern.getAutocompletableRoutePattern().accepts(it)
                && TwigUtil.isValidStringWithoutInterpolatedOrConcat(it)
        } ?: return null

        return RouteDocumentationContext(element.text, element)
    }

    data class RouteDocumentationContext(
        val routeName: String,
        val anchor: PsiElement,
    )
}

internal fun renderRouteDocumentation(routes: Collection<Route>, twigTemplates: Int = 0): String = routes.map { route ->
    buildString {
        append(DocumentationMarkup.CONTENT_START)
        append("<code>").append(escape(route.name)).append("</code>")
        RouteHelper.getRouteUrl(route)?.let { path ->
            append("<br>Path: <code>").append(escape(path)).append("</code>")
        }

        if (route.methods.isNotEmpty()) {
            val methods = route.methods.map { it.uppercase(Locale.ROOT) }.sorted().joinToString(", ")
            append("<br>Methods: <code>").append(escape(methods)).append("</code>")
        }

        val defaults = route.defaults.keys.filter { it != "_controller" }.sorted()
        if (defaults.isNotEmpty()) {
            append("<br>Defaults: ")
            append(defaults.joinToString(", ") { "<code>${escape(it)}</code>" })
        }

        if (route.requirements.isNotEmpty()) {
            append("<br>Requirements: ")
            append(route.requirements.toSortedMap().entries.joinToString(", ") { (name, value) ->
                "<code>${escape(name)}: ${escape(value)}</code>"
            })
        }

        route.controller?.takeIf { it.isNotBlank() }?.let {
            append("<br>Controller: <code>").append(escape(it)).append("</code>")
        }

        if (twigTemplates > 0) {
            append("<br>Twig usages: ").append(twigTemplates)
        }

        append(DocumentationMarkup.CONTENT_END)
    }
}.distinct().sorted().joinToString("<hr>")

private fun escape(value: String): String {
    val text = if (value.length > 1000) value.take(1000) + "…" else value

    return StringUtil.escapeXmlEntities(text)
}
