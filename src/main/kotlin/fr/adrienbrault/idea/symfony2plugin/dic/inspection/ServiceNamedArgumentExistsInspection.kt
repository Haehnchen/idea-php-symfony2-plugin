package fr.adrienbrault.idea.symfony2plugin.dic.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

private fun isSupportedDefinition(element: PsiElement): Boolean {
    val context = element.context

    if (context is YAMLKeyValue) {
        // arguments: ['$foobar': '@foo']
        val yamlMapping = context.parent
        if (yamlMapping is YAMLMapping) {
            val yamlKeyValue = yamlMapping.parent
            if (yamlKeyValue is YAMLKeyValue) {
                val parentMapping = yamlKeyValue.parentMapping
                if (parentMapping != null) {
                    return parentMapping.getKeyValueByKey("factory") == null
                }
            }
        }
    }

    return true
}

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class ServiceNamedArgumentExistsInspection : LocalInspectionTool() {
    companion object {
        const val INSPECTION_MESSAGE = "Symfony: named argument does not exists"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector? = null
        private var namedArgumentPattern: ElementPattern<*>? = null

        override fun visitElement(element: PsiElement) {
            if (getNamedArgumentPattern().accepts(element)) {
                if (isSupportedDefinition(element) && ServiceContainerUtil.hasMissingYamlNamedArgumentForInspection(element, getLazyServiceCollector())) {
                    holder.registerProblem(element, INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                }
            }

            super.visitElement(element)
        }

        private fun getLazyServiceCollector(): ContainerCollectionResolver.LazyServiceCollector {
            val collector = lazyServiceCollector ?: ContainerCollectionResolver.LazyServiceCollector(holder.project)
            lazyServiceCollector = collector
            return collector
        }

        private fun getNamedArgumentPattern(): ElementPattern<*> {
            val pattern = namedArgumentPattern ?: YamlElementPatternHelper.getNamedArgumentPattern()
            namedArgumentPattern = pattern
            return pattern
        }
    }
}
