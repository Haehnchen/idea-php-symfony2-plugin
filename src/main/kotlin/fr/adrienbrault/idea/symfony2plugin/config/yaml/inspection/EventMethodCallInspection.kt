package fr.adrienbrault.idea.symfony2plugin.config.yaml.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.util.NotNullLazyValue
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
        private val serviceCollector by lazy(LazyThreadSafetyMode.NONE) {
            NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
        }
        private val yamlTagsMethodPattern by lazy(LazyThreadSafetyMode.NONE) {
            StandardPatterns.and(
                YamlElementPatternHelper.getInsideKeyValue("tags"),
                YamlElementPatternHelper.getSingleLineScalarKey("method")
            )
        }
        private val yamlTextPattern by lazy(LazyThreadSafetyMode.NONE) {
            PlatformPatterns.psiElement(YAMLTokenTypes.TEXT)
        }
        private val yamlDoubleQuotedScalarPattern by lazy(LazyThreadSafetyMode.NONE) {
            PlatformPatterns.psiElement(YAMLTokenTypes.SCALAR_DSTRING)
        }
        private val yamlInsideCallsPattern by lazy(LazyThreadSafetyMode.NONE) {
            YamlElementPatternHelper.getInsideKeyValue("calls")
        }
        private val xmlTagMethodPattern by lazy(LazyThreadSafetyMode.NONE) {
            XmlHelper.getTagAttributePattern("tag", "method")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern())
        }
        private val xmlCallMethodPattern by lazy(LazyThreadSafetyMode.NONE) {
            XmlHelper.getTagAttributePattern("call", "method")
                .inside(XmlHelper.getInsideTagPattern("services"))
                .inFile(XmlHelper.getXmlFilePattern())
        }

        override fun visitElement(element: PsiElement) {
            val language = element.language

            when {
                language === YAMLLanguage.INSTANCE -> visitYmlElement(element)
                language === XMLLanguage.INSTANCE -> visitXmlElement(element)
                language === PhpLanguage.INSTANCE && element is StringLiteralExpression ->
                    visitPhpElement(element, holder)
            }

            super.visitElement(element)
        }

        private fun visitXmlElement(element: PsiElement) {
            val isSupportedTag = xmlTagMethodPattern.accepts(element) || xmlCallMethodPattern.accepts(element)

            if (isSupportedTag) {
                // attach to text child only
                val psiElements = element.children
                if (psiElements.size < 2) {
                    return
                }

                val serviceClass = XmlHelper.getServiceDefinitionClass(element)
                    ?.takeIf { it.isNotBlank() }
                    ?: return
                registerMethodProblem(psiElements[1], serviceClass)
            }
        }

        private fun visitYamlMethodTagKey(psiElement: PsiElement) {
            val methodName = PsiElementUtils.trimQuote(psiElement.text)
            if (methodName.isBlank()) {
                return
            }

            val classValue = YamlHelper.getServiceDefinitionClassFromTagMethod(psiElement) ?: return

            registerMethodProblem(psiElement, classValue)
        }

        private fun visitYmlElement(psiElement: PsiElement) {
            if (yamlTagsMethodPattern.accepts(psiElement)) {
                visitYamlMethodTagKey(psiElement)
            }

            if (yamlTextPattern.accepts(psiElement) || yamlDoubleQuotedScalarPattern.accepts(psiElement)) {
                visitYamlMethod(psiElement)
            }
        }

        private fun visitYamlMethod(psiElement: PsiElement) {
            if (yamlInsideCallsPattern.accepts(psiElement)) {
                val parent = psiElement.parent
                if (parent is YAMLScalar) {
                    YamlHelper.visitServiceCall(parent) { service ->
                        registerMethodProblem(psiElement, YamlHelper.trimSpecialSyntaxServiceName(service))
                    }
                }
            }
        }

        private fun registerMethodProblem(psiElement: PsiElement, classKeyValue: String) =
            registerMethodProblem(
                psiElement,
                holder,
                ServiceUtil.getResolvedClassDefinition(
                    psiElement.project,
                    classKeyValue,
                    serviceCollector.get()
                )
            )
    }
}

private fun registerMethodProblem(psiElement: PsiElement, holder: ProblemsHolder, phpClass: PhpClass?) {
    val resolvedClass = phpClass ?: return

    val methodName = PsiElementUtils.trimQuote(psiElement.text)
    if (resolvedClass.findMethodByName(methodName) != null) {
        return
    }

    holder.registerProblem(
        psiElement,
        "Missing Method",
        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
        CreateMethodQuickFix(resolvedClass, methodName, MyCreateMethodQuickFix())
    )
}

private class MyCreateMethodQuickFix : CreateMethodQuickFix.InsertStringInterface {
    override fun getStringBuilder(problemDescriptor: ProblemDescriptor, phpClass: PhpClass, functionName: String): StringBuilder {
        val eventType = getEventTypeHint(problemDescriptor, phpClass)
        val parameter = if (eventType != null) $$"$$eventType $event" else ""

        return StringBuilder("public function $functionName($parameter)\n {\n}\n\n")
    }

    private fun getEventTypeHint(problemDescriptor: ProblemDescriptor, phpClass: PhpClass): String? {
        val eventName = EventDispatcherSubscriberUtil.getEventNameFromScope(problemDescriptor.psiElement)
            ?: return null

        val taggedEventMethodParameter = EventSubscriberUtil.getTaggedEventMethodParameter(problemDescriptor.psiElement.project, eventName)
        if (taggedEventMethodParameter.isEmpty()) {
            return null
        }

        return taggedEventMethodParameter.joinToString("|") { importIfNecessary(phpClass, it) ?: "null" }
    }

    private fun importIfNecessary(phpClass: PhpClass, fqn: String): String? {
        val qualifiedName = AnnotationBackportUtil.getQualifiedName(phpClass, fqn)
        if (qualifiedName != null && qualifiedName != fqn.trimStart('\\')) {
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
    if (parent?.node?.elementType === PhpElementTypes.ARRAY_VALUE &&
        PsiTreeUtil.getParentOfType(parent, PhpReturn::class.java) != null
    ) {
        val method = PsiTreeUtil.getParentOfType(parent, Method::class.java)
        val containingClass = method?.containingClass
        if (method?.name == "getSubscribedEvents" &&
            containingClass != null &&
            PhpElementsUtil.isInstanceOf(
                containingClass,
                "\\Symfony\\Component\\EventDispatcher\\EventSubscriberInterface"
            )
        ) {
            val contents = element.contents
            if (contents.isNotBlank() && containingClass.findMethodByName(contents) == null) {
                registerMethodProblem(element, holder, containingClass)
            }
        }
    }

    if (parent is ParameterList && PhpElementsUtil.isAttributeNamedArgumentString(
            element,
            "\\Symfony\\Component\\EventDispatcher\\Attribute\\AsEventListener",
            "method"
        )
    ) {
        val owner = PsiTreeUtil.getParentOfType(parent, PhpAttribute::class.java)?.owner
        if (owner is PhpClass) {
            val contents = element.contents
            if (contents.isNotBlank() && owner.findMethodByName(contents) == null) {
                registerMethodProblem(element, holder, owner)
            }
        }
    }
}
