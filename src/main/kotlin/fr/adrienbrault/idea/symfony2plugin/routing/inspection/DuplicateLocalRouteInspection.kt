package fr.adrienbrault.idea.symfony2plugin.routing.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlAttributeValue
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocComment
import com.jetbrains.php.lang.documentation.phpdoc.psi.tags.PhpDocTag
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.PhpAttribute
import com.jetbrains.php.lang.psi.elements.PhpClass
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import de.espend.idea.php.annotation.dict.PhpDocCommentAnnotation
import de.espend.idea.php.annotation.dict.PhpDocTagAnnotation
import de.espend.idea.php.annotation.pattern.AnnotationPattern
import de.espend.idea.php.annotation.util.AnnotationUtil
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.config.xml.inspection.XmlDuplicateServiceKeyInspection
import fr.adrienbrault.idea.symfony2plugin.routing.RouteHelper
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil
import fr.adrienbrault.idea.symfony2plugin.util.yaml.YamlHelper
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import java.util.ArrayList

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class DuplicateLocalRouteInspection : LocalInspectionTool() {
    companion object {
        private const val MESSAGE = "Symfony: Duplicate route name"
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        override fun visitElement(element: PsiElement) {
            if (element is YAMLKeyValue && element.language === YAMLLanguage.INSTANCE) {
                visitYaml(element)
            } else if (element is StringLiteralExpression && element.language === PhpLanguage.INSTANCE) {
                visitPhp(element)
            } else if (element is XmlAttributeValue) {
                XmlDuplicateServiceKeyInspection.visitRoot(element, holder, "routes", "route", "id", MESSAGE)
            }

            super.visitElement(element)
        }

        private fun visitYaml(yamlKeyValue: YAMLKeyValue) {
            val yamlMapping = yamlKeyValue.parent
            if (YamlHelper.isRoutingFile(yamlKeyValue.containingFile) && yamlMapping is YAMLMapping && yamlMapping.parent is YAMLDocument) {
                var keyText1: String? = null

                var found = 0
                for (keyValue in yamlMapping.keyValues) {
                    val keyText = keyValue.keyText

                    // lazy
                    if (keyText1 == null) {
                        keyText1 = yamlKeyValue.keyText
                    }

                    if (keyText1.equals(keyText)) {
                        found++
                    }

                    if (found == 2) {
                        val keyElement = yamlKeyValue.key
                        assert(keyElement != null)
                        holder.registerProblem(keyElement!!, "Symfony: Duplicate route name", ProblemHighlightType.GENERIC_ERROR_OR_WARNING)

                        break
                    }
                }
            }
        }

        private fun visitPhp(element: StringLiteralExpression) {
            if (PhpElementsUtil.isAttributeNamedArgumentString(element, "\\Symfony\\Component\\Routing\\Annotation\\Route", "name") || PhpElementsUtil.isAttributeNamedArgumentString(element, "\\Symfony\\Component\\Routing\\Attribute\\Route", "name")) {
                val parentOfType = PsiTreeUtil.getParentOfType(element, PhpAttribute::class.java)
                val owner = parentOfType!!.owner
                if (owner is Method && owner.access.isPublic && owner.containingClass != null) {
                    var found = 0
                    val contents = element.contents

                    for (ownMethod in owner.containingClass!!.ownMethods) {
                        val attributes: MutableCollection<PhpAttribute> = ArrayList(ownMethod.getAttributes("\\Symfony\\Component\\Routing\\Annotation\\Route"))
                        attributes.addAll(ownMethod.getAttributes("\\Symfony\\Component\\Routing\\Attribute\\Route"))

                        for (attribute in attributes) {
                            val name = PhpElementsUtil.getAttributeArgumentStringByName(attribute, "name")
                            if (contents == name) {
                                found++
                            }

                            if (found == 2) {
                                holder.registerProblem(element, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                                return
                            }
                        }
                    }
                }
            }

            if (AnnotationPattern.getPropertyIdentifierValue("name").accepts(element)) {
                val phpDocTag: PhpDocTag? = PsiTreeUtil.getParentOfType(element, PhpDocTag::class.java)
                if (phpDocTag != null) {
                    val phpClass: PhpClass? = AnnotationUtil.getAnnotationReference(phpDocTag)
                    if (phpClass != null && RouteHelper.isRouteClassAnnotation(phpClass.fqn)) {
                        val phpDocComment: PhpDocComment? = PsiTreeUtil.getParentOfType(element, PhpDocComment::class.java)
                        val nextPsiSibling = phpDocComment!!.nextPsiSibling
                        if (nextPsiSibling is Method && nextPsiSibling.access.isPublic && nextPsiSibling.containingClass != null) {
                            var found = 0
                            val contents = element.contents

                            for (ownMethod in nextPsiSibling.containingClass!!.ownMethods) {
                                val phpClassContainer: PhpDocCommentAnnotation? = AnnotationUtil.getPhpDocCommentAnnotationContainer(ownMethod.docComment)
                                if (phpClassContainer != null) {
                                    val firstPhpDocBlock: PhpDocTagAnnotation? = phpClassContainer.getFirstPhpDocBlock(*RouteHelper.ROUTE_ANNOTATIONS)
                                    if (firstPhpDocBlock != null) {
                                        val name = firstPhpDocBlock.getPropertyValue("name")
                                        if (contents == name) {
                                            found++
                                        }

                                        if (found == 2) {
                                            holder.registerProblem(element, MESSAGE, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
                                            return
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
