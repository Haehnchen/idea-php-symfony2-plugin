package fr.adrienbrault.idea.symfony2plugin.dic.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.quickfix.CorrectClassNameCasingYamlLocalQuickFix
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.YAMLTokenTypes
import org.jetbrains.yaml.psi.YAMLKeyValue

private fun inspectYamlClass(psiElement: PsiElement, holder: ProblemsHolder) {
    val className = PsiElementUtils.getText(psiElement)

    val project = holder.project

    if (YamlHelper.isValidParameterName(className)) {
        val resolvedParameter = ContainerCollectionResolver.resolveParameter(project, className)
        if (resolvedParameter != null && PhpElementsUtil.hasClassOrInterface(project, resolvedParameter)) {
            return
        }
    }

    val foundClass = PhpElementsUtil.getClassInterface(project, className)
    if (foundClass == null) {
        holder.registerProblem(psiElement, YamlClassInspection.MESSAGE_MISSING_CLASS, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    } else if (!foundClass.presentableFQN.equals(className)) {
        holder.registerProblem(psiElement, YamlClassInspection.MESSAGE_WRONG_CASING, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, CorrectClassNameCasingYamlLocalQuickFix(foundClass.presentableFQN))
    }
}

/**
 * Check if class exists
 *
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class YamlClassInspection : LocalInspectionTool() {
    companion object {
        const val MESSAGE_WRONG_CASING = "Wrong class casing"
        const val MESSAGE_MISSING_CLASS = "Missing class"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var singleLineClassPattern: ElementPattern<*>? = null
        private var parameterClassPattern: ElementPattern<*>? = null
        private var insideServiceKeyPattern: ElementPattern<*>? = null
        private var serviceIdKeyValuePattern: ElementPattern<*>? = null

        override fun visitElement(psiElement: PsiElement) {
            if ((getSingleLineClassPattern().accepts(psiElement) || getParameterClassPattern().accepts(psiElement)) && getInsideServiceKeyPattern().accepts(psiElement)) {
                // foobar.foo:
                //   class: Foobar\Foo
                inspectYamlClass(psiElement, holder)
            } else if (psiElement.node.elementType == YAMLTokenTypes.SCALAR_KEY && getServiceIdKeyValuePattern().accepts(psiElement.parent)) {
                // Foobar\Foo: ~
                val text = PsiElementUtils.getText(psiElement)
                if (StringUtils.isNotBlank(text) && YamlHelper.isClassServiceId(text) && text.contains("\\")) {
                    val yamlKeyValue = psiElement.parent
                    if (yamlKeyValue is YAMLKeyValue && YamlHelper.getYamlKeyValue(yamlKeyValue, "resource") == null && YamlHelper.getYamlKeyValue(yamlKeyValue, "exclude") == null) {
                        inspectYamlClass(psiElement, holder)
                    }
                }
            }

            super.visitElement(psiElement)
        }

        private fun getSingleLineClassPattern(): ElementPattern<*> {
            val pattern = singleLineClassPattern ?: YamlElementPatternHelper.getSingleLineScalarKey("class", "factory_class")
            singleLineClassPattern = pattern
            return pattern
        }

        private fun getParameterClassPattern(): ElementPattern<*> {
            val pattern = parameterClassPattern ?: YamlElementPatternHelper.getParameterClassPattern()
            parameterClassPattern = pattern
            return pattern
        }

        private fun getInsideServiceKeyPattern(): ElementPattern<*> {
            val pattern = insideServiceKeyPattern ?: YamlElementPatternHelper.getInsideServiceKeyPattern()
            insideServiceKeyPattern = pattern
            return pattern
        }

        private fun getServiceIdKeyValuePattern(): ElementPattern<*> {
            val pattern = serviceIdKeyValuePattern ?: YamlElementPatternHelper.getServiceIdKeyValuePattern()
            serviceIdKeyValuePattern = pattern
            return pattern
        }
    }
}
