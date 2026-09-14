package fr.adrienbrault.idea.symfony2plugin.codeInspection.service

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlTag
import com.jetbrains.php.lang.psi.elements.PhpClass
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.form.util.FormUtil
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLCompoundValue
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TaggedExtendsInterfaceClassInspection {
    open class TaggedExtendsInterfaceClassInspectionYaml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyYamlPsiElementVisitor(holder)
        }

        private class MyYamlPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var serviceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>? = null

            private var singleLineClassPattern: ElementPattern<*>? = null
            private var serviceIdKeyValuePattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                visitYamlElement(element, holder)
                super.visitElement(element)
            }

            private fun visitYamlElement(psiElement: PsiElement, holder: ProblemsHolder) {
                if (getSingleLineClassPattern().accepts(psiElement)) {
                    // class: '\Foo'
                    val text = PsiElementUtils.trimQuote(psiElement.text)
                    if (StringUtils.isBlank(text)) {
                        return
                    }

                    val yamlScalar = psiElement.parent
                    if (yamlScalar !is YAMLScalar) {
                        return
                    }

                    val classKey = yamlScalar.parent
                    if (classKey is YAMLKeyValue) {
                        val yamlCompoundValue = classKey.parent
                        if (yamlCompoundValue is YAMLCompoundValue) {
                            val serviceKeyValue = yamlCompoundValue.parent
                            if (serviceKeyValue is YAMLKeyValue) {
                                val tags = YamlHelper.collectServiceTags(serviceKeyValue)
                                if (tags.isNotEmpty()) {
                                    registerTaggedProblems(psiElement, tags, text, holder, createLazyServiceCollector())
                                }
                            }
                        }
                    }
                } else if (psiElement.node.elementType === YAMLTokenTypes.SCALAR_KEY && getServiceIdKeyValuePattern().accepts(psiElement.parent)) {
                    // Foobar\Foo: ~
                    val text = PsiElementUtils.getText(psiElement)
                    if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text) && text.contains("\\")) {
                        val yamlKeyValue = psiElement.parent
                        if (yamlKeyValue is YAMLKeyValue && YamlHelper.getYamlKeyValue(yamlKeyValue, "resource") == null && YamlHelper.getYamlKeyValue(yamlKeyValue, "exclude") == null) {
                            val tags = YamlHelper.collectServiceTags(yamlKeyValue)
                            if (tags.isNotEmpty()) {
                                registerTaggedProblems(psiElement, tags, text, holder, createLazyServiceCollector())
                            }
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

            private fun getSingleLineClassPattern(): ElementPattern<*> {
                return singleLineClassPattern ?: YamlElementPatternHelper.getSingleLineScalarKey("class").also { singleLineClassPattern = it }
            }

            private fun getServiceIdKeyValuePattern(): ElementPattern<*> {
                return serviceIdKeyValuePattern ?: YamlElementPatternHelper.getServiceIdKeyValuePattern().also { serviceIdKeyValuePattern = it }
            }
        }
    }

    open class TaggedExtendsInterfaceClassInspectionXml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyXmlPsiElementVisitor(holder)
        }

        private class MyXmlPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var serviceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>? = null

            private var xmlServiceClassAttrPattern: ElementPattern<*>? = null
            private var xmlServiceIdAttrPattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                visitXmlElement(element, holder)
                super.visitElement(element)
            }

            private fun visitXmlElement(element: PsiElement, holder: ProblemsHolder) {
                val className = getClassNameFromServiceDefinition(element, getXmlServiceClassAttrPattern(), getXmlServiceIdAttrPattern())
                if (className != null) {
                    val parentOfType = PsiTreeUtil.getParentOfType(element, XmlTag::class.java)
                    if (parentOfType != null) {
                        // attach problems to string value only
                        val psiElements = element.children
                        if (psiElements.size > 2) {
                            registerTaggedProblems(psiElements[1], FormUtil.getTags(parentOfType), className, holder, createLazyServiceCollector())
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

            private fun getXmlServiceClassAttrPattern(): ElementPattern<*> {
                return xmlServiceClassAttrPattern ?: XmlHelper.getServiceClassAttributeWithIdPattern().also { xmlServiceClassAttrPattern = it }
            }

            private fun getXmlServiceIdAttrPattern(): ElementPattern<*> {
                return xmlServiceIdAttrPattern ?: XmlHelper.getServiceIdAttributePattern().also { xmlServiceIdAttrPattern = it }
            }
        }
    }
}

private fun registerTaggedProblems(
    source: PsiElement,
    tags: Set<String>,
    serviceClass: String,
    holder: ProblemsHolder,
    lazyServiceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
) {
    if (tags.isEmpty()) {
        return
    }

    var phpClass: PhpClass? = null

    for (tag in tags) {
        var missingTagInstance: String? = null

        for (expectedClass in ServiceUtil.TAG_INTERFACES.getOrDefault(tag, emptyArray())) {
            // load PhpClass only if we need it, on error exit
            if (phpClass == null) {
                phpClass = ServiceUtil.getResolvedClassDefinition(holder.project, serviceClass, lazyServiceCollector.get())
                if (phpClass == null) {
                    return
                }
            }

            // skip unknown classes
            if (PhpElementsUtil.getClassesInterface(phpClass.project, expectedClass).isEmpty()) {
                continue
            }

            // check interfaces
            if (!PhpElementsUtil.isInstanceOf(phpClass, expectedClass)) {
                missingTagInstance = expectedClass
                continue
            }

            missingTagInstance = null
            break
        }

        // check interfaces
        if (missingTagInstance != null) {
            holder.registerProblem(
                source,
                String.format("Class needs to implement '%s' for tag '%s'", StringUtils.stripStart(missingTagInstance, "\\"), tag),
                ProblemHighlightType.WEAK_WARNING
            )
        }
    }
}

/**
 * <service class="Foo\\Bar" id="required_attribute">
 * <service id="Foo\\Bar" />
 */
private fun getClassNameFromServiceDefinition(
    element: PsiElement,
    xmlServiceClassAttrPattern: ElementPattern<*>,
    xmlServiceIdAttrPattern: ElementPattern<*>
): String? {
    if (xmlServiceClassAttrPattern.accepts(element)) {
        // <service class="Foo\\Bar" id="required_attribute">
        val text = PsiElementUtils.trimQuote(element.text)
        if (StringUtils.isNotBlank(text)) {
            return text
        }
    } else if (xmlServiceIdAttrPattern.accepts(element)) {
        // <service id="Foo\\Bar" />
        val text = PsiElementUtils.trimQuote(element.text)
        if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text) && text.contains("\\")) {
            return text
        }
    }

    return null
}
