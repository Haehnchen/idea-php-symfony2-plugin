package fr.adrienbrault.idea.symfony2plugin.extension

import com.intellij.openapi.vfs.VirtualFile

/**
 * Describes one already-resolved Symfony UX Twig Component contributed by another plugin.
 *
 * @param name canonical UX component name, for example `Alert` or `ExternalPackage:Button:Primary`.
 * In a Twig tag, this is the part after `<twig:`, for example `<twig:ExternalPackage:Button:Primary />`.
 * @param template concrete Twig template file backing this component.
 * @param phpClassFqn optional component class FQN string. When present, it must include the leading backslash.
 */
data class TwigComponentDefinition(
    val name: String,
    val template: VirtualFile,
    val phpClassFqn: String? = null,
)
