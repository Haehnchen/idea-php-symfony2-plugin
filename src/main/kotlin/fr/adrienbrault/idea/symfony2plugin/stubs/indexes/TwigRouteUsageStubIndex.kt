package fr.adrienbrault.idea.symfony2plugin.stubs.indexes

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.jetbrains.twig.TwigFile
import com.jetbrains.twig.TwigFileType
import com.jetbrains.twig.TwigTokenTypes
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.externalizer.StringSetDataExternalizer
import fr.adrienbrault.idea.symfony2plugin.templating.TwigPattern
import fr.adrienbrault.idea.symfony2plugin.templating.util.TwigUtil
import org.apache.commons.lang3.StringUtils
import java.util.Locale

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 */
class TwigRouteUsageStubIndex : FileBasedIndexExtension<String, Set<String>>() {
    override fun getName(): ID<String, Set<String>> = KEY

    override fun getIndexer(): DataIndexer<String, Set<String>, FileContent> = DataIndexer { inputData ->
        val map = HashMap<String, MutableSet<String>>()
        val psiFile = inputData.psiFile
        if (!Symfony2ProjectComponent.isEnabledForIndex(psiFile.project) || psiFile !is TwigFile) {
            return@DataIndexer map
        }

        for (usage in getRouteUsages(psiFile)) {
            map.putIfAbsent(usage.routeName, hashSetOf())
            map[usage.routeName]!!.add(usage.kind.name.lowercase(Locale.ROOT))
        }

        map
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = KEY_DESCRIPTOR

    override fun getValueExternalizer(): DataExternalizer<Set<String>> = DATA_EXTERNALIZER

    override fun getVersion(): Int = 1

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { it.fileType == TwigFileType.INSTANCE }

    override fun dependsOnFileContent(): Boolean = true

    enum class UsageKind {
        PATH,
        URL,
        COMPARE,
        SAME_AS,
        IN_ARRAY,
    }

    data class Usage(val routeName: String, val target: PsiElement, val kind: UsageKind)

    companion object {
        @JvmField
        val KEY: ID<String, Set<String>> = ID.create("fr.adrienbrault.idea.symfony2plugin.twig_route_usages")

        private val KEY_DESCRIPTOR: KeyDescriptor<String> = EnumeratorStringDescriptor()
        private val DATA_EXTERNALIZER: DataExternalizer<Set<String>> = StringSetDataExternalizer()

        @JvmStatic
        fun getRouteUsages(twigFile: TwigFile, routeNames: Collection<String>): Collection<Usage> {
            if (routeNames.isEmpty()) {
                return emptyList()
            }

            val routeNameSet = routeNames.toHashSet()
            val usages = LinkedHashSet<Usage>()
            for (usage in getRouteUsages(twigFile)) {
                if (routeNameSet.contains(usage.routeName)) {
                    usages.add(usage)
                }
            }

            return usages
        }

        @JvmStatic
        fun getUsageKind(element: PsiElement): UsageKind? {
            if (element.node == null ||
                element.node.elementType != TwigTokenTypes.STRING_TEXT ||
                !TwigUtil.isValidStringWithoutInterpolatedOrConcat(element)
            ) {
                return null
            }

            if (TwigPattern.getAutocompletableRoutePattern().accepts(element)) {
                return getFunctionUsage(element)?.kind
            }

            if (TwigPattern.getTwigRouteComparePattern().accepts(element) && TwigPattern.isRouteCompareContext(element)) {
                return UsageKind.COMPARE
            }

            if (TwigPattern.getTwigRouteSameAsPattern().accepts(element) && TwigPattern.isRouteCompareContext(element)) {
                return UsageKind.SAME_AS
            }

            if (TwigPattern.getTwigRouteInArrayPattern().accepts(element) && TwigPattern.isRouteCompareContext(element)) {
                return UsageKind.IN_ARRAY
            }

            return null
        }

        private fun getRouteUsages(twigFile: TwigFile): Collection<Usage> {
            val usages = LinkedHashSet<Usage>()
            twigFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element.node != null && element.node.elementType == TwigTokenTypes.STRING_TEXT) {
                        val usageKind = getUsageKind(element)
                        val usage = usageKind?.let { Usage(element.text, element, it) }
                        if (usage != null && StringUtils.isNotBlank(usage.routeName)) {
                            usages.add(usage)
                        }
                    }

                    super.visitElement(element)
                }
            })

            return usages
        }

        private fun getFunctionUsage(element: PsiElement): Usage? {
            return when (getPrecedingFunctionName(element)) {
                "path" -> Usage(element.text, element, UsageKind.PATH)
                "url" -> Usage(element.text, element, UsageKind.URL)
                else -> null
            }
        }

        private fun getPrecedingFunctionName(element: PsiElement): String? {
            var prev = PsiTreeUtil.prevLeaf(element)
            while (prev != null) {
                if (prev.node == null) {
                    prev = PsiTreeUtil.prevLeaf(prev)
                    continue
                }

                if (prev.node.elementType == TwigTokenTypes.IDENTIFIER) {
                    val text = prev.text
                    if (text == "path" || text == "url") {
                        return text
                    }
                }

                prev = PsiTreeUtil.prevLeaf(prev)
            }

            return null
        }
    }
}
