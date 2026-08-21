package fr.adrienbrault.idea.symfony2plugin.routing.inspection

import com.intellij.openapi.project.Project
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.util.AbstractGuessTypoQuickFix
import fr.adrienbrault.idea.symfony2plugin.util.SimilarSuggestionUtil

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class RouteGuessTypoQuickFix(private val missingRoute: String) : AbstractGuessTypoQuickFix() {
    protected override fun getSuggestionLabel(): String {
        return "Route"
    }

    protected override fun getSimilarItems(project: Project): List<String> {
        return SimilarSuggestionUtil.findSimilarString(missingRoute, RouteHelper.getAllRoutes(project).keys)
    }
}
