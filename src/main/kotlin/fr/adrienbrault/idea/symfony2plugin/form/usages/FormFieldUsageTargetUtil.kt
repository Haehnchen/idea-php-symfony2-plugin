package fr.adrienbrault.idea.symfony2plugin.form.usages

import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.Field
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpNamedElement
import com.jetbrains.php.lang.psi.elements.PhpTypeDeclaration
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.form.FormUnderscoreMethodReference
import fr.adrienbrault.idea.symfony2plugin.util.StringUtils

/**
 * Keep prefixes, argument counts and getter precedence in one place.
 */
private enum class FormAccessor(val prefix: String, val argumentCount: Int) {
    GET("get", 0),
    IS("is", 0),
    HAS("has", 0),
    SET("set", 1),
}

internal fun formTargetClass(target: PhpNamedElement): PhpClass? = when (target) {
    is Field -> target.containingClass
    is Method -> target.containingClass
    else -> null
}

internal fun isSupportedFormTarget(target: PhpNamedElement): Boolean = when (target) {
    is Field -> isSupportedFormField(target)
    is Method -> formTargetClass(target)?.let { !it.isInterface && !it.isTrait } == true &&
        formAccessor(target) != null
    else -> false
}

private fun isSupportedFormField(field: Field): Boolean =
    field.modifier.isPublic && !field.modifier.isStatic && !field.isConstant &&
        field.containingClass?.let { !it.isInterface && !it.isTrait } == true

/**
 * Check public PropertyAccessor methods and their callable signatures.
 * Optional trailing arguments are allowed; getters must be able to return a value.
 */
private fun formAccessor(method: Method): FormAccessor? {
    if (!method.access.isPublic || method.isStatic) return null

    val name = method.name
    val accessor = FormAccessor.entries.firstOrNull {
        name.startsWith(it.prefix, true) && name.length > it.prefix.length
    } ?: return null

    val parameters = method.parameters
    if (parameters.size < accessor.argumentCount || parameters.drop(accessor.argumentCount).any { !it.isOptional }) {
        return null
    }

    if (accessor == FormAccessor.SET) return accessor

    val returnType = PsiTreeUtil.getChildOfType(method, PhpTypeDeclaration::class.java)?.text
    if (returnType == "void" || returnType == "never") return null

    return accessor
}

/**
 * Resolve on the actual data class so an override does not count as a reference to its parent.
 * Properties and setters reuse the existing form-field navigation and name normalization.
 */
internal fun resolvesToFormTarget(literal: StringLiteralExpression, dataClass: PhpClass, target: PhpNamedElement): Boolean {
    val manager = PsiManager.getInstance(literal.project)
    val accessor = (target as? Method)?.let(::formAccessor)

    if (target is Field || accessor == FormAccessor.SET) {
        return FormUnderscoreMethodReference(literal, dataClass).multiResolve(false).any {
            manager.areElementsEquivalent(it.element, target)
        }
    }

    if (accessor == null) return false

    val getter = resolveFormGetter(literal, dataClass) ?: return false

    return manager.areElementsEquivalent(getter, target)
}

/**
 * Read access uses the first callable getter in PropertyAccessor order.
 * Twig's shortcut helpers only check names and do not enforce these signatures.
 */
private fun resolveFormGetter(literal: StringLiteralExpression, dataClass: PhpClass): Method? {
    val suffix = StringUtils.camelize(literal.contents)

    for (accessor in FormAccessor.entries) {
        if (accessor == FormAccessor.SET) continue

        val method = dataClass.findMethodByName(accessor.prefix + suffix) ?: continue
        if (formAccessor(method) != accessor) continue

        return method
    }

    return null
}
