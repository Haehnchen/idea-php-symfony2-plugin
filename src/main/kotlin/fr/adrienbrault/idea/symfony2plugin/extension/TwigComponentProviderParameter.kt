package fr.adrienbrault.idea.symfony2plugin.extension

import com.intellij.openapi.project.Project

/**
 * Input passed to external Twig component providers.
 */
data class TwigComponentProviderParameter(
    val project: Project,
)
