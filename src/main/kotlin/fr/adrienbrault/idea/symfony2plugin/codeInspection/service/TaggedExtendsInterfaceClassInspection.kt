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
            private val serviceCollector by lazy(LazyThreadSafetyMode.NONE) {
                NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
            }
            private val singleLineClassPattern by lazy(LazyThreadSafetyMode.NONE) {
                YamlElementPatternHelper.getSingleLineScalarKey("class")
            }
            private val serviceIdKeyValuePattern by lazy(LazyThreadSafetyMode.NONE) {
                YamlElementPatternHelper.getServiceIdKeyValuePattern()
            }

            override fun visitElement(element: PsiElement) {
                visitYamlElement(element)
                super.visitElement(element)
            }

            private fun visitYamlElement(psiElement: PsiElement) {
                if (singleLineClassPattern.accepts(psiElement)) {
                    // class: '\Foo'
                    val text = PsiElementUtils.trimQuote(psiElement.text)
                    if (text.isBlank()) {
                        return
                    }

                    val yamlScalar = psiElement.parent as? YAMLScalar ?: return
                    val classKey = yamlScalar.parent as? YAMLKeyValue ?: return
                    val yamlCompoundValue = classKey.parent as? YAMLCompoundValue ?: return
                    val serviceKeyValue = yamlCompoundValue.parent as? YAMLKeyValue ?: return
                    val tags = YamlHelper.collectServiceTags(serviceKeyValue)
                    if (tags.isNotEmpty()) {
                        registerTaggedProblems(psiElement, tags, text, holder, serviceCollector)
                    }
                } else if (psiElement.node.elementType === YAMLTokenTypes.SCALAR_KEY && serviceIdKeyValuePattern.accepts(psiElement.parent)) {
                    // Foobar\Foo: ~
                    val text = PsiElementUtils.getText(psiElement)
                    if (text.isNotBlank() && YamlHelper.isClassServiceId(text) && "\\" in text) {
                        val yamlKeyValue = psiElement.parent as? YAMLKeyValue
                        if (yamlKeyValue != null && YamlHelper.getYamlKeyValue(yamlKeyValue, "resource") == null && YamlHelper.getYamlKeyValue(yamlKeyValue, "exclude") == null) {
                            val tags = YamlHelper.collectServiceTags(yamlKeyValue)
                            if (tags.isNotEmpty()) {
                                registerTaggedProblems(psiElement, tags, text, holder, serviceCollector)
                            }
                        }
                    }
                }
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
            private val serviceCollector by lazy(LazyThreadSafetyMode.NONE) {
                NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
            }
            private val xmlServiceClassAttrPattern by lazy(LazyThreadSafetyMode.NONE) {
                XmlHelper.getServiceClassAttributeWithIdPattern()
            }
            private val xmlServiceIdAttrPattern by lazy(LazyThreadSafetyMode.NONE) {
                XmlHelper.getServiceIdAttributePattern()
            }

            override fun visitElement(element: PsiElement) {
                visitXmlElement(element)
                super.visitElement(element)
            }

            private fun visitXmlElement(element: PsiElement) {
                val className = getClassNameFromServiceDefinition(element, xmlServiceClassAttrPattern, xmlServiceIdAttrPattern) ?: return
                val serviceTag = PsiTreeUtil.getParentOfType(element, XmlTag::class.java) ?: return

                // attach problems to string value only
                val psiElements = element.children
                if (psiElements.size > 2) {
                    registerTaggedProblems(psiElements[1], FormUtil.getTags(serviceTag), className, holder, serviceCollector)
                }
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
            val resolvedClass = phpClass
                ?: ServiceUtil.getResolvedClassDefinition(holder.project, serviceClass, lazyServiceCollector.get())
                ?: return
            phpClass = resolvedClass

            // skip unknown classes
            if (PhpElementsUtil.getClassesInterface(resolvedClass.project, expectedClass).isEmpty()) {
                continue
            }

            // check interfaces
            if (!PhpElementsUtil.isInstanceOf(resolvedClass, expectedClass)) {
                missingTagInstance = expectedClass
                continue
            }

            missingTagInstance = null
            break
        }

        // check interfaces
        if (missingTagInstance != null) {
            holder.registerProblem(source, "Class needs to implement '${missingTagInstance.trimStart('\\')}' for tag '$tag'", ProblemHighlightType.WEAK_WARNING)
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
    return if (xmlServiceClassAttrPattern.accepts(element)) {
        // <service class="Foo\\Bar" id="required_attribute">
        PsiElementUtils.trimQuote(element.text).takeIf { it.isNotBlank() }
    } else if (xmlServiceIdAttrPattern.accepts(element)) {
        // <service id="Foo\\Bar" />
        val text = PsiElementUtils.trimQuote(element.text)
        text.takeIf { it.isNotBlank() && YamlHelper.isClassServiceId(it) && "\\" in it }
    } else {
        null
    }
}
