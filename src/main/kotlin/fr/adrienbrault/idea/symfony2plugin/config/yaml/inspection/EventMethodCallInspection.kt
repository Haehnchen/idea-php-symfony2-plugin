package fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.parser.PhpElementTypes
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.PhpReturn
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.codeInspection.quickfix.CreateMethodQuickFix
import fr.adrienbrault.idea.symfony2plugin.config.EventDispatcherSubscriberUtil
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.AnnotationBackportUtil
import fr.adrienbrault.idea.symfony2plugin.util.EventSubscriberUtil
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class EventMethodCallInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var serviceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>? = null

        private var yamlTagsMethodPattern: ElementPattern<*>? = null
        private var yamlTextPattern: ElementPattern<PsiElement>? = null
        private var yamlScalarDstringPattern: ElementPattern<PsiElement>? = null
        private var yamlInsideCallsPattern: ElementPattern<*>? = null
        private var xmlTagMethodPattern: ElementPattern<*>? = null
        private var xmlCallMethodPattern: ElementPattern<*>? = null

        override fun visitElement(element: PsiElement) {
            val language = element.language

            if (language === YAMLLanguage.INSTANCE) {
                if (serviceCollector == null) {
                    serviceCollector = NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
                }

                visitYmlElement(element, holder, serviceCollector!!)
            } else if (language === XMLLanguage.INSTANCE) {
                if (serviceCollector == null) {
                    serviceCollector = NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
                }

                visitXmlElement(element, holder, serviceCollector!!)
            } else if (language === PhpLanguage.INSTANCE) {
                if (element is StringLiteralExpression) {
                    visitPhpElement(element, holder)
                }
            }

            super.visitElement(element)
        }

        private fun visitXmlElement(
            element: PsiElement,
            holder: ProblemsHolder,
            collector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
        ) {
            val isSupportedTag = getXmlTagMethodPattern().accepts(element)
                || getXmlCallMethodPattern().accepts(element)

            if (isSupportedTag) {
                // attach to text child only
                val psiElements = element.children
                if (psiElements.size < 2) {
                    return
                }

                val serviceClassValue = XmlHelper.getServiceDefinitionClass(element)
                if (StringUtils.isNotBlank(serviceClassValue)) {
                    registerMethodProblem(psiElements[1], holder, serviceClassValue!!, collector)
                }
            }
        }

        private fun visitYamlMethodTagKey(
            psiElement: PsiElement,
            holder: ProblemsHolder,
            collector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
        ) {
            val methodName = PsiElementUtils.trimQuote(psiElement.text)
            if (StringUtils.isBlank(methodName)) {
                return
            }

            val classValue = YamlHelper.getServiceDefinitionClassFromTagMethod(psiElement) ?: return

            registerMethodProblem(psiElement, holder, classValue, collector)
        }

        private fun visitYmlElement(
            psiElement: PsiElement,
            holder: ProblemsHolder,
            collector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
        ) {
            if (getYamlTagsMethodPattern().accepts(psiElement)) {
                visitYamlMethodTagKey(psiElement, holder, collector)
            }

            if (getYamlTextPattern().accepts(psiElement) || getYamlScalarDstringPattern().accepts(psiElement)) {
                visitYamlMethod(psiElement, holder, collector)
            }
        }

        private fun visitYamlMethod(
            psiElement: PsiElement,
            holder: ProblemsHolder,
            collector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
        ) {
            if (getYamlInsideCallsPattern().accepts(psiElement)) {
                val parent = psiElement.parent
                if (parent is YAMLScalar) {
                    YamlHelper.visitServiceCall(parent) { service ->
                        registerMethodProblem(psiElement, holder, YamlHelper.trimSpecialSyntaxServiceName(service), collector)
                    }
                }
            }
        }

        private fun registerMethodProblem(
            psiElement: PsiElement,
            holder: ProblemsHolder,
            classKeyValue: String,
            collector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
        ) {
            registerMethodProblem(psiElement, holder, ServiceUtil.getResolvedClassDefinition(psiElement.project, classKeyValue, collector.get()))
        }

        private fun getYamlTagsMethodPattern(): ElementPattern<*> {
            return yamlTagsMethodPattern ?: StandardPatterns.and(
                YamlElementPatternHelper.getInsideKeyValue("tags"),
                YamlElementPatternHelper.getSingleLineScalarKey("method")
            ).also { yamlTagsMethodPattern = it }
        }

        private fun getYamlTextPattern(): ElementPattern<PsiElement> {
            return yamlTextPattern ?: PlatformPatterns.psiElement(YAMLTokenTypes.TEXT).also { yamlTextPattern = it }
        }

        private fun getYamlScalarDstringPattern(): ElementPattern<PsiElement> {
            return yamlScalarDstringPattern ?: PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING).also { yamlScalarDstringPattern = it }
        }

        private fun getYamlInsideCallsPattern(): ElementPattern<*> {
            return yamlInsideCallsPattern ?: YamlElementPatternHelper.getInsideKeyValue("calls").also { yamlInsideCallsPattern = it }
        }

        private fun getXmlTagMethodPattern(): ElementPattern<*> {
            return xmlTagMethodPattern ?: XmlHelper.getTagAttributePattern("tag", "method")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern())
                .also { xmlTagMethodPattern = it }
        }

        private fun getXmlCallMethodPattern(): ElementPattern<*> {
            return xmlCallMethodPattern ?: XmlHelper.getTagAttributePattern("call", "method")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern())
                .also { xmlCallMethodPattern = it }
        }
    }
}

