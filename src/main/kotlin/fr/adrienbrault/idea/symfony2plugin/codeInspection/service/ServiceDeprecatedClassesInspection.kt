package fr.adrienbrault.idea.symfony2plugin.codeInspection.service

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.YAMLTokenTypes

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class ServiceDeprecatedClassesInspection {
    open class ServiceDeprecatedClassesInspectionYaml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyYamlPsiElementVisitor(holder)
        }

        private class MyYamlPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var serviceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>? = null
            private var singleLineClassPattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                visitYamlElement(element, holder)
                super.visitElement(element)
            }

            private fun visitYamlElement(element: PsiElement, holder: ProblemsHolder) {
                if (getSingleLineClassPattern().accepts(element)) {
                    // class: '\Foo'
                    val text = PsiElementUtils.trimQuote(element.text)
                    if (StringUtils.isNotBlank(text)) {
                        ProblemRegistrar.attachDeprecatedProblem(element, text, holder, createLazyServiceCollector())
                    }
                } else if (element.node.elementType === YAMLTokenTypes.TEXT) {
                    // @service
                    val text = element.text
                    if (StringUtils.isNotBlank(text) && text.startsWith("@")) {
                        ProblemRegistrar.attachDeprecatedProblem(element, text.substring(1), holder, createLazyServiceCollector())
                        ProblemRegistrar.attachServiceDeprecatedProblem(element, text.substring(1), holder, createLazyServiceCollector())
                    }
                }
            }

            private fun createLazyServiceCollector(): NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> {
                if (serviceCollector == null) {
                    serviceCollector = NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
                }

                return serviceCollector!!
            }

            private fun getSingleLineClassPattern(): ElementPattern<*> {
                return singleLineClassPattern ?: YamlElementPatternHelper.getSingleLineScalarKey("class").also { singleLineClassPattern = it }
            }
        }
    }

    open class ServiceDeprecatedClassesInspectionXml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyXmlPsiElementVisitor(holder)
        }

        private class MyXmlPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var serviceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>? = null
            private var argumentServiceIdPattern: ElementPattern<*>? = null
            private var serviceClassAttributePattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                visitXmlElement(element, holder)
                super.visitElement(element)
            }

            private fun visitXmlElement(element: PsiElement, holder: ProblemsHolder) {
                val serviceArgumentAccepted = getArgumentServiceIdPattern().accepts(element)

                if (serviceArgumentAccepted || getServiceClassAttributePattern().accepts(element)) {
                    val text = PsiElementUtils.trimQuote(element.text)
                    val psiElements = element.children

                    // we need to attach to child because else strike out equal and quote char
                    if (StringUtils.isNotBlank(text) && psiElements.size > 2) {
                        ProblemRegistrar.attachDeprecatedProblem(psiElements[1], text, holder, createLazyServiceCollector())

                        // check service arguments for "deprecated" defs
                        if (serviceArgumentAccepted) {
                            ProblemRegistrar.attachServiceDeprecatedProblem(psiElements[1], text, holder, createLazyServiceCollector())
                        }
                    }
                }
            }

            private fun createLazyServiceCollector(): NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> {
                if (serviceCollector == null) {
                    serviceCollector = NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
                }

                return serviceCollector!!
            }

            private fun getArgumentServiceIdPattern(): ElementPattern<*> {
                return argumentServiceIdPattern ?: XmlHelper.getArgumentServiceIdPattern().also { argumentServiceIdPattern = it }
            }

            private fun getServiceClassAttributePattern(): ElementPattern<*> {
                return serviceClassAttributePattern ?: XmlHelper.getServiceClassAttributeWithIdPattern().also { serviceClassAttributePattern = it }
            }
        }
    }

    open class ServiceDeprecatedClassesInspectionPhp : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyPhpPsiElementVisitor(holder)
        }

        private class MyPhpPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var serviceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>? = null
            private var autowireServicePattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                if (element is StringLiteralExpression) {
                    visitPhpElement(element, holder)
                }

                super.visitElement(element)
            }

            private fun visitPhpElement(psiElement: StringLiteralExpression, holder: ProblemsHolder) {
                // #[Autowire(service: 'foobar')]
                val leafText = PsiElementUtils.getTextLeafElementFromStringLiteralExpression(psiElement)

                if (leafText != null && getAutowireServicePattern().accepts(leafText)) {
                    val contents = psiElement.contents
                    if (StringUtils.isNotBlank(contents)) {
                        ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, createLazyServiceCollector())
                        ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, createLazyServiceCollector())
                    }

                    return
                }

                val methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(psiElement)
                if (methodReference == null || !ServiceContainerUtil.isServiceGetMethod(methodReference)) {
                    return
                }

                val contents = psiElement.contents
                if (StringUtils.isNotBlank(contents)) {
                    ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, createLazyServiceCollector())
                    ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, createLazyServiceCollector())
                }
            }

            private fun createLazyServiceCollector(): NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> {
                if (serviceCollector == null) {
                    serviceCollector = NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
                }

                return serviceCollector!!
            }

            private fun getAutowireServicePattern(): ElementPattern<*> {
                return autowireServicePattern ?: PhpElementsUtil.getAttributeNamedArgumentStringPattern(
                    ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS,
                    "service"
                ).also { autowireServicePattern = it }
            }
        }
    }

    private object ProblemRegistrar {
        fun attachDeprecatedProblem(
            element: PsiElement,
            text: String,
            holder: ProblemsHolder,
            lazyServiceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
        ) {
            val phpClass = ServiceUtil.getResolvedClassDefinition(element.project, text, lazyServiceCollector.get())
                ?: return

            val docComment = phpClass.docComment
            if (docComment != null && docComment.getTagElementsByName("@deprecated").isNotEmpty()) {
                holder.registerProblem(element, String.format("Class '%s' is deprecated", phpClass.name), ProblemHighlightType.LIKE_DEPRECATED)
            }
        }

        fun attachServiceDeprecatedProblem(
            element: PsiElement,
            serviceName: String,
            holder: ProblemsHolder,
            lazyServiceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
        ) {
            val services = lazyServiceCollector.get().collector.services
            if (!services.containsKey(serviceName)) {
                return
            }

            if (!services[serviceName]!!.isDeprecated) {
                return
            }

            holder.registerProblem(element, String.format("Service '%s' is deprecated", serviceName), ProblemHighlightType.LIKE_DEPRECATED)
        }
    }
}
