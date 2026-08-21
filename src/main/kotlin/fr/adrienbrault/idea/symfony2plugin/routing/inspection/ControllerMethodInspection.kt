package fr.adrienbrault.idea.symfony2plugin.routing.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.codeInspection.InspectionUtil
import fr.adrienbrault.idea.symfony2plugin.config.xml.XmlHelper
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.apache.commons.lang3.StringUtils
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class ControllerMethodInspection {
    open class ControllerMethodInspectionYaml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyYamlPsiElementVisitor(holder)
        }

        private class MyYamlPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var controllerScalarPattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                if (getControllerScalarPattern().accepts(element)) {
                    val text = PsiElementUtils.trimQuote(element.text)
                    if (StringUtils.isNotBlank(text)) {
                        InspectionUtil.inspectController(element, text, holder, YamlLazyRouteName(element))
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

    open class ControllerMethodInspectionXml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyXmlPsiElementVisitor(holder)
        }

        private class MyXmlPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var routeControllerPattern: ElementPattern<*>? = null

            override fun visitElement(element: PsiElement) {
                if (getRouteControllerPattern().accepts(element)) {
                    val text = PsiElementUtils.trimQuote(element.text)
                    if (StringUtils.isNotBlank(text)) {
                        InspectionUtil.inspectController(element, text, holder, XmlLazyRouteName(element))
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

    private data class YamlLazyRouteName(private val psiElement: PsiElement) : InspectionUtil.LazyControllerNameResolve {
        override fun getRouteName(): String? {
            val defaultKeyValue = PsiTreeUtil.getParentOfType(psiElement.parent, YAMLKeyValue::class.java)
            if (defaultKeyValue == null) {
                return null
            }

            val def = PsiTreeUtil.getParentOfType(defaultKeyValue, YAMLKeyValue::class.java)
            if (def == null) {
                return null
            }

            return YamlHelper.getYamlKeyName(def)
        }
    }

    private data class XmlLazyRouteName(private val psiElement: PsiElement) : InspectionUtil.LazyControllerNameResolve {
        override fun getRouteName(): String? {
            val defaultTag = PsiTreeUtil.getParentOfType(psiElement, XmlTag::class.java)
            if (defaultTag != null) {
                val routeTag = PsiTreeUtil.getParentOfType(defaultTag, XmlTag::class.java)
                if (routeTag != null) {
                    val id: XmlAttribute? = routeTag.getAttribute("id")
                    if (id != null) {
                        return id.value
                    }
                }
            }

            return null
        }
    }
}
