package fr.adrienbrault.idea.symfony2plugin.dic.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.intellij.psi.xml.XmlTag
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.dic.container.suggestion.XmlServiceSuggestIntentionAction
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import org.apache.commons.lang3.StringUtils

private fun annotateServiceInstance(psiElement: PsiElement, holder: ProblemsHolder, lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector) {
    // search for parent service definition
    val currentXmlTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag::class.java) ?: return
    val parentXmlTag = PsiTreeUtil.getParentOfType(currentXmlTag, XmlTag::class.java)
    if (parentXmlTag == null) {
        return
    }

    val name = parentXmlTag.name
    if (name == "service") {
        // service/argument[id]
        val serviceDefName = XmlHelper.getClassFromServiceDefinition(parentXmlTag)
        if (serviceDefName != null) {
            val phpClass = ServiceUtil.getResolvedClassDefinition(holder.project, serviceDefName, lazyServiceCollector)

            // check type hint on constructor
            if (phpClass != null) {
                val constructor = phpClass.constructor
                if (constructor != null) {
                    val serviceName = (psiElement as XmlAttributeValue).value
                    if (StringUtils.isNotBlank(serviceName)) {
                        attachMethodInstances(psiElement, serviceName, constructor, XmlHelper.getArgumentIndex(currentXmlTag, constructor), holder, lazyServiceCollector)
                    }
                }
            }
        }
    } else if (name == "call") {

        // service/call/argument[id]

        val methodAttribute = parentXmlTag.getAttribute("method")
        if (methodAttribute != null) {
            val methodName = methodAttribute.value
            val serviceTag = parentXmlTag.parentTag

            // get service class
            if (serviceTag != null && "service" == serviceTag.name) {
                val serviceDefName = XmlHelper.getClassFromServiceDefinition(serviceTag)
                if (serviceDefName != null) {
                    val phpClass = ServiceUtil.getResolvedClassDefinition(holder.project, serviceDefName, lazyServiceCollector)

                    // finally check method type hint
                    if (phpClass != null) {
                        val method = phpClass.findMethodByName(methodName)
                        if (method != null) {
                            val serviceName = (psiElement as XmlAttributeValue).value
                            if (StringUtils.isNotBlank(serviceName)) {
                                attachMethodInstances(psiElement, serviceName, method, XmlHelper.getArgumentIndex(currentXmlTag, method), holder, lazyServiceCollector)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun attachMethodInstances(target: PsiElement, serviceName: String, method: Method, parameterIndex: Int, holder: ProblemsHolder, lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector) {
    val constructorParameter = method.parameters
    if (parameterIndex >= constructorParameter.size) {
        return
    }

    val className = constructorParameter[parameterIndex].declaredType.toString()
    val expectedClass = PhpElementsUtil.getClassInterface(holder.project, className)
    if (expectedClass == null) {
        return
    }

    val serviceParameterClass = ServiceUtil.getResolvedClassDefinition(holder.project, serviceName, lazyServiceCollector)
    if (serviceParameterClass != null && !PhpElementsUtil.isInstanceOf(serviceParameterClass, expectedClass)) {
        holder.registerProblem(
            target,
            "Expect instance of: " + expectedClass.presentableFQN,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            XmlServiceSuggestIntentionAction(expectedClass.fqn, target)
        )
    }
}

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class XmlServiceInstanceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector? = null
        private var argumentServiceIdPattern: ElementPattern<*>? = null

        override fun visitElement(psiElement: PsiElement) {
            if (getArgumentServiceIdPattern().accepts(psiElement)) {
                val collector = lazyServiceCollector ?: ContainerCollectionResolver.LazyServiceCollector(holder.project)
                lazyServiceCollector = collector

                annotateServiceInstance(psiElement, holder, collector)
            }

            super.visitElement(psiElement)
        }

        private fun getArgumentServiceIdPattern(): ElementPattern<*> {
            val pattern = argumentServiceIdPattern ?: XmlHelper.getArgumentServiceIdPattern()
            argumentServiceIdPattern = pattern
            return pattern
        }
    }

}
