package fr.adrienbrault.idea.symfony2plugin.translation.intention

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.TranslationStubIndex
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import fr.adrienbrault.idea.symfony2plugin.translation.TranslationKeyIntentionAndQuickFixAction
import fr.adrienbrault.idea.symfony2plugin.translation.dict.TranslationUtil
import org.apache.commons.lang3.StringUtils

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigTranslationKeyIntention : PsiElementBaseIntentionAction() {
    override fun invoke(project: Project, editor: Editor?, psiElement: PsiElement) {
        val pair = getKeyAndDomain(psiElement) ?: return

        TranslationKeyIntentionAndQuickFixAction(pair.first, pair.second, MyKeyDomainNotExistingCollector())
            .invoke(project, editor, psiElement.containingFile)
    }

    override fun isAvailable(project: Project, editor: Editor?, psiElement: PsiElement): Boolean =
        getKeyAndDomain(psiElement) != null

    override fun getFamilyName(): String = "Symfony: create translation key"

    override fun getText(): String = this.familyName

    private fun getKeyAndDomain(psiElement: PsiElement): Pair<String, String>? {
        if (!TwigPattern.getTranslationKeyPattern("trans", "transchoice").accepts(psiElement)) {
            return null
        }

        val key = psiElement.text
        if (StringUtils.isBlank(key)) {
            return null
        }

        // get domain on file scope or method parameter
        val domainName = TwigUtil.getPsiElementTranslationDomain(psiElement)

        // inspection will take care of complete unknown key
        if (!TranslationUtil.hasTranslationKey(psiElement.project, key, domainName)) {
            return null
        }

        return Pair.create(key, domainName)
    }

    /**
     * Collect all domain files that are not providing the given key
     * Known VirtualFiles are filtered out based on the index
     */
    private class MyKeyDomainNotExistingCollector : TranslationKeyIntentionAndQuickFixAction.DomainCollector {
        override fun collect(project: Project, key: String, domain: String): Collection<PsiFile> {
            return TranslationUtil.getDomainPsiFiles(project, domain)
                .filter { psiFile -> !isDomainAndKeyInPsi(psiFile, key, domain) }
        }

        private fun isDomainAndKeyInPsi(psiFile: PsiFile, key: String, domain: String): Boolean {
            val values: List<Set<String>> = FileBasedIndex.getInstance()
                .getValues(TranslationStubIndex.KEY, domain, GlobalSearchScope.fileScope(psiFile))

            for (value in values) {
                if (key in value) {
                    return true
                }
            }

            return false
        }
    }
}
