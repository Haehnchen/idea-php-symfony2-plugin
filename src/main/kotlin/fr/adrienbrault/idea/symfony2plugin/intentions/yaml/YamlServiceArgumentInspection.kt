package fr.adrienbrault.idea.symfony2plugin.intentions.yaml

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class YamlServiceArgumentInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val problemsHolder: ProblemsHolder) : PsiElementVisitor() {
        private val lazyServiceCollector by lazy(LazyThreadSafetyMode.NONE) {
            ContainerCollectionResolver.LazyServiceCollector(problemsHolder.project)
        }

        override fun visitElement(element: PsiElement) {
            val servicesKey = (element.parent as? YAMLMapping)?.parent as? YAMLKeyValue
            if (element is YAMLKeyValue && servicesKey?.keyText == "services") {
                // we don't support parent services for now
                if (!element.keyText.equals("_defaults", ignoreCase = true) && isValidService(element)) {
                    val container = ServiceActionUtil.ServiceYamlContainer.create(element)
                    if (container != null) {
                        val yamlMissingArgumentTypes = ServiceActionUtil.getYamlMissingArgumentTypes(
                            problemsHolder.project,
                            container,
                            false,
                            lazyServiceCollector
                        )

                        if (yamlMissingArgumentTypes.isNotEmpty()) {
                            problemsHolder.registerProblem(element.firstChild, "Missing argument", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, YamlArgumentQuickfix())
                        }
                    }
                }
            }

            super.visitElement(element)
        }

        private fun isValidService(serviceKey: YAMLKeyValue): Boolean {
            val keySet = YamlHelper.getKeySet(serviceKey) ?: return true

            if (INVALID_KEYS.any { it in keySet }) {
                return false
            }

            // check autowire scope
            val serviceAutowire = YamlHelper.getYamlKeyValueAsBoolean(serviceKey, "autowire")
            if (serviceAutowire != null) {
                // use service scope for autowire
                return !serviceAutowire
            }

            // find file scope defaults: defaults: [autowire: true]
            val defaults = serviceKey.parentMapping
                ?.let { YamlHelper.getYamlKeyValue(it, "_defaults") }
                ?: return true
            return YamlHelper.getYamlKeyValueAsBoolean(defaults, "autowire") != true
        }
    }

    private class YamlArgumentQuickfix : LocalQuickFix {
        override fun getName() = "Symfony: Yaml Argument"

        override fun getFamilyName() = "Symfony"

        override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
            val serviceKeyValue = problemDescriptor.psiElement.parent

            if (serviceKeyValue is YAMLKeyValue) {
                ServiceActionUtil.fixServiceArgument(serviceKeyValue)
            }
        }

        override fun startInWriteAction() = false
    }

    @Suppress("CompanionObjectInExtension") // Kept for the existing Java ABI.
    companion object {
        @JvmField
        val INVALID_KEYS = arrayOf(
            "parent", "factory_class", "factory_service",
            "factory_method", "abstract", "factory",
            "resource", "exclude", "alias"
        )
    }
}
