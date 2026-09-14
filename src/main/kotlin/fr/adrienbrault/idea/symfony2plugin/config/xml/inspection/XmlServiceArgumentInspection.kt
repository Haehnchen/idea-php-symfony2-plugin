package fr.adrienbrault.idea.symfony2plugin.config.xml.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlTokenType
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.action.ServiceActionUtil
import fr.adrienbrault.idea.symfony2plugin.action.quickfix.AddServiceXmlArgumentLocalQuickFix
import fr.adrienbrault.idea.symfony2plugin.stubs.ContainerCollectionResolver
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
open class XmlServiceArgumentInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        if (!Symfony2ProjectComponent.isEnabled(holder.project)) {
            return super.buildVisitor(holder, isOnTheFly)
        }

        return MyPsiElementVisitor(holder)
    }

    private class MyPsiElementVisitor(private val holder: ProblemsHolder) : PsiElementVisitor() {
        private var serviceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>? = null

        override fun visitElement(element: PsiElement) {
            if (element is XmlTag) {
                visitService(element, holder, createLazyServiceCollector())
            }

            super.visitElement(element)
        }

        private fun visitService(
            xmlTag: XmlTag,
            holder: ProblemsHolder,
            lazyServiceCollector: NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector>
        ) {
            if (!ServiceActionUtil.isValidXmlParameterInspectionService(xmlTag)) {
                return
            }

            val args = ServiceActionUtil.getXmlMissingArgumentTypes(xmlTag, false, lazyServiceCollector.get())
            if (args.isEmpty()) {
                return
            }

            val childrenOfType = PsiElementUtils.getChildrenOfType(xmlTag, PlatformPatterns.psiElement(XmlTokenType.XML_NAME))
                ?: return

            holder.registerProblem(
                childrenOfType,
                "Missing argument",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                AddServiceXmlArgumentLocalQuickFix(args)
            )
        }

        private fun createLazyServiceCollector(): NotNullLazyValue<ContainerCollectionResolver.LazyServiceCollector> {
            if (serviceCollector == null) {
                serviceCollector = NotNullLazyValue.lazy { ContainerCollectionResolver.LazyServiceCollector(holder.project) }
            }

            return serviceCollector!!
        }
    }
}
