package fr.adrienbrault.idea.symfony2plugin.dic.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.xml.XmlAttribute
import com.intellij.psi.xml.XmlTag
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.yaml.YamlElementPatternHelper
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils
import fr.adrienbrault.idea.symfony2plugin.util.psi.PsiElementAssertUtil
import org.jetbrains.yaml.psi.YAMLKeyValue

private fun registerYmlRoutePatternProblem(holder: ProblemsHolder, element: YAMLKeyValue, insideServiceKeyPattern: ElementPattern<PsiElement>) {
    val s = PsiElementUtils.trimQuote(element.keyText)
    if ((s == "factory_class" || s == "factory_method" || s == "factory_service") && insideServiceKeyPattern.accepts(element)) {
        // services:
        //   foo:
        //      factory_*:
        registerProblem(holder, element.key)
    }
}

private fun registerXmlAttributeProblem(holder: ProblemsHolder, xmlAttribute: XmlAttribute) {
    val name = xmlAttribute.name
    if (!(name == "factory-class" || name == "factory-method" || name == "factory-service")) {
        return
    }

    val xmlTagRoute = PsiElementAssertUtil.getParentOfTypeWithNameOrNull(xmlAttribute, XmlTag::class.java, "service")
    if (xmlTagRoute != null) {
        registerProblem(holder, xmlAttribute.firstChild)
    }
}

private fun registerProblem(holder: ProblemsHolder, target: PsiElement?) {
    if (target == null) {
        return
    }

    holder.registerProblem(target, "Symfony: this factory pattern is deprecated use 'factory' instead", ProblemHighlightType.LIKE_DEPRECATED)
}

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class ContainerSettingDeprecatedInspection {
    open class ContainerSettingDeprecatedInspectionYaml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return MyYamlPsiElementVisitor(holder)
        }

        private class MyYamlPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
            private var insideServiceKeyPattern: ElementPattern<PsiElement>? = null

            override fun visitElement(element: PsiElement) {
                if (element is YAMLKeyValue) {
                    registerYmlRoutePatternProblem(holder, element, getInsideServiceKeyPattern())
                }

                super.visitElement(element)
            }

            private fun getInsideServiceKeyPattern(): ElementPattern<PsiElement> {
                val pattern = insideServiceKeyPattern ?: YamlElementPatternHelper.getInsideServiceKeyPattern()
                insideServiceKeyPattern = pattern
                return pattern
            }
        }
    }

    open class ContainerSettingDeprecatedInspectionXml : LocalInspectionTool() {
        override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
            if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
                return super.buildVisitor(holder, isOnTheFly)
            }

            return object : PsiElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is XmlAttribute) {
                        registerXmlAttributeProblem(holder, element)
                    }

                    super.visitElement(element)
                }
            }
        }
    }
}