private fun registerMethodProblem(psiElement: PsiElement, holder: ProblemsHolder, phpClass: PhpClass?) {
    if (phpClass == null) {
        return
    }

    val methodName = PsiElementUtils.trimQuote(psiElement.text)
    if (phpClass.findMethodByName(methodName) != null) {
        return
    }

    holder.registerProblem(
        psiElement,
        "Missing Method",
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
        CreateMethodQuickFix(phpClass, methodName, MyCreateMethodQuickFix())
    )
}

private class MyCreateMethodQuickFix : CreateMethodQuickFix.InsertStringInterface {
    override fun getStringBuilder(problemDescriptor: ProblemDescriptor, phpClass: PhpClass, functionName: String): StringBuilder {
        val taggedEventMethodParameter = getEventTypeHint(problemDescriptor, phpClass)

        var parameter = ""
        if (taggedEventMethodParameter != null) {
            parameter = "$taggedEventMethodParameter \$event"
        }

        return StringBuilder()
            .append("public function ")
            .append(functionName)
            .append("(")
            .append(parameter)
            .append(")\n {\n}\n\n")
    }

    private fun getEventTypeHint(problemDescriptor: ProblemDescriptor, phpClass: PhpClass): String? {
        val eventName = EventDispatcherSubscriberUtil.getEventNameFromScope(problemDescriptor.psiElement)
            ?: return null

        val taggedEventMethodParameter = EventSubscriberUtil.getTaggedEventMethodParameter(problemDescriptor.psiElement.project, eventName)
        if (taggedEventMethodParameter.isEmpty()) {
            return null
        }

        return taggedEventMethodParameter
            .map { fqn -> importIfNecessary(phpClass, fqn) }
            .joinToString("|")
    }

    private fun importIfNecessary(phpClass: PhpClass, fqn: String): String? {
        val qualifiedName = AnnotationBackportUtil.getQualifiedName(phpClass, fqn)
        if (qualifiedName != null && qualifiedName != StringUtils.stripStart(fqn, "\\")) {
            // class already imported
            return qualifiedName
        }

        return PhpElementsUtil.insertUseIfNecessary(phpClass, fqn)
    }
}

/**
 * getSubscribedEvents method quick fix check
 *
 * return array(
 *   ConsoleEvents::COMMAND => array('onCommanda', 255),
 *   ConsoleEvents::TERMINATE => array('onTerminate', -255),
 * );
 *
 */
private fun visitPhpElement(element: StringLiteralExpression, holder: ProblemsHolder) {
    val parent = element.parent
    if (parent != null && parent.node.elementType === PhpElementTypes.ARRAY_VALUE) {
        val phpReturn = PsiTreeUtil.getParentOfType(parent, PhpReturn::class.java)
        if (phpReturn != null) {
            val method = PsiTreeUtil.getParentOfType(parent, Method::class.java)
            if (method != null) {
                val name = method.name
                if (name == "getSubscribedEvents") {
                    val containingClass = method.containingClass
                    if (containingClass != null && PhpElementsUtil.isInstanceOf(containingClass, "\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface")) {
                        val contents = element.contents
                        if (StringUtils.isNotBlank(contents) && containingClass.findMethodByName(contents) == null) {
                            registerMethodProblem(element, holder, containingClass)
                        }
                    }
                }
            }
        }
    }

    if (parent is ParameterList && PhpElementsUtil.isAttributeNamedArgumentString(element, "\\Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener", "method")) {
        val parentOfType = PsiTreeUtil.getParentOfType(parent, PhpAttribute::class.java)!!
        val owner = parentOfType.owner
        if (owner is PhpClass) {
            val contents = element.contents
            if (contents.isNotBlank() && owner.findMethodByName(contents) == null) {
                registerMethodProblem(element, holder, owner)
            }
        }
    }
}
