package fr.adrienbrault.idea.symfony2plugin.routing

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class RouteControllerDeprecatedInspection {
    open class RouteControllerDeprecatedXmlLocalInspectionTool : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            val project = holder.project
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyXmlPsiElementVisitor(project, holder)
        }

        private class MyXmlPsiElementVisitor(private val project: Project, private val holder: ProblemsHolder) : PsiElementVisitor() {
            private val routeControllerPattern by lazy(LazyThreadSafetyMode.NONE) { XmlHelper.getRouteControllerPattern() }

            override fun visitElement(element: PsiElement) {
                if (routeControllerPattern.accepts(element)) {
                    inspectController(element)
                }

                super.visitElement(element)
            }

            private fun inspectController(element: PsiElement) {
                val parent = element.parent ?: return
                val text = RouteXmlReferenceContributor.getControllerText(parent) ?: return
                hasDeprecatedActionOrClass(project, element, text, holder)
            }
        }
    }

    open class RouteControllerDeprecatedYamlLocalInspectionTool : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            val project = holder.project
            if (!Symfony2ProjectComponent.isEnabled(project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyYamlPsiElementVisitor(project, holder)
        }

        private class MyYamlPsiElementVisitor(private val project: Project, private val holder: ProblemsHolder) : PsiElementVisitor() {
            private val controllerScalarPattern by lazy(LazyThreadSafetyMode.NONE) {
                YamlElementPatternHelper.getSingleLineScalarKey("_controller", "controller")
            }

            override fun visitElement(element: PsiElement) {
                if (controllerScalarPattern.accepts(element)) {
                    val text = PsiElementUtils.trimQuote(element.text)
                    if (text.isNotBlank()) {
                        hasDeprecatedActionOrClass(project, element, text, holder)
                    }
                }

                super.visitElement(element)
            }
        }
    }
}

private fun hasDeprecatedActionOrClass(project: Project, element: PsiElement, text: String, holder: ProblemsHolder) {
    if (RouteHelper.isControllerActionDeprecated(project, text)) {
        holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED)
    }
}
