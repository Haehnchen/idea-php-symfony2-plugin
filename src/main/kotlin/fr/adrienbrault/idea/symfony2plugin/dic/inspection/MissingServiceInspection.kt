package fr.adrienbrault.idea.symfony2plugin.dic.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class MissingServiceInspection {
    companion object {
        const val INSPECTION_MESSAGE = "Symfony: Missing Service"
    }

    open class PhpLocalInspectionTool : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyPhpPsiElementVisitor(holder)
        }

        private class MyPhpPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector? = null
            private var autowireServicePattern: ElementPattern<*>? = null
            private var decoratorAttributePattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                if (element.language === PhpLanguage.INSTANCE && element is StringLiteralExpression) {
                    // PHP
                    val methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(element)
                    if (methodReference != null && ServiceContainerUtil.isServiceGetMethod(methodReference)) {
                        val serviceName = PhpElementsUtil.getFirstArgumentStringValue(methodReference)
                        if (serviceName != null && StringUtils.isNotBlank(serviceName) && !hasService(serviceName)) {
                            holder.registerProblem(element, INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                        }
                    }

                    // #[Autowire(service: 'foobar')]
                    val leafText = PsiElementUtils.getTextLeafElementFromStringLiteralExpression(element)

                    val isAttributeLeaf = leafText != null && (
                        getAutowireServicePattern().accepts(leafText)
                            || getDecoratorAttributePattern().accepts(leafText)
                    )

                    if (isAttributeLeaf) {
                        val serviceName = element.contents
                        if (StringUtils.isNotBlank(serviceName) && !hasService(serviceName)) {
                            holder.registerProblem(element, INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                        }
                    }
                }

                super.visitElement(element)
            }

            private fun hasService(serviceName: String): Boolean {
                val collector = lazyServiceCollector ?: ContainerCollectionResolver.LazyServiceCollector(holder.project)
                lazyServiceCollector = collector

                return ContainerCollectionResolver.hasServiceName(collector, serviceName)
            }

            private fun getAutowireServicePattern(): ElementPattern<*> {
                val pattern = autowireServicePattern
                    ?: PhpElementsUtil.getAttributeNamedArgumentStringPattern(ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS, "service")
                autowireServicePattern = pattern
                return pattern
            }

            private fun getDecoratorAttributePattern(): ElementPattern<*> {
                val pattern = decoratorAttributePattern
                    ?: PhpElementsUtil.getFirstAttributeStringPattern(ServiceContainerUtil.DECORATOR_ATTRIBUTE_CLASS)
                decoratorAttributePattern = pattern
                return pattern
            }
        }
    }


    open class YamlLocalInspectionTool : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyYamlPsiElementVisitor(holder)
        }

        private class MyYamlPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector? = null
            private var serviceDefinitionPattern: ElementPattern<*>? = null
            private var insideServiceKeyPattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                if (getServiceDefinitionPattern().accepts(element) && getInsideServiceKeyPattern().accepts(element)) {
                    val serviceName = YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(element))

                    // dont mark "@", "@?", "@@" escaping and expressions
                    if (serviceName.length > 2 && !serviceName.startsWith("=") && !serviceName.startsWith("@") && !hasService(serviceName)) {
                        holder.registerProblem(element, INSPECTION_MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                    }
                }

                super.visitElement(element)
            }

            private fun hasService(serviceName: String): Boolean {
                val collector = lazyServiceCollector ?: ContainerCollectionResolver.LazyServiceCollector(holder.project)
                lazyServiceCollector = collector

                return ContainerCollectionResolver.hasServiceName(collector, serviceName)
            }

            private fun getServiceDefinitionPattern(): ElementPattern<*> {
                val pattern = serviceDefinitionPattern ?: YamlElementPatternHelper.getServiceDefinition()
                serviceDefinitionPattern = pattern
                return pattern
            }

            private fun getInsideServiceKeyPattern(): ElementPattern<*> {
                val pattern = insideServiceKeyPattern ?: YamlElementPatternHelper.getInsideServiceKeyPattern()
                insideServiceKeyPattern = pattern
                return pattern
            }
        }
    }
}
