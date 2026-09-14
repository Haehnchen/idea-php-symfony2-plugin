package fr.adrienbrault.idea.symfony2plugin.codeInspection.service

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
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
            private val serviceCollector by lazy(LazyThreadSafetyMode.NONE) {
                NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
            }
            private val singleLineClassPattern by lazy(LazyThreadSafetyMode.NONE) {
                YamlElementPatternHelper.getSingleLineScalarKey("class")
            }

            override fun visitElement(element: PsiElement) {
                visitYamlElement(element)
                super.visitElement(element)
            }

            private fun visitYamlElement(element: PsiElement) {
                if (singleLineClassPattern.accepts(element)) {
                    // class: '\Foo'
                    val text = PsiElementUtils.trimQuote(element.text)
                    if (text.isNotBlank()) {
                        ProblemRegistrar.attachDeprecatedProblem(element, text, holder, serviceCollector)
                    }
                } else if (element.node.elementType === YAMLTokenTypes.TEXT) {
                    // @service
                    val text = element.text
                    if (text.isNotBlank() && text.startsWith('@')) {
                        val serviceName = text.drop(1)
                        ProblemRegistrar.attachDeprecatedProblem(element, serviceName, holder, serviceCollector)
                        ProblemRegistrar.attachServiceDeprecatedProblem(element, serviceName, holder, serviceCollector)
                    }
                }
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
            private val serviceCollector by lazy(LazyThreadSafetyMode.NONE) {
                NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
            }
            private val argumentServiceIdPattern by lazy(LazyThreadSafetyMode.NONE) { XmlHelper.getArgumentServiceIdPattern() }
            private val serviceClassAttributePattern by lazy(LazyThreadSafetyMode.NONE) { XmlHelper.getServiceClassAttributeWithIdPattern() }

            override fun visitElement(element: PsiElement) {
                visitXmlElement(element)
                super.visitElement(element)
            }

            private fun visitXmlElement(element: PsiElement) {
                val serviceArgumentAccepted = argumentServiceIdPattern.accepts(element)

                if (serviceArgumentAccepted || serviceClassAttributePattern.accepts(element)) {
                    val text = PsiElementUtils.trimQuote(element.text)
                    val psiElements = element.children

                    // we need to attach to child because else strike out equal and quote char
                    if (text.isNotBlank() && psiElements.size > 2) {
                        ProblemRegistrar.attachDeprecatedProblem(psiElements[1], text, holder, serviceCollector)

                        // check service arguments for "deprecated" defs
                        if (serviceArgumentAccepted) {
                            ProblemRegistrar.attachServiceDeprecatedProblem(psiElements[1], text, holder, serviceCollector)
                        }
                    }
                }
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
            private val serviceCollector by lazy(LazyThreadSafetyMode.NONE) {
                NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
            }
            private val autowireServicePattern by lazy(LazyThreadSafetyMode.NONE) {
                PhpElementsUtil.getAttributeNamedArgumentStringPattern(
                    ServiceContainerUtil.AUTOWIRE_ATTRIBUTE_CLASS,
                    "service"
                )
            }

            override fun visitElement(element: PsiElement) {
                if (element is StringLiteralExpression) {
                    visitPhpElement(element, holder)
                }

                super.visitElement(element)
            }

            private fun visitPhpElement(psiElement: StringLiteralExpression, holder: ProblemsHolder) {
                // #[Autowire(service: 'foobar')]
                val leafText = PsiElementUtils.getTextLeafElementFromStringLiteralExpression(psiElement)

                if (leafText != null && autowireServicePattern.accepts(leafText)) {
                    val contents = psiElement.contents
                    if (contents.isNotBlank()) {
                        ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, serviceCollector)
                        ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, serviceCollector)
                    }

                    return
                }

                val methodReference = PsiElementUtils.getMethodReferenceWithFirstStringParameter(psiElement)
                if (methodReference == null || !ServiceContainerUtil.isServiceGetMethod(methodReference)) {
                    return
                }

                val contents = psiElement.contents
                if (contents.isNotBlank()) {
                    ProblemRegistrar.attachDeprecatedProblem(psiElement, contents, holder, serviceCollector)
                    ProblemRegistrar.attachServiceDeprecatedProblem(psiElement, contents, holder, serviceCollector)
                }
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

            if (phpClass.docComment?.getTagElementsByName("@deprecated")?.isNotEmpty() == true) {
                holder.registerProblem(
                    element,
                    "Class '${phpClass.name}' is deprecated",
                    ProblemHighlightType.LIKE_DEPRECATED
                )
            }
        }

        fun attachServiceDeprecatedProblem(
            element: PsiElement,
            serviceName: String,
            holder: ProblemsHolder,
            lazyServiceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
        ) {
            if (lazyServiceCollector.get().collector.services[serviceName]?.isDeprecated != true) {
                return
            }

            holder.registerProblem(
                element,
                "Service '$serviceName' is deprecated",
                ProblemHighlightType.LIKE_DEPRECATED
            )
        }
    }
}
