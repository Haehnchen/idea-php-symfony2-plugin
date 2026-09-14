package fr.adrienbrault.idea.symfony2plugin.routing

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import org.apache.commons.lang3.StringUtils

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
            private var routeControllerPattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                if (getRouteControllerPattern().accepts(element)) {
                    val parent = element.parent
                    if (parent != null) {
                        val text = RouteXmlReferenceContributor.getControllerText(parent)
                        if (text != null) {
                            hasDeprecatedActionOrClass(project, element, text, holder)
                        }
                    }
                }

                super.visitElement(element)
            }

            private fun getRouteControllerPattern(): ElementPattern<*> {
                if (routeControllerPattern != null) {
                    return routeControllerPattern!!
                }

                routeControllerPattern = XmlHelper.getRouteControllerPattern()
                return routeControllerPattern!!
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
            private var controllerScalarPattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                if (getControllerScalarPattern().accepts(element)) {
                    val text = PsiElementUtils.trimQuote(element.text)
                    if (StringUtils.isNotBlank(text)) {
                        hasDeprecatedActionOrClass(project, element, text, holder)
                    }
                }

                super.visitElement(element)
            }

            private fun getControllerScalarPattern(): ElementPattern<*> {
                if (controllerScalarPattern != null) {
                    return controllerScalarPattern!!
                }

                controllerScalarPattern = YamlElementPatternHelper.getSingleLineScalarKey("_controller", "controller")
                return controllerScalarPattern!!
            }
        }
    }
}

private fun hasDeprecatedActionOrClass(project: Project, element: PsiElement, text: String, holder: ProblemsHolder) {
    if (RouteHelper.isControllerActionDeprecated(project, text)) {
        holder.registerProblem(element, "Symfony: Controller action is deprecated", ProblemHighlightType.LIKE_DEPRECATED)
    }
}
