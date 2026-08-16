package fr.adrienbrault.idea.symfony2plugin.dic.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.xml.XmlText
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.dic.container.util.ServiceContainerUtil
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.psi.YAMLScalar

private fun visitYamlElement(psiElement: YAMLScalar, holder: ProblemsHolder) {
    val textValue = psiElement.textValue
    if (textValue.startsWith("!php/const:")) {
        val constantName = textValue.substring(11)
        if (StringUtils.isNotBlank(constantName) && ServiceContainerUtil.getTargetsForConstant(holder.project, constantName).isEmpty()) {
            holder.registerProblem(psiElement, ContainerConstantInspection.MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
        }
    }
}

private fun visitXmlElement(psiElement: PsiElement, holder: ProblemsHolder, xmlConstantPattern: ElementPattern<PsiElement>) {
    if (!xmlConstantPattern.accepts(psiElement)) {
        return
    }

    val xmlText = psiElement.parent
    if (xmlText !is XmlText) {
        return
    }

    val value = xmlText.value
    if (StringUtils.isBlank(value)) {
        return
    }

    if (ServiceContainerUtil.getTargetsForConstant(holder.project, value).isEmpty()) {
        holder.registerProblem(xmlText, ContainerConstantInspection.MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    }
}

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class ContainerConstantInspection : LocalInspectionTool() {
    companion object {
        const val MESSAGE = "Symfony: constant not found"
    }

    open class ContainerConstantYamlInspection : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            val project: Project = holder.project
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is YAMLScalar) {
                        visitYamlElement(element, holder)
                    }

                    super.visitElement(element)
                }
            }
        }
    }

    open class ContainerConstantXmlInspection : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            val project: Project = holder.project
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyXmlPsiElementVisitor(holder)
        }

        private class MyXmlPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var xmlConstantPattern: ElementPattern<PsiElement>? = null

            override fun visitElement(element: PsiElement) {
                visitXmlElement(element, holder, getXmlConstantPattern())
                super.visitElement(element)
            }

            private fun getXmlConstantPattern(): ElementPattern<PsiElement> {
                val pattern = xmlConstantPattern ?: XmlHelper.getArgumentValueWithTypePattern("constant")
                xmlConstantPattern = pattern
                return pattern
            }
        }
    }
}
