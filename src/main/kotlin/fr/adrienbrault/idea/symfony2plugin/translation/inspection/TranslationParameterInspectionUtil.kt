package fr.adrienbrault.idea.symfony2plugin.translation.inspection

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil

object TranslationParameterInspectionUtil {
    /**
     * Reports the parameters passed to a translation, keyed by name, that match no ICU argument of its messages.
     */
    @JvmStatic
    fun registerUnknownParameters(holder: ProblemsHolder, key: String, domain: String, parameters: Map<String, PsiElement>) {
        if (parameters.isEmpty()) {
            return
        }

        val names = TranslationUtil.getIcuParameterNames(holder.project, key, domain) ?: return
        for ((parameter, element) in parameters) {
            // Symfony's ICU formatter reads "%name%" and "{name}" keys as "name"
            val name = if (parameter.startsWith('%') || parameter.startsWith('{')) parameter.trim('%', '{', '}', ' ') else parameter
            if (name !in names) {
                holder.registerProblem(element, TwigTranslationParameterInspection.MESSAGE)
            }
        }
    }
}
