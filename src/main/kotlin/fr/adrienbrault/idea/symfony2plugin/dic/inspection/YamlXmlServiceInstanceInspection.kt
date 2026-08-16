package fr.adrienbrault.idea.symfony2plugin.dic.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.php.lang.psi.elements.Method
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import fr.adrienbrault.idea.symfony2plugin.dic.inspection.intention.YamlSuggestIntentionAction
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.dict.ServiceUtil
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.psi.YAMLScalar

private fun registerInstanceProblem(psiElement: PsiElement, holder: ProblemsHolder, parameterIndex: Int, constructor: Method, lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector) {
    val serviceName = getServiceName(psiElement)
    if (StringUtils.isBlank(serviceName)) {
        return
    }

    val serviceParameterClass = ServiceUtil.getResolvedClassDefinition(holder.project, getServiceName(psiElement), lazyServiceCollector)
    if (serviceParameterClass == null) {
        return
    }

    val constructorParameter = constructor.parameters
    if (parameterIndex >= constructorParameter.size) {
        return
    }

    val expectedClass = PhpElementsUtil.getClassInterface(holder.project, constructorParameter[parameterIndex].declaredType.toString())
    if (expectedClass == null) {
        return
    }

    if (!PhpElementsUtil.isInstanceOf(serviceParameterClass, expectedClass)) {
        holder.registerProblem(
            psiElement,
            "Expect instance of: " + expectedClass.presentableFQN,
            YamlSuggestIntentionAction(expectedClass.fqn, psiElement)
        )
    }
}

private fun getServiceName(psiElement: PsiElement): String {
    return YamlHelper.trimSpecialSyntaxServiceName(PsiElementUtils.getText(psiElement))
}

/**
 * foo:
 *  class: Foo
 *  arguments: [@<caret>]
 *  arguments:
 *      - @<caret>
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class YamlXmlServiceInstanceInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var lazyServiceCollector: ContainerCollectionResolver.LazyServiceCollector? = null

        private var insideServicesPattern: ElementPattern<PsiElement>? = null

        override fun visitElement(psiElement: PsiElement) {
            // only match inside service definitions
            if (YamlHelper.isStringValue(psiElement) && getInsideServicesPattern().accepts(psiElement)) {
                visitConstructor(psiElement)
                visitCall(psiElement)
            }

            super.visitElement(psiElement)
        }

        /**
         * class: FooClass
         * tags:
         *  - [ setFoo, [@args_bar] ]
         */
        private fun visitCall(psiElement: PsiElement) {
            val yamlScalar = psiElement.context
            if (yamlScalar !is YAMLScalar) {
                return
            }

            YamlHelper.visitServiceCallArgument(yamlScalar) { visitor ->
                val serviceClass = ServiceUtil.getResolvedClassDefinition(holder.project, visitor.className, getLazyServiceCollector(holder.project))
                if (serviceClass == null) {
                    return@visitServiceCallArgument
                }

                val method = serviceClass.findMethodByName(visitor.method)
                if (method == null) {
                    return@visitServiceCallArgument
                }

                registerInstanceProblem(psiElement, holder, visitor.parameterIndex, method, getLazyServiceCollector(holder.project))
            }
        }

        /**
         * foo:
         *  class: Foo
         *  arguments: [@<caret>]
         *  arguments:
         *      - @<caret>
         */
        private fun visitConstructor(psiElement: PsiElement) {
            val methodTypeHint = ServiceContainerUtil.getYamlConstructorTypeHint(psiElement, getLazyServiceCollector(holder.project))
            if (methodTypeHint == null) {
                return
            }

            registerInstanceProblem(psiElement, holder, methodTypeHint.index, methodTypeHint.method, getLazyServiceCollector(holder.project))
        }

        private fun getLazyServiceCollector(project: Project): ContainerCollectionResolver.LazyServiceCollector {
            val collector = lazyServiceCollector ?: ContainerCollectionResolver.LazyServiceCollector(project)
            lazyServiceCollector = collector
            return collector
        }

        private fun getInsideServicesPattern(): ElementPattern<PsiElement> {
            val pattern = insideServicesPattern ?: YamlElementPatternHelper.getInsideKeyValue("services")
            insideServicesPattern = pattern
            return pattern
        }
    }

}
