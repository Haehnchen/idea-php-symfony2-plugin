package fr.adrienbrault.idea.symfony2plugin.dic.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import java.util.Locale

/**
 * Check if service parameter exists
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class YamlParameterInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return ParameterVisitor(holder)
    }

    private class ParameterVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector? = null

        private var serviceParameterPattern: ElementPattern<PsiElement>? = null
        private var insideServiceKeyPattern: ElementPattern<PsiElement>? = null

        override fun visitElement(psiElement: PsiElement) {
            if (getServiceParameterPattern().accepts(psiElement) && getInsideServiceKeyPattern().accepts(psiElement)) {
                val collector = lazyServiceCollector ?: ContainerCollectionResolver.LazyServiceCollector(holder.project)
                lazyServiceCollector = collector

                invoke(psiElement, holder, collector)
            }

            super.visitElement(psiElement)
        }

        private fun invoke(psiElement: PsiElement, holder: ProblemsHolder, lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector) {
            // at least %a%
            // and not this one: %kernel.root_dir%/../web/
            // %kernel.root_dir%/../web/%webpath_modelmasks%
            var parameterName = PsiElementUtils.getText(psiElement)
            if (!YamlHelper.isValidParameterName(parameterName)) {
                return
            }

            // strip "%"
            parameterName = parameterName.substring(1, parameterName.length - 1)

            // parameter a always lowercase see #179
            parameterName = parameterName.lowercase(Locale.getDefault())
            if (!ContainerCollectionResolver.hasParameterName(lazyServiceCollector, parameterName)) {
                holder.registerProblem(psiElement, "Symfony: Missing Parameter", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
            }
        }

        private fun getServiceParameterPattern(): ElementPattern<PsiElement> {
            val pattern = serviceParameterPattern ?: YamlElementPatternHelper.getServiceParameterDefinition()
            serviceParameterPattern = pattern
            return pattern
        }

        private fun getInsideServiceKeyPattern(): ElementPattern<PsiElement> {
            val pattern = insideServiceKeyPattern ?: YamlElementPatternHelper.getInsideServiceKeyPattern()
            insideServiceKeyPattern = pattern
            return pattern
        }
    }
}
